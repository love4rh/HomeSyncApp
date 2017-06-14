package com.tool4us.net.common;

import static com.tool4us.net.common.NetSetting.NS;

import java.io.File;
import java.io.RandomAccessFile;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;



/**
 * 프로토콜을 정의하기 위한 Protocol 클래스를 주고 받는 데이터의 단위로 처리하는 Channel 핸들러
 *  
 * @author TurboK
 *
 */
public abstract class ProtocolHandle extends SimpleChannelInboundHandler<ProtocolElem>
{
	/**
	 * Protocol 받기완료, 채널 Close 등 Channel에서 발생하는 이벤트를 처리하기 위한 핸들러
	 */
	private IChannelEventHandle		_channelEventHandle = null;
	
	/**
	 * 이 핸들을 소유하고 있는 Session
	 */
	private ISession                _session = null;
	
	/**
	 * 현재 새로 처리하고 있는 프로토콜
	 */
	private Protocol				_currentProtocol = null;
	
	/**
	 * 현재 받고 있는 파일
	 */
	private File					_recvFile = null;
	private RandomAccessFile		_fileOut = null;
	private long					_fileLength = 0;
	private long					_readLength = 0;

	private Channel                 _channel = null;
	
	/** -1: Not Assigned, 0: Not Connected, 1: Connected */
	private int                     _active = -1;

	
	public ProtocolHandle(IChannelEventHandle channelEventHandle)
	{
		super(true);
		
		_channelEventHandle = channelEventHandle;
	}
	
	/**
	 * 서버에서 실행되고 있는 지 여부 반환
	 */
	abstract public boolean isOnServer();
	
	/**
	 * 연결이 정상적으로 활성화 되어 있는 지 여부 반환
	 * @return
	 */
	public boolean isActive()
	{
	    return _active == 1 || (_channel != null && _channel.isActive());
	}
	
	public void setChannelHandler(IChannelEventHandle channelEventHandle)
	{
		_channelEventHandle = channelEventHandle;
	}
	
	public void setChannel(Channel channel)
    {
        _channel = channel;
    }
	
	public void setSession(ISession session)
	{
	    _session = session;
	}
	
	/**
	 * IP 주소 반환.
	 */
	public String getIPAddress()
	{
	    if( _channel == null )
	        return null;
	    
	    String tmpStr = _channel.remoteAddress().toString();
	    
	    int sPos = tmpStr.indexOf("/");
	    if( sPos == -1 ) sPos = 0;
	    
	    int ePos = tmpStr.lastIndexOf(":");
	    if( ePos == -1 )
	        ePos = tmpStr.length();
	    
	    return tmpStr.substring(sPos + 1, ePos);
	}
	
	/**
	 * "/192.168.0.1:47788"와 같이 접속한 PC이 IP와 사용하고 있는 포트를 문자열로 반환함.
	 */
	public String getIPAndPort()
	{
	    return _channel == null ? null : _channel.remoteAddress().toString();
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception
    {	
		if( _channelEventHandle != null )
            _channelEventHandle.onChannelActive(ctx);
		
		_active = 1;
    }
	
	@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        ctx.fireChannelInactive();
        
        if( _channelEventHandle != null )
        	_channelEventHandle.onChannelInactive(ctx);
        
        _active = 0;
    }
	
	public void close()
	{
		//
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ProtocolElem elem) throws Exception
	{
		if( _currentProtocol == null && elem.valueType() == ProtocolElem._headerType_ )
		{
			int[] headerVal = (int[]) elem.getValue();
			_currentProtocol = new Protocol(headerVal[0], headerVal[1], headerVal[2]);
		}
		else if( _currentProtocol != null )
		{
			// elem에 있는 내용을 하나씩 _currentProtocol에 추가하기
			if( elem.valueType() == ProtocolElem._fileType_ )
			{
				switch( elem.subType() )
				{
				case (byte) 0x01:	// 처음 파일 크기 정보
					{
						_fileLength = (Long) elem.getValue();
						_recvFile = NS.createTempFile("_recv", ".tmp", true);
						_fileOut = new RandomAccessFile(_recvFile, "rw");
					} break;
				case (byte) 0x02:	// 중간 내용들
				case (byte) 0x03:	// 마지막 내용
					{
						byte[] segment = null;
						
						if( _fileLength > 0 )
						{
						    segment = (byte[]) elem.getValue();
						    
						    _fileOut.write(segment);
						    _readLength += segment.length;
						}

						// 마지막이므로 File 핸들 닫고 파라미터로 추가하기
						if( elem.subType() == (byte) 0x03 )
						{
							_fileOut.close();
							_fileOut = null;
							
							if( _fileLength != _readLength )
							{
								System.out.println("RECV FILE DIFF LENGTH: " + _readLength + " / " + _fileLength);
							}
							
							_fileLength = _readLength = 0;
							
							_currentProtocol.addParameter(_recvFile);
							_recvFile = null;
						}
					} break;
				default:
					break;
				}
			}
			else
			{
				_currentProtocol.addParameter( elem.getValue() );
			}
		}

		// 더 이상 필요한 인수가 없다면 Protocol이 완료된 것임
        if( _currentProtocol != null && !_currentProtocol.isNeedMoreParam() )
        {
            if( _channelEventHandle != null )
            {
                NS.debug( _session, "RECV"
                        , "PROTOCOL: 0x" + _currentProtocol.typeHex()
                        , "MSG ID: " + _currentProtocol.id()
                        , _currentProtocol.getParameterAsString() );
                
                _channelEventHandle.onRecvProtocol(_currentProtocol);
            }

            _currentProtocol = null;
        }
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		if( _channelEventHandle != null )
			_channelEventHandle.onExceptionCaught(ctx, cause);
		else
		    NS.trace(_session, cause);

		_active = 0;

		ctx.close();
	}

	/**
	 * Protocol 보내기
	 * @param msg	보낼 Protocol 객체
	 * @throws Exception
	 */
	synchronized
	public boolean send(Protocol msg) throws Exception
	{
	    _channel.writeAndFlush( new ProtocolChuckedInput(msg, isOnServer()) );
	    
	    /*
	    final long sTick = System.currentTimeMillis();
	    final ChannelFuture f = _channel.writeAndFlush( new ProtocolChuckedInput(msg) );
	    
	    f.addListener(new ChannelFutureListener()
	    {
            @Override
            public void operationComplete(ChannelFuture future)
            {
                assert f == future;
                
                C.writeDebug(_session, "NETWORK", "writeAndFlush", System.currentTimeMillis() - sTick);
            }
        }); // */

		return this.isActive();
	}
}
