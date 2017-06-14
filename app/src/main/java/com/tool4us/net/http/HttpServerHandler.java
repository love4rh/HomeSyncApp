package com.tool4us.net.http;

import static com.tool4us.net.common.NetSetting.NS;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;



public class HttpServerHandler extends SimpleChannelInboundHandler<Object>
{
    /**
     * Request Handler를 생성하기 위한 멤버
     */
    private final RequestHandlerFactory     _requestFac;
    
    /**
     * 생성된 Request Handler 관리 멤버.
     * 이미 생성된 것은 여기 것을 이용하고 없다면 _requestFac을 이용하여 만듦.
     */
    private Map<String, HTTPServiceHandle>  _reqMap = null;
    
    /**
     * 최근 요청된 Request 객체
     */
    private HttpRequest     _request = null;
    
    private HttpPostRequestDecoder      _postDecoder = null;
    
    
    public HttpServerHandler(RequestHandlerFactory reqFac)
    {
        _requestFac = reqFac;
        _reqMap = new TreeMap<String, HTTPServiceHandle>();
    }
    
    private HTTPServiceHandle getHandler(String uri)
    {
        HTTPServiceHandle reqHandle = _reqMap.get(uri);
        if( reqHandle == null )
        {
            try
            {
                reqHandle = _requestFac.getRquestClazz(uri);

                _reqMap.put(uri, reqHandle);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        
        return reqHandle;
    }
    
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx)
    {
        ctx.flush();
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg)
    {
        if( msg instanceof HttpRequest )
        {
            _request = (HttpRequest) msg;

            if( _request.method() == HttpMethod.POST )
            {
                _postDecoder = new HttpPostRequestDecoder(_request);
            }
            else if( _request.method() == HttpMethod.OPTIONS )
            {
                //
            }
            else
                _postDecoder = null;
        }
        
        if( msg instanceof HttpContent )
        {
            if( _postDecoder != null )
                _postDecoder.offer((HttpContent) msg);
        
            if( msg instanceof LastHttpContent )
            {
                // Parameter Map 생성
                String uriPath = null;
                Map<String, List<String>> params = null;
                
                if( _request.method() == HttpMethod.GET )
                {
                    QueryStringDecoder gDecoder = new QueryStringDecoder(_request.uri());
                    params = gDecoder.parameters();
                    uriPath = gDecoder.path();
                }
                // POST라고 가정
                else if( _postDecoder != null )
                {
                    uriPath = _request.uri();
                    
                    params = new TreeMap<String, List<String>>();
                    
                    List<InterfaceHttpData> paramList = _postDecoder.getBodyHttpDatas();
                    for(InterfaceHttpData data : paramList)
                    {
                        List<String> paramVal = new ArrayList<String>();
                        try
                        {
                            paramVal.add( ((Attribute)data).getValue() );
                        }
                        catch( Exception e )
                        {
                            NS.trace(null, e);
                        }

                        params.put(data.getName(), paramVal);
                    }
                }
                else
                {
                    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
                    return;
                }
                
                HTTPServiceHandle reqHandle = getHandler(uriPath);
                if( reqHandle != null )
                {
                    NS.info(null, "Request", uriPath);
                    
                    // TODO parameter 목록 표시
                    
                    Responser resEx = new Responser();
                    Requester reqEx = new Requester(_request, params, ctx);
    
                    try
                    {
                        resEx.headers().set(_request.headers());

                        // resEx.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
                        resEx.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
                        resEx.setResultContent( reqHandle.call(reqEx, resEx) );
                        writeResponse(resEx, ctx);
                    }
                    catch( Exception e )
                    {
                        NS.trace(null, e);
                        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                    }
                }
                else
                {
                    NS.warn(null, "Invalid API", uriPath);

                    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                }
            }
        }
    }

    private boolean writeResponse(FullHttpResponse response, ChannelHandlerContext ctx)
    {
        // Decide whether to close the connection or not.
        boolean keepAlive = HttpUtil.isKeepAlive(_request);

        if( keepAlive )
        {
            // Add 'Content-Length' header only for a keep-alive connection.
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            // Add keep alive header as per:
            // -
            // http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        
        // Encode the cookie.
        String cookieString = _request.headers().get(HttpHeaderNames.COOKIE);
        if( cookieString != null )
        {
            Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieString);

            if( !cookies.isEmpty() )
            {
                // Reset the cookies if necessary.
                for(Cookie cookie : cookies)
                {
                    response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
                }
            }
        }
        
        // Write the response.
        ctx.write(response);
        
        return keepAlive;
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
        cause.printStackTrace();
        ctx.close();
    }
}
