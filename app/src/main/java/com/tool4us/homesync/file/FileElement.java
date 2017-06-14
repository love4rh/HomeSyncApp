package com.tool4us.homesync.file;

import java.io.File;

/**
 * 동기화 대상 파일 및 디렉토리 정보 관리 클래스
 * 
 * @author TurboK
 */
public class FileElement
{
    private String      _uniquePathName = null;
    private long        _fileSize = 0L;
    private long        _modifiedTime = 0L;
    private boolean     _isDirectory = false;
    
    
    public FileElement(String rootPath, File file)
    {
        String absPath = file.getAbsolutePath();
        
        // absPath가 rootPath의 하위 폴더가 아닌 경우는 무시해야 함.
        // if( absPath.indexOf(rootPath) == -1 )
        
        String uniquePath = absPath.substring(rootPath.length());
        
        _isDirectory = file.isDirectory();
        _uniquePathName = makeKey(uniquePath);

        _fileSize = file.length();
        _modifiedTime = file.lastModified();
    }
    
    // 테스팅을 위한 생성자
    public FileElement(String uniquePathName, long fileSize, long mTime, boolean isDirectory)
    {
        _uniquePathName = uniquePathName;
        _fileSize = fileSize;
        _modifiedTime = mTime;
        _isDirectory = isDirectory;
    }
    
    public static String makeKey(String uniquePath)
    {
        return uniquePath.replace('\\', '/');
    }
    
    public String getKey()
    {
        return _uniquePathName;
    }
    
    public String getFileName()
    {
        return _uniquePathName.substring(_uniquePathName.lastIndexOf('/') + 1);
    }
    
    public String getUniquePath()
    {
        return _uniquePathName.substring(0, _uniquePathName.lastIndexOf('/'));
    }
    
    public String getAbsolutePath(String rootPath)
    {
        return rootPath + _uniquePathName.replace('/', File.separatorChar);
    }
    
    public long getFileSize()
    {
        return _fileSize;
    }
    
    public long getModifiedTime()
    {
        return _modifiedTime;
    }
    
    public boolean isDirectory()
    {
        return _isDirectory;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if( !(obj instanceof FileElement) )
            return false;
        
        if( this == obj )
            return true;
        
        FileElement that = (FileElement) obj;
        
        if( this._isDirectory != that._isDirectory
                || !this._uniquePathName.equals(that._uniquePathName) )
            return false;
        
        return this._isDirectory || (this._fileSize == that._fileSize && this._modifiedTime == that._modifiedTime);
    }
    
    @Override
    public int hashCode()
    {
        return _uniquePathName.hashCode();
    }
    
    public Object toJson()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append("{\"pathName\":")
          .append("\"").append(_uniquePathName).append("\"")
          .append(",\"size\":").append(_fileSize)
          .append(",\"time\":").append(_modifiedTime)
          .append(",\"directory\":").append(_isDirectory ? 1 : 0)
          .append("}")
          ;
        
        return sb.toString();
    }
    
    public void debugOut()
    {
        System.out.println(getKey() + ": "
            + getFileName() + " | "
            + getUniquePath() + " | "
            + _fileSize + " | "
            + _modifiedTime + " | "
            + _isDirectory
        );
    }
}
