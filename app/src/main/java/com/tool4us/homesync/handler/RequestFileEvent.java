package com.tool4us.homesync.handler;

import static com.tool4us.net.common.NetSetting.NS;
import static com.tool4us.homesync.file.Repository.RT;

import java.io.File;

import com.tool4us.net.common.ErrorCode;
import com.tool4us.net.common.ISession;
import com.tool4us.net.common.Protocol;
import com.tool4us.net.common.ProtocolExecutor;
import com.tool4us.net.handler.MessageDefine;
import com.tool4us.net.handler.MessageHandler;



/**
 * 파일 요청.
 * @author TurboK
 */
@MessageDefine(id=TypeConstant.REQ_FILE)
public class RequestFileEvent extends MessageHandler
{
    public RequestFileEvent(ProtocolExecutor executor)
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
        // 파일 상대 경로
        if( msg.sizeOfParam() < 1 )
        {
            return ErrorCode.errApiInsufficientParameter;
        }

        return ErrorCode.errSuccess;
    }
    
    @Override
    public Protocol work(Protocol msg, ISession session) throws Exception
    {
        String uniquePath = (String) msg.getParameter(0);
                
        NS.info(session, "ReqFile", uniquePath);
        
        Protocol rMsg = Protocol.newProtocol(TypeConstant.RES_FILE);
        
        File file = new File(RT.getAbsolutePath(uniquePath));
        
        if( file.exists() )
        {
            rMsg.addParameter((byte) 0x01);
            rMsg.addParameter(file);
            rMsg.addParameter(file.lastModified());
            rMsg.addParameter(uniquePath);
        }
        else
        {
            writeReplyProtocol(rMsg, ErrorCode.errFileNotExist);
            rMsg.addParameter(uniquePath);
        }

        return rMsg;
    }
}
