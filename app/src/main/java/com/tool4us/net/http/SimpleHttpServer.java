package com.tool4us.net.http;

import java.io.File;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;



/**
 * HTTP 서버
 * 
 * @author TurboK
 */
public class SimpleHttpServer
{
    private int 				_port = 8080;

    private EventLoopGroup 		_bossGroup = null;
    private EventLoopGroup 		_workerGroup = null;
    private ServerBootstrap 	_bootstrap = null;
    private ChannelFuture       _bootChannel = null;
    
    private RequestHandlerFactory	_handlerFactory = null;

    
    /**
     * @param handlerPackage    요청에 대한 핸들러가 모여 있는 패키지 이름
     */
    public SimpleHttpServer(String handlerPackage)
    {
        _handlerFactory = new RequestHandlerFactory(handlerPackage);
    }
    
    public boolean isRunning()
    {
    	return _bossGroup != null;
    }
    
    /**
     * 일반 서버 실행.
     * 
     * @param port              포트번호
     * @param bossThreadNum     Listening Thread 개수
     * @param workThreadNum     작업 Thread 개수
     * @throws Exception
     */
    public void start(int port, int bossThreadNum, int workThreadNum) throws Exception
    {
        start(port, bossThreadNum, workThreadNum, null);
    }

    /**
     * SSL로 서버 실행.
     * 
     * @param port          	포트번호
     * @param bossThreadNum 	Listening Thread 개수
     * @param workThreadNum 	작업 Thread 개수
     * @param sslPemFilePath
     * @param sslKeyFilePath
     * @throws Exception
     */
    public void start(int port, int bossThreadNum, int workThreadNum
            , String sslPemFilePath, String sslKeyFilePath ) throws Exception
    {        
        start(port, bossThreadNum, workThreadNum 
            , SslContextBuilder.forServer(new File(sslPemFilePath), new File(sslKeyFilePath)).build() );
    }

    private void start(int port, int bossThreadNum, int workThreadNum, final SslContext sslCtx) throws Exception
    {
    	_port = port;
        
        // Configure the server.
    	_bossGroup = new NioEventLoopGroup(bossThreadNum);
    	_workerGroup = workThreadNum <= 0 ? new NioEventLoopGroup()
    									  : new NioEventLoopGroup(workThreadNum);

        try
        {
            _bootstrap = new ServerBootstrap();

            _bootstrap.group(_bossGroup, _workerGroup)
                .channel(NioServerSocketChannel.class);

            _bootstrap.option(ChannelOption.SO_BACKLOG, 100)
	            .option(ChannelOption.SO_KEEPALIVE, true)
	            .childHandler( new HttpServerInitializer(_handlerFactory, sslCtx) );
            
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
    	if( _bossGroup == null )
    		return;

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
    
    public RequestHandlerFactory getHandlerFactory()
    {
        return _handlerFactory;
    }
}


/**
 * netty example에서 가져옴.
 * https://github.com/netty/netty/blob/master/example/src/main/java/io/netty/example/http/upload/HttpUploadServerInitializer.java
 */
class HttpServerInitializer extends ChannelInitializer<SocketChannel>
{
    private final SslContext                _sslCtx;
    private final RequestHandlerFactory     _requestFac;

    
    public HttpServerInitializer(RequestHandlerFactory reqFac, SslContext sslCtx)
    {
        _sslCtx = sslCtx;
        _requestFac = reqFac;
    }

    @Override
    public void initChannel(SocketChannel ch)
    {
        ChannelPipeline pipeline = ch.pipeline();

        if( _sslCtx != null )
        {
            pipeline.addLast( _sslCtx.newHandler(ch.alloc()) );
        }

        pipeline.addLast(new HttpRequestDecoder())
                .addLast(new HttpResponseEncoder())
                // Remove the following line if you don't want automatic content compression.
                .addLast(new HttpContentCompressor())
                .addLast(new SimpleChannelInboundHandler<HttpRequest>()
                {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest request) throws Exception
                    {
                        // to solve CORS problem
                        request.headers().add("Access-Control-Allow-Origin", "*");
                        request.headers().add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
                        request.headers().add("Access-Control-Allow-Headers", "X-Requested-With, Content-Type, Content-Length");
                        ctx.fireChannelRead(request);
                    }
                })
                .addLast(new HttpServerHandler(_requestFac))
                ;
    }
}
