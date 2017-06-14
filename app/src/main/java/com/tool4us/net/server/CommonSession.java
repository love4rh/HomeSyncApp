package com.tool4us.net.server;

import static com.tool4us.net.common.NetSetting.NS;

import com.tool4us.net.common.Protocol;
import com.tool4us.net.common.ProtocolExecutor;
import com.tool4us.net.handler.CommonExecutor;



/**
 * Executor를 이용하여 프로토콜을 핸들링하는 서버 세션 클래스
 * 
 * @author TurboK
 */
public class CommonSession extends ClientSession
{
    private ProtocolExecutor    _executor;

    
    public CommonSession(String pkgName)
    {
        this( CommonExecutor.newInstance(pkgName) );
    }
    
    public CommonSession(ProtocolExecutor executor)
    {
        super();

        _executor = executor;

        if( _executor != null )
            _executor.setClientSession(this);
    }
    
    @Override
    public void cleanUp()
    {
        //
    }
    
    @Override
    public void onRecvProtocol(Protocol msg)
    {
        if( _executor == null )
        {
            // TODO Exception 처리
            return;
        }
        
        try
        {
            _executor.executeProtocol(this, msg);
        }
        catch( Exception xe )
        {
            NS.trace(this, xe);
        }
        
    }
    
}
