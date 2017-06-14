package com.tool4us.util;

import android.content.pm.PackageManager;

import java.io.File;
import java.io.IOException;


/**
 * This class collects the functions to be used in common.
 * 공통 기능으로 자주 사용되는 기능함수들 모음
 * 
 * import static com.tool4us.util.CommonTool.CT;
 * 
 * @author TurboK
 */
public enum CommonTool
{
    CT;
    
    private CommonTool()
    {
        //
    }

    /**
     * returns the folder path that the application is in.
     * Application의 설치 경로 반환.
     * @return
     */
    public String getAppPath()
    {   
        String modulePath = null;
        
        try
        {
            modulePath = (new File(".")).getCanonicalPath();
        }
        catch( IOException xe )
        {
            xe.printStackTrace();
        }
        
        return modulePath;
    }
    
    public String getAppPath(String sub)
    {
        String appPath = getAppPath();
        
        if( sub != null )
        {
            appPath += File.separator + sub;
        }
        
        return appPath;
    }

    /**
     * Returns the defined value if the value is null.
     * 값이 null인 경우 지정된 다른 값을 반환
     * @param value
     * @param ifnull
     * @return
     */
    public Object nvl(Object value, Object ifnull)
    {
        return value == null ? ifnull : value;
    }
    
    /**
     * Returns the string that is replaced line-feed or carriage-return
     * with the character '\n' or '\r'.
     * As a result, this method makes multi-lines string single-line string.
     * @param text
     * @return
     */
    public String makeNoWrapped(String text)
    {
        if( text == null || text.isEmpty() )
            return "";

        return text.replace("\r\n", "\\n").replace("\r", "\\n").replace("\n", "\\n");
    }
    
    public String makeWrapped(String text)
    {
        if( text == null || text.isEmpty() )
            return "";

        return text.replace("\\n", "\n");
    }
    
    /**
     * 문자열로 합치기
     * @param args
     * @return
     */
    public String concat(Object ... args)
    {
        if( args == null )
            return null;
        
        StringBuilder sb = new StringBuilder();
        
        for(Object o : args)
            sb.append(o == null ? "null" : o.toString());
        
        return sb.toString();
    }
}
