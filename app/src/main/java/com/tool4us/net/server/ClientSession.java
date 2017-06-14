package com.tool4us.net.server;

import static com.tool4us.net.common.NetSetting.NS;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import com.tool4us.net.common.IChannelEventHandle;
import com.tool4us.net.common.ISession;
import com.tool4us.net.common.Protocol;
import com.tool4us.net.common.ProtocolElem;
import com.tool4us.net.common.ProtocolHandle;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutException;



/**
 * 서버 내에서 클라이언트를 관리하기 위한 클래스.
 * 메소드 중 onXXXX()와 같이 on으로 시작하는 이벤트 핸들러 메소드를 Override할 때 꼭 이 메소드의 것을 호출해야 함.
 * 
 * @author TurboK
 */
public abstract class ClientSession extends ProtocolHandle
						            implements IChannelEventHandle, ISession
{
    /** 클라이언트 세션을 관리할 서버 객체 */
    private TCPServer        _server = null;
    
    /**
     * 접속할 때 부여되는 Client ID.
     * SessionManager에서 할당되는 값임
     */
    private int                 _clientId = -1;
    
    /** 클라이언트 식별을 위한 정보 문자열 */
    private String              _clientInfo = "Undefined";
    
    /** 접속한 시간 */
    private Date				_connectedTime = null;
    
    /** 접속한 클라이언트의 IP */
    private String				_connectedIP = null;
    
    /** Session Value 관리 멤버 */
    private Map<String, Object>     _sessionValue = null;
    
    
	public ClientSession()
	{
		super(null);
		
		setSession(this);
		setChannelHandler(this);
		
        _connectedTime = new Date();
        _sessionValue = new ConcurrentSkipListMap<String, Object>();
	}
	
	void setServer(TCPServer server)
	{
	    _server = server;
	}

	/**
	 * SessionManager에서 호출됨
	 * @param id
	 */
	void setClientId(int id)
	{
		if( _clientId == -1 )
			_clientInfo += ("|" + id);
			
		_clientId = id;
	}
	
	public int getClientID()
	{
		return _clientId;
	}
	
	public void addClientDescription(String desc)
	{
		_clientInfo += ("|" + desc);
	}
	
	/**
	 * 사용한 리소스 정리를 위한 메소드
	 */
	abstract public void cleanUp();
	
	/**
	 * 접속한 사용자의 정보를 한 문자열로 표현하여 반환
	 */
	@Override
	public String getClientDescription()
	{
		return _clientInfo;
	}
	
	@Override
    public boolean isValid()
    {
        return this.isActive();
    }
	
	/**
	 * 접속한 시간 반환
	 */
	@Override
	public Date getConnectedTime()
	{
		return _connectedTime;
	}
	
	@Override
	public String getClientIP()
	{
		return _connectedIP;
	}
	
	@Override
    public Object getValue(String key)
    {
	    return _sessionValue.get(key);
    }

    @Override
    public void setValue(String key, Object value)
    {
        _sessionValue.put(key, value);
    }
    
    @Override
    public void removeValue(String key)
    {
        _sessionValue.remove(key);
    }
    
    @Override
    public int getUserLevel()
    {
        return 0;
    }
	
	@Override
	public void onChannelActive(ChannelHandlerContext ctx)
	{
		// "접속 중" 목록에 추가
	    _server.addClient(this);
		
		_clientInfo = _connectedIP = ctx.channel().remoteAddress().toString();

	    NS.info(null, "CHANNEL OPEN", _connectedIP);
	}
	
	@Override
	public void onChannelInactive(ChannelHandlerContext ctx)
	{
		cleanUp();
		
		// "접속 중" 목록에서 제거
		_server.removeClient(this);
		
		NS.info(this, "CHANNEL CLOSED");
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ProtocolElem elem) throws Exception
	{
	    super.channelRead0(ctx, elem);
	}
	
	@Override
	public void onExceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
	    String msg = cause.getMessage();

		NS.warn(this, "CHANNEL CLOSED BY ", msg);

		// ReadTimeoutException 는 오류가 아님.
        // IOException 중 Connection reset by peer 는 메지시에 안 남게 했으면 좋겠다.
		if( !(cause instanceof ReadTimeoutException)
		        && !"Connection reset by peer".equals(msg)
		        && !"현재 연결은 원격 호스트에 의해 강제로 끊겼습니다".equals(msg) )
		{
		    NS.trace(this, cause);
		}
	}
	
	@Override
    public boolean isOnServer()
    {
        return true;
    }
	
	@Override
	abstract public void onRecvProtocol(Protocol msg);
}
