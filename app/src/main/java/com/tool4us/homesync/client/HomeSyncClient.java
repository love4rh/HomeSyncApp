package com.tool4us.homesync.client;

import android.net.wifi.WifiManager;

import static android.content.Context.WIFI_SERVICE;
import static com.tool4us.util.CommonTool.CT;
import static com.tool4us.net.common.NetSetting.NS;

import java.io.File;

import static com.tool4us.homesync.file.Repository.RT;

import com.tool4us.homesync.file.CompResult;
import com.tool4us.homesync.file.FileElement;
import com.tool4us.homesync.handler.TypeConstant;
import com.tool4us.homesync.task.*;

import com.tool4us.logging.Logs;
import com.tool4us.net.client.TCPClient;
import com.tool4us.net.common.Protocol;
import com.tool4us.net.handler.CommonExecutor;
import com.tool4us.util.AppSetting;
import com.tool4us.util.FileTool;

import io.netty.channel.ConnectTimeoutException;



/**
 * 
 * @author TurboK
 */
public class HomeSyncClient extends TCPClient
{
    public static final String      OPT_SYNC_FOLDER = "syncfolder";
    public static final String      OPT_LAST_SERVER = "lastServer";
    
    private AppSetting  _setting = null;
    
    
    public HomeSyncClient()
    {
        super( CommonExecutor.newInstance("com.tool4us.homesync.handler") );
    }
    
    public void start(String appPath) throws Exception
    {
        _setting = new AppSetting(CT.concat(appPath, File.separator, "synclient.cfg"));

        _setting.load();

        String defaultSync = CT.concat(appPath, File.separator, "syncFolder");

        RT.setUpRoot(_setting.getValue(OPT_SYNC_FOLDER, defaultSync), false);
    }
    
    public void close()
    {
        try
        {
            _setting.save();
        }
        catch(Exception xe)
        {
            xe.printStackTrace();
        }
        
        this.disconnect();
    }

    public boolean findServer(String localIp, int port)
    {
        Protocol helloMsg = Protocol.newProtocol(TypeConstant.HELLO, localIp);
        
        localIp = localIp.substring(0, localIp.lastIndexOf('.') + 1);
        
        boolean found = false;
        String lastServer = _setting.getValue(OPT_LAST_SERVER);
        
        for(int i = 1; !found && i < 255; ++i)
        {
            String checkIp = localIp + i;
            
            if( i == 1 )
            {
                if( lastServer == null || lastServer.isEmpty() )
                    continue;
                checkIp = lastServer;
            }

            try
            {
                if( this.connect(checkIp, port, 500) )
                {
                    Protocol rMsg = this.sendSync(helloMsg, 1000);
                    if( rMsg.type() == TypeConstant.HELLO + 1 )
                    {
                        found = (0x01 == (Byte) rMsg.getParameter(0));
                    }
                    
                    if( !found )
                        this.disconnect();
                }
            }
            catch(ConnectTimeoutException xe)
            {
                // to do nothing
            }
            catch(Exception xe)
            {
                xe.printStackTrace();
            }
            
            if( !found )
            {
                Logs.debug("check " + checkIp + ", but it's not.");
            }
            else
            {
                Logs.debug("check " + checkIp + ", that's it.");
                _setting.setValue(OPT_LAST_SERVER, checkIp);
                _setting.save();
            }
        }

        return true;
    }
    
    public void compareList()
    {
        if( !isConnected() )
            return;
        
        try
        {
            Protocol msg = Protocol.newProtocol(TypeConstant.COMPARE);
            msg.addParameter( RT.getFileDictionary().toJson() );
            
            Protocol rMsg = this.sendSync(msg, 5000);
            
            boolean isOk = 0x01 == (Byte) rMsg.getParameter(0);
            
            if( isOk )
            {
                final int vCount = 5;
                int count = (Integer) rMsg.getParameter(1);
                
                System.out.println("Count for sync: " + count);
                
                for(int i = 0; i < count; ++i)
                {
                    int compResult = (Integer) rMsg.getParameter(i * vCount + 2);
                    String uniquePathName = (String) rMsg.getParameter(i * vCount + 3);
                    long fileSize = (Long) rMsg.getParameter(i * vCount + 4);
                    long mTime = (Long) rMsg.getParameter(i * vCount + 5);
                    int dir = (Integer) rMsg.getParameter(i * vCount + 6);
                    
                    FileElement fe = new FileElement(uniquePathName, fileSize, mTime, dir == 1);
                    
                    System.out.print("Comp: " + compResult + " --> ");
                    fe.debugOut();
                    
                    this.pushTask(compResult, fe);;
                }
            }
            else
            {
                System.out.println("Error occurred: " + rMsg.getParameter(1) + " | " + rMsg.getParameter(2));
            }
        }
        catch(Exception xe)
        {
            xe.printStackTrace();
        }
    }
    
    private void pushTask(int taskType, FileElement elem)
    {
        // 파일을 요청해 받아와야 하는 경우
        if( taskType == CompResult.I_HAVE || taskType == CompResult.DIFF_HAVE )
        {
            if( elem.isDirectory() )
            {
                String elemPath = elem.getAbsolutePath(RT.getRootPath());

                if( FileTool.makeDir(elemPath, false) )
                {
                    RT.addOrUpdateEntry(new File(elemPath));
                }
            }
            else
                RT.pushTask( new GettingFileTask(elem.getKey(), this) );
        }
        // 삭제해야 하는 경우임
        else if( taskType == CompResult.YOU_HAVE )
        {
        	String absPathName = elem.getAbsolutePath(RT.getRootPath());
        	
        	RT.pushTask( new DeleteFileTask(absPathName) );
        }
    }
    
    public void sendMessage(String msg)
    {
        /*
        try
        {
            this.send( Protocol.newProtocol(MessageType.SEND_MSG_SERVER, msg) );
        }
        catch(Exception xe)
        {
            xe.printStackTrace();
        } // */
    }
}

