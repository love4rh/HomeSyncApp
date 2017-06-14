package com.tool4us.logging;

import android.util.Log;

import static com.tool4us.util.CommonTool.CT;

import java.io.File;
import java.io.IOException;



/**
 * Logging Class
 * @author TurboK
 */
public class Logs
{
    public static final int DEBUG = 1;
    public static final int NORMAL = 2;
    public static final int WARNING = 3;
    public static final int ERROR = 4;
    public static final int FATAL = 5;
    
    // 로그에 남기는 항목을 구분하기 위한 구분자.
    public static final String  _sep_ = "`";
    
    /// Singleton 유일 객체
    private static Logs         _theOne = null;

    
    private Logs()
    {
        //
    }
    
    /**
     * 기본 설정으로 Logger 클래스를 생성하는 메소드.
     * NOTE: 기존에 설정되어 있는 Appender 클래스를 모두 제거함.
     * @param loggingFolder
     * @param progName
     */
    public static void initDefault(String loggingFolder, String progName)
    {
        if( _theOne == null )
        {
            _theOne = new Logs();
        }
    }
    
    public static void addConsoleLogger()
    {
        // Nothing to do
    }

    public static Logs instance()
    {   
        if( _theOne == null )
        {
            _theOne = new Logs();
        }

        return _theOne;
    }

    private String formatMsg(Object ... args)
    {
        if( args == null )
            return null;
        
        StringBuilder sb = new StringBuilder(128);
        
        // 쓰레드 정보 기록
        sb.append("[")
          .append(Thread.currentThread().getId())
          .append("]");

        sb.append("[");
        for(Object o : args)
        {
            if( o instanceof Object[] )
            {
                for(Object o2 : (Object[]) o)
                    sb.append(o2 == null ? "null" : o2.toString())
                      .append(_sep_);
            }
            else
            {
                sb.append(o == null ? "null" : o.toString())
                  .append(_sep_);
            }
        }
        sb.append("]");
        
        return sb.toString();
    }

    public void write(String message, int msgType)
    {
        _write(formatMsg(message), msgType);
    }
    
    private void _write(String message, int msgType)
    {
        switch( msgType )
        {
        case DEBUG:
            Log.d("HomeSync", message);
            break;
        case ERROR:
            Log.e("HomeSync", message);
            break;
        case WARNING:
            Log.w("HomeSync", message);
            break;
        case FATAL:
            Log.wtf("HomeSync", message);
            break;
        default:
            Log.i("HomeSync", message);
            break;
        }
    }
    
    public void write(int msgType, Object ... args)
    {
        _write(formatMsg(args), msgType);
    }
    
    public static void info(Object ... args)
    {
        instance().write(NORMAL, args);
    }
    
    public static void debug(Object ... args)
    {
        instance().write(DEBUG, args);
    }

    public static void error(Object ... args)
    {
        instance().write(ERROR, args);
    }

    public static void warning(Object ... args)
    {
        instance().write(WARNING, args);
    }
    
    public static void fatal(Object ... args)
    {
        instance().write(FATAL, args);
    }
    
    public static void trace(Throwable e)
    {
        StackTraceElement[] elem = e.getStackTrace();
        
        StringBuilder sb = new StringBuilder(elem.length * 64);
        
        sb.append(e)
          .append(" / ")
          .append(e.getMessage())
          ;
        
        for(int i = 0; i < elem.length; ++i)
        {
            sb.append(" << ")
              .append(elem[i]);
        }

        error(sb.toString());
    }
}
