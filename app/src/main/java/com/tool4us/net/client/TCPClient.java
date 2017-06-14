package com.tool4us.net.client;

import java.net.SocketAddress;

import com.tool4us.net.common.IChannelEventHandle;
import com.tool4us.net.common.ICloseHandle;
import com.tool4us.net.common.Protocol;
import com.tool4us.net.common.ProtocolDecoder;
import com.tool4us.net.common.ProtocolExecutor;
import com.tool4us.net.common.ProtocolHandle;
import com.tool4us.net.exception.TimeOutException;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.stream.ChunkedWriteHandler;



/**
 * TCPServer 용 클라이언트 클래스
 * 
 * @author TurboK
 */
public class TCPClient implements IChannelEventHandle
{
    private EventLoopGroup      _workerGroup = null;
    private ProtocolHandle      _handler = null;
    
    /**
     * Synchonized Send/Recv를 위한 Lock 객체
     */
    private Object              _syncLocker = new Object();
    
    private boolean             _syncOn = false;
    
    /**
     * 마지막에 받은 Protocol 객체
     */
    private Protocol            _lastRecvProtocol = null;
    
    private ProtocolExecutor    _executor = null;
    
    private ICloseHandle<TCPClient>     _closeHandle = null;
    
    /**
     * 서버로 부터 받은 클라이언트 ID
     */
    private int                 _clientId = -1;
    
    /** 서버와 통신하기 위한 채널 */
    private Channel             _channel = null;
    
    /** -1: Not Assigned, 0: Not Connected, 1: Connected */
    private int                 _connected = -1;
    
    
    public TCPClient(ProtocolExecutor executor)
    {
        this(executor, null);
    }
    
    public TCPClient(ProtocolExecutor executor, ICloseHandle<TCPClient> closeHandle)
    {
        _executor = executor;
        _closeHandle = closeHandle;
    }
    
    /** 지정된 Protocol 실행 모듈 반환 */
    public ProtocolExecutor getProtocolExecutor()
    {
        return _executor;
    }
    
    public boolean connect(String host, int port) throws Exception
    {
        return connect(host, port, -1); // -1 means default timeout.
    }
    
