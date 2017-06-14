package com.tool4us.net.http;

import java.util.List;
import java.util.Map;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;


public class Requester implements HttpRequest
{
    private HttpRequest                 _req = null;
   
    private Map<String, List<String>>   _params = null;
    
    private ChannelHandlerContext       _ctx = null;
    
    
    public Requester( HttpRequest req
                    , Map<String, List<String>> params
                    , ChannelHandlerContext ctx )
    {
        _req = req;
        _params = params;
        _ctx = ctx;
    }
    
    public Channel channel()
    {
        return _ctx.channel();
    }
    
    public String getRemoteDescription()
    {
        if( _ctx == null )
            return "Unkwoun";
        
        return _ctx.channel().remoteAddress().toString();
    }

    @Override
    public HttpVersion getProtocolVersion()
    {
        return _req.protocolVersion();
    }

    @Override
    public HttpHeaders headers()
    {
        return _req.headers();
    }

    @Override
    public DecoderResult getDecoderResult()
    {
        return _req.decoderResult();
    }

    @Override
    public void setDecoderResult(DecoderResult result)
    {
        _req.setDecoderResult(result);
    }

    @Override
    public HttpMethod getMethod()
    {
        return _req.method();
    }

    @Override
    public HttpRequest setMethod(HttpMethod method)
    {
        return _req.setMethod(method);
    }

    @Override
    public String getUri()
    {
        return _req.uri();
    }

    @Override
    public HttpRequest setUri(String uri)
    {
        return _req.setUri(uri);
    }

    @Override
    public HttpRequest setProtocolVersion(HttpVersion version)
    {
        return _req.setProtocolVersion(version);
    }
    
    public String getParameter(String paramName)
    {
        return parameter(paramName);
    }
    
    public String parameter(String paramName)
    {
        List<String> pList = _params.get(paramName);
        
        return pList == null ? null : pList.get(0);
    }

    public Map<String, List<String>> parameterMap()
    {
        return _params;
    }

    @Override
    public HttpVersion protocolVersion()
    {
        return _req.protocolVersion();
    }

    @Override
    public DecoderResult decoderResult()
    {
        return _req.decoderResult();
    }

    @Override
    public HttpMethod method()
    {
        return _req.method();
    }

    @Override
    public String uri()
    {
        return _req.uri();
    }
}
