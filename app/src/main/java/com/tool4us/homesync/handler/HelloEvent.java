package com.tool4us.homesync.handler;

import static com.tool4us.net.common.NetSetting.NS;

import com.tool4us.net.common.ErrorCode;
import com.tool4us.net.common.ISession;
import com.tool4us.net.common.Protocol;
import com.tool4us.net.common.ProtocolExecutor;
import com.tool4us.net.handler.MessageDefine;
import com.tool4us.net.handler.MessageHandler;



/**
 * 클라리언트가 서버에 접속 후 서버 확인을 위하여 보내는 메시지
 * 
 * @author TurboK
 */
@MessageDefine(id=TypeConstant.HELLO)
public class HelloEvent extends MessageHandler
{
    public HelloEvent(ProtocolExecutor executor)
    {
        super(executor);
    }
    
    @Override
    public void clear()
    {
        // Nothing to do
    }

    @Override
    public ErrorCode setAndCheck(Protocol msg, ISession session) throws Exception
    {
        if( msg.sizeOfParam() < 1 )
        {
            return ErrorCode.errApiInsufficientParameter;
        }

        return ErrorCode.errSuccess;
    }
    
    @Override
    public Protocol work(Protocol msg, ISession session) throws Exception
    {
        String text = (String) msg.getParameter(0);
        
        NS.info(session, "Hello", text);
        
        Protocol rMsg = msg.createReply();
        
        // TODO 식별 코드 같은 것을 보냈으면 좋겠는데...
        rMsg.addParameter((byte) 0x01);
        rMsg.addParameter(1);
        // rMsg.addParameter(paramVal);
        
        return rMsg;
    }
}