    /**
     * 
     * @param host
     * @param port
     * @param timeoutMs 1이상일때 유효하며, 0이하일 경우 default timeout값이 적용된다. ms 단위
     * @return
     * @throws Exception
     */
    public boolean connect(String host, int port, int timeoutMs) throws Exception
    {
        boolean isOk = true;
        
        _handler = new ProtocolHandle(this)
            {
                @Override
                public boolean isOnServer()
                {
                    return false;
                }
            };
        
        // NioEventLoopGroup 만들 때 FD가 70여개 증가함. 생성할 Thread 개수에 제한(1)을 둠. 
        _workerGroup = new NioEventLoopGroup(1);

        try
        {
            Bootstrap b = new Bootstrap();
            b.group(_workerGroup)
             .channel(NioSocketChannel.class)
             .option(ChannelOption.SO_KEEPALIVE, true)
             // Client에서 PooledByteBufAllocator를 사용하면 여러 커넥션이 ByteBuf를 새로 할당할 때
             // 경합이 발생하여 멈춘 것 같은 현상을 야기함. ProtocolChuckedInput.readChunk()에서 대기가 발생함.
             //.option(ChannelOption.ALLOCATOR, new PooledByteBufAllocator())
             .handler(new ChannelInitializer<SocketChannel>()
                {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception
                    {
                        ChannelPipeline pipeline = ch.pipeline();
    
                        // Enable stream compression (you can remove these two if unnecessary)
                        // 두 번이상 추가하면 NoClassDefFoundError 예외가 발생함
                        pipeline.addLast("deflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
                        pipeline.addLast("inflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
                        
                        pipeline.addLast("decoder", new ProtocolDecoder());
                        pipeline.addLast("streamer", new ChunkedWriteHandler());
                        
                        // Add the client handler
                        pipeline.addLast(_handler);
                    }
                });
            
            if( timeoutMs > 0 )
            {
                b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs);
            }

            // Start the client.
            ChannelFuture future = b.connect(host, port).sync();
            
            _channel = future.channel();
            
            if( _channel == null )
                throw new Exception("Can not connect to the server.");
            
            _handler.setChannel(_channel);
        }
        catch( Exception e )
        {
            disconnect();
            isOk = false;
            
            throw e;
        }
        
        _connected = isOk ? 1 : 0;
        
        return isOk;
    }
    
    /**
     * 서버로 부터 받은 클라이언트 ID 반환
     * @return
     */
    public int getClientId()
    {
        return _clientId;
    }
    
    /** 접속 여부 반환 */
    public boolean isConnected()
    {
        return _connected == 1 || (_channel != null && _channel.isActive());
    }
    
    public void disconnect()
    {
        _connected = 0;
        
        if( _workerGroup == null || _handler == null )
            return;
        
        _workerGroup.shutdownGracefully();
        _workerGroup = null;
        _channel = null;
        _connected = -1;
    }
    
    public SocketAddress remoteAddress()
    {
        return _channel != null ? _channel.remoteAddress() : null;
    }
    
    public ProtocolHandle getHandler()
    {
        return _handler;
    }

    /**
     * Protocol을 서버로 보내기
     * 
     * @param msg
     */
    synchronized
    public boolean send(Protocol msg) throws Exception
    {
        if( !isConnected() )   
            return false;
        
        return _handler.send(msg);
    }
    
    /**
     * Protocol을 보낸 후 reply protocol을 받을 때까지 대기하는 메소드.
     * 
     * @param msg       보낸 protocol 객체
     * @param waitTime  Reply 대기 시간. ms 단위로 입력하며 0이면 무한 대기임
     * @return reply된 protocol 객체. 못 보내거나 못 받은 상황이라면 null
     * @throws Exception
     */
    synchronized
    public Protocol sendSync(Protocol msg, int waitTime) throws Exception
    {
        _syncOn = true;
        _lastRecvProtocol = null;

        if( !send(msg) )
        {
            throw new Exception("[" + Thread.currentThread().getId() + "] TCPClient.sendSync() - SEND RETURNS FALSE.");
        }
        
        synchronized( _syncLocker )
        {
            if( waitTime > 0 )
            {
                long sTick = System.currentTimeMillis();
                while( waitTime > 0 && _lastRecvProtocol == null && isConnected() )
                {
                    _syncLocker.wait(waitTime);
                    
                    waitTime -= System.currentTimeMillis() - sTick;
                }
            }
            else
            {
                while( _lastRecvProtocol == null && isConnected() )
                    _syncLocker.wait();
            }
        }
        
        _syncOn = false;
        
        if( _lastRecvProtocol == null )
            throw new TimeOutException("Time out or disconnected.");
        
        // _lastRecvProtocol는 onRecvProtocol()에서 할당하게 되어 있음.
        return _lastRecvProtocol;
    }

    @Override
    public void onChannelActive(ChannelHandlerContext ctx)
    {
        //
    }
    
    @Override
    public void onChannelInactive(ChannelHandlerContext ctx)
    {
        if( _syncLocker != null )
            synchronized( _syncLocker )
            {
                _syncLocker.notifyAll();
            }

        disconnect();

        // 이 클라이언트 객체를 이용하는 다른 클래스에 종료됐음을 알려 주기
        if( _closeHandle != null )
            _closeHandle.onClosed(this);
    }
    
    @Override
    public void onExceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
        cause.printStackTrace();
        
        disconnect();
        
//      throw new IllegalStateException(cause.getMessage());
    }

    @Override
    public void onRecvProtocol(Protocol msg)
    {
        if( _syncOn )
        {
            _lastRecvProtocol = msg;
            
            synchronized( _syncLocker )
            {
                _syncLocker.notifyAll();
            }
        }
        else if( _executor != null )
        {
            try
            {
                _executor.executeProtocol(_handler, msg);
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
        }
    }
}
