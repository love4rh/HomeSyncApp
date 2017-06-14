package com.tool4us.homesync.handler;

import static com.tool4us.net.common.NetSetting.NS;
import static com.tool4us.homesync.file.Repository.RT;

import java.io.File;

import com.tool4us.logging.Logs;
import com.tool4us.net.common.ErrorCode;
import com.tool4us.net.common.ISession;
import com.tool4us.net.common.Protocol;
import com.tool4us.net.common.ProtocolExecutor;
import com.tool4us.net.handler.MessageDefine;
import com.tool4us.net.handler.MessageHandler;
import com.tool4us.util.FileTool;



/**
 * 파일 받음
 * @author TurboK
 */
@MessageDefine(id=TypeConstant.RES_FILE)
public class ReceiveFileEvent extends MessageHandler
{
    public ReceiveFileEvent(ProtocolExecutor executor)
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
        boolean isOk = (Byte) msg.getParameter(0) == 0x01;
        String uniquePath = (String) msg.getParameter(3);

        NS.info(session, "ReceiveFile", uniquePath, isOk);
        
        if( isOk )
        {
            File sFile = (File) msg.getParameter(1);
            long mTime = (Long) msg.getParameter(2);
            String pathName = RT.getAbsolutePath(uniquePath);
            
            // file을 이동
            File tFile = new File(pathName);

            if( FileTool.makeDir(pathName, true) )
            {
                try
                {
                    Logs.info("RECV FILE", sFile, tFile);

                    FileTool.moveFile(sFile, tFile);
                    tFile.setLastModified(mTime);
                    
                    NS.info(session, "ReceiveFile", "Updated", uniquePath);
                    RT.addOrUpdateEntry(tFile);
                }
                catch(Exception xe)
                {
                    NS.warn(session, "ReceiveFile", "Error", xe.getMessage());
                }
            }
            else
                NS.warn(session, "ReceiveFile", "can not make directories");
        }

        return null;
    }
}
