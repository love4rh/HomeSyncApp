package com.tool4us.homesync.task;

import java.io.File;

import com.tool4us.task.ITask;



/**
 * task of deleting the file 
 * @author TurboK
 */
public class DeleteFileTask extends ITask
{
    private String      _uniquePath;
    
    
    public DeleteFileTask(String uniquePath)
    {
        _uniquePath = uniquePath;
    }
    
    @Override
    public boolean isPossibleToRun()
    {
        return true;
    }
    
    @Override
    public String toString()
    {
        return "DELETE:" + _uniquePath;
    }
    
    @Override
    public void run() throws Exception
    {
        File file = new File(_uniquePath);
        
        if( file.delete() )
        	System.out.println("DELETED: " + _uniquePath);
        else
        	System.out.println("FAILED TO DELETE: " + _uniquePath);
    }
    
}
