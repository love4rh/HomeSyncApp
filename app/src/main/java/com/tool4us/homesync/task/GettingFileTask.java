package com.tool4us.homesync.task;

import com.tool4us.homesync.handler.TypeConstant;
import com.tool4us.net.client.TCPClient;
import com.tool4us.net.common.Protocol;
import com.tool4us.task.ITask;



/**
 * 서버에 파일을 요청하여 받고 클라이언트를 업데이트하는 작업
 * 
 * @author TurboK
 */
public class GettingFileTask extends ITask
{
    private String      _uniquePath;
    private TCPClient   _client;
    
    
    public GettingFileTask(String uniquePath, TCPClient client)
    {
        _uniquePath = uniquePath;
        _client = client;
    }
    
    @Override
    public boolean isPossibleToRun()
    {
        return true;
    }
    
    @Override
    public String toString()
    {
        return "GET:" + _uniquePath;
    }
    
    @Override
    public void run() throws Exception
    {
        Protocol msg = Protocol.newProtocol(TypeConstant.REQ_FILE, _uniquePath);

        _client.send(msg);
    }
    
}
