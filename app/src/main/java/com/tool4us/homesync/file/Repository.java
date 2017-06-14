package com.tool4us.homesync.file;

import static java.nio.file.StandardWatchEventKinds.*;
import static com.tool4us.net.common.NetSetting.NS;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent.Kind;

import com.tool4us.task.ITask;
import com.tool4us.task.TaskQueue;



/**
 * 서버 내 지정된 Home 위치의 폴더 및 파일 목록을 관리하기 위한 클래스
 * @author TurboK
 */
public enum Repository implements DirectoryCallback
{
    RT;
    
    private FileDictionary      _fileDict;
    private DirectoryMonitor    _dirMonitor;
    
    private TaskQueue           _jobQ;
    
    
    private Repository()
    {
        _jobQ = new TaskQueue(null);
    }
    
    public String getRootPath()
    {
        return _fileDict.getRootPath();
    }
    
    public void setUpRoot(String rootPath, boolean monitoring) throws Exception
    {
        _fileDict = new FileDictionary(rootPath);
        _fileDict.reload();
        
        if( _dirMonitor != null )
        {
            _dirMonitor.close();
            _dirMonitor = null;
        }
        
        if( monitoring )
        {
            _dirMonitor = new DirectoryMonitor(Paths.get(rootPath), true, this);
            _dirMonitor.doMonitoring();
        }
        
        _jobQ.startQueue(1, "Worker");
    }
    
    public void close()
    {
        _jobQ.clearAllJob();
        
        if( _dirMonitor != null )
        {
            _dirMonitor.close();
            _dirMonitor = null;
        }
    }
    
    public FileDictionary getFileDictionary()
    {
        return _fileDict;
    }
    
    public String getAbsolutePath(String uniquePath)
    {
        return getRootPath() + uniquePath;
    }
    
    public void addOrUpdateEntry(File file)
    {
        _fileDict.addEntry(file);
    }
    
    public void removeEntry(File file)
    {
        _fileDict.removeEntry(file);
    }

    @Override
    public void onChange(Kind<?> kind, Path path)
    {
        NS.info(null, "FileChanged", kind.name(), path);
        
        File file = path.toFile();
        
        if( kind == ENTRY_DELETE )
            _fileDict.removeEntry(file);
        else
            _fileDict.addEntry(file);
        
        // TODO 연결된 클라이언트에 전달하기
    }

    public void pushTask(ITask task)
    {
        _jobQ.pushTask(task);
    }
}
