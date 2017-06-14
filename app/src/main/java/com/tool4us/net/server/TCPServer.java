package com.tool4us.net.server;

import static com.tool4us.net.common.NetSetting.NS;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import com.tool4us.net.common.Protocol;
import com.tool4us.net.common.ProtocolDecoder;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.oio.OioServerSocketChannel;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;



/**
 * 
 * @author TurboK
 */
public abstract class TCPServer
{
    private int                 _port = 8181;
    
    private EventLoopGroup      _bossGroup = null;
    private EventLoopGroup      _workerGroup = null;
    private ServerBootstrap     _bootstrap = null;
    private ChannelFuture       _bootChannel = null;
    
    private SessionManager      _clientList = null;

    
    /**
     * @param clientFactory     Client 접속 시 필요한 Client Session 생성 객체
     */
    public TCPServer()
    {
        _clientList = new SessionManager();
    }
    
    public boolean isRunning()
    {
        return _bossGroup != null;
    }
    
    /**
     * 서버 실행.
     * 
     * @param port              포트번호
     * @param bossThreadNum     Listening Thread 개수
     * @param workThreadNum     작업 Thread 개수
     * @throws Exception
     */
    public void start(int port, int bossThreadNum, int workThreadNum) throws Exception
    {
        _port = port;
        
        boolean blockingIO = false;
        
        if( blockingIO )
        {
            _bossGroup = new OioEventLoopGroup();
            _workerGroup = new OioEventLoopGroup();
        }
        else
        {
            _bossGroup = new NioEventLoopGroup(bossThreadNum);
            _workerGroup = workThreadNum <= 0 ? new NioEventLoopGroup()
                                              : new NioEventLoopGroup(workThreadNum);
        }
        
        try
        {
            _bootstrap = new ServerBootstrap();
            
            if( blockingIO )
                _bootstrap.group(_bossGroup, _workerGroup)
                          .channel(OioServerSocketChannel.class);
            else
                _bootstrap.group(_bossGroup, _workerGroup)
                          .channel(NioServerSocketChannel.class);

            _bootstrap
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_SNDBUF, NS.chunkedSize() * 2)
                .option(ChannelOption.ALLOCATOR, new PooledByteBufAllocator())
                // .handler(new DataOnLogging()) // io.netty.handler.logging.LoggingHandler;
                .childHandler( new ChannelInitializer<SocketChannel>()
                {
                     @Override
                     public void initChannel(SocketChannel ch) throws Exception
                     {
                         ChannelPipeline pipeline = ch.pipeline();

                         // 몇 초 이상 응답이 없을 경우 클라이언트를 종료하려면 아래 Handler를 추가
                         if( NS.getReadTimeOut() > 0 )
                             pipeline.addLast(new ReadTimeoutHandler(NS.getReadTimeOut()));
    
                         // Enable stream compression (you can remove these two if unnecessary)
                         pipeline.addLast("deflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
                         pipeline.addLast("inflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
    
                         pipeline.addLast("decoder", new ProtocolDecoder());
                         pipeline.addLast("streamer", new ChunkedWriteHandler());

                         ClientSession session = newClientSessionEx();
                         session.setChannel(pipeline.channel());
                         
                         pipeline.addLast(session);
                     }
                 } );
            
            // Start the server.
            _bootChannel = _bootstrap.bind(_port).sync();
        }
        catch( Exception e )
        {
            shutdown();
            
            throw e;
        }
    }
    
    // Shut down all event loops to terminate all threads.
    public void shutdown()
    {
        if( _workerGroup == null )
            return;

        if( _bossGroup != null )
            _bossGroup.shutdownGracefully();

        _workerGroup.shutdownGracefully();
        
        _bossGroup = null;
        _workerGroup = null;
        _bootChannel = null;
        _bootstrap = null;
    }
    
    /**
     * 서버가 끝날 때까지 대기하는 메소드
     * 
     * @throws Exception
     */
    public void sync() throws Exception
    {
        if( _bootChannel == null )
            return;
        
        _bootChannel.channel().closeFuture().sync();
    }
    
    /** 내 주소 반환 */
    public String address()
    {
        try
        {
            return InetAddress.getLocalHost().getHostAddress();
        }
        catch( UnknownHostException xe )
        {
            xe.printStackTrace();
        }
        
        return "N/A";
    };
    
    /** 사용 중인 리스닝 포트 반환 */
    public int port() { return _port; }
    
    void addClient(ClientSession session)
    {
        synchronized(_clientList)
        {
            _clientList.add(session);
        }
    }
    
    void removeClient(ClientSession session)
    {
        synchronized(_clientList)
        {
            _clientList.remove(session);
        }
    }

    /**
     * 접속 중인 클라이언트 세션 목록 반환
     */
    public List<ClientSession> getSessionList()
    {
        return _clientList.getConnectedList();
    }
    
    /**
     * 접속 중인 클라이언트 세션 개수 반환
     */    
    public int getConnectedSessionSize()
    {
        return _clientList.getConnectedSessionSize();
    }
    
    /**
     * 지정한 세션 강제로 닫기
     * @param session
     */
    public void killSession(ClientSession session) throws Exception
    {
        // TODO
    }
    
    public void broadCast(Protocol msg)
    {
        List<ClientSession> cList = getSessionList();
        
        for(ClientSession session : cList)
        {
            try
            {
                session.send(msg);
            }
            catch(Exception xe)
            {
                NS.trace(null, xe);
            }
        }
    }
    
    public ClientSession newClientSessionEx()
    {
        ClientSession session = newClientSession();
        
        session.setServer(this);
        
        return session;
    }
    
    /**
     * 새로 접속한 Client를 위한 ClientSession 객체 생성하여 반환.
     * 서버에 접속한 Client의 Session을 관리하기 위한 객체임.
     */
    abstract public ClientSession newClientSession();
}
