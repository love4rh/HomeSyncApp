package com.tool4us.net.common;

import static com.tool4us.util.CommonTool.CT;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import com.tool4us.logging.Logs;



/**
 * com.tool4us.net 패키지 내 서버 및 클라이언트 클래스를 사용하기 위하여 필요한
 * 기본 정보 및 설정을 관리하기 위한 클래스
 * 
 * import static com.tool4us.net.common.NetSetting.NS;
 * 
 * @author TurboK
 */
public enum NetSetting
{
    NS;
    
    /**
     * 작업용 임시폴더. 따로 설정하지 않으면 설치 폴더 밑의 temporary 폴더를 이용함.
     * 환경변수: WorkingDir
     */
    private File            _workingDir = null;

    /**
     * 디버그 모드 여부. 환경변수: DebugOn
     * Run-Time 시 변경 가능함.
     */
    private boolean         _debugOn = true;
    
    /**
     * 이 시간 동안 아무런 반응이 없는 경우 Connection을 종료함. 초단위 
     */
    private int             _readTimeOut = -1;
    
    /**
     * Protocol을 보낼 때 사용할 Chuck의 크기
     */
    private int             _chunkedSize = 65536;   // 1024 * 1024; // 1M
    
    
    private NetSetting()
    {
        // Default Setting
        initialize(CT.getAppPath() + File.separator + "temporary", false, -1);
    }
    
    /**
     * 네트워크 라이브러리 파라미터 초기 설정
     * @param workingDir    임시 작업 폴더. 파일 등을 받을 때 사용할 임시 디렉토리 지정.
     * @param debugMsg      상세 로깅을 할 지 여부
     * @param readTimeOut   일정 시간 요청이 없을 때 세션을 종료할 기준 시간. 초단위.
     */
    public void initialize(String workingDir, boolean debugMsg, int readTimeOut)
    {
        _debugOn = debugMsg;
        _workingDir = new File( workingDir );
        
        if( !_workingDir.exists() )
            _workingDir.mkdir();
        
        _readTimeOut = readTimeOut;
    }
    
    /**
     * 실행되고 있는 장비의 유효한 IP 주소 반환. xxx.xxx.xxx.xxx
     * @return
     */
    public String localAddress()
    {
        try
        {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
            {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress() && inetAddress.isSiteLocalAddress())
                    {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        }
        catch (Exception xe)
        {
            xe.printStackTrace();
        }
        
        return null;
    }
    
    public boolean isValid()
    {
        return _workingDir != null;
    }
    
    public void setDebugMode(boolean debugOn)
    {
        _debugOn = debugOn;
    }
    
    /**
     * 디버깅 모드가 설정되어 있는 지 여부 반환
     * @return
     */
    public boolean isOnDebug()
    {
        return _debugOn;
    }
    
    public int chunkedSize()
    {
        return _chunkedSize;
    }
    
    public int getReadTimeOut()
    {
        return _readTimeOut;
    }
    
    /**
     * 임시 파일 객체를 만들어서 반환.
     * 
     * @param prefix    파일명 접두어
     * @param suffix    파일명 접미어
     * @param deleteOnExit
     * @return  File 객체를 반환함
     * @throws IOException
     */
    public File createTempFile( String prefix, String suffix
                              , boolean deleteOnExit ) throws IOException
    {
        if( _workingDir == null )
            throw new IOException("You should call NetSetting.initialize() first.");
        
        File tempFile = File.createTempFile(prefix, suffix, _workingDir);

        if( deleteOnExit )
            tempFile.deleteOnExit();
        
        return tempFile;
    }
    
    private String sessionDesc(ISession session)
    {
        return session == null ? "n/b": session.getClientDescription();
    }
    
    public void debug(ISession session, Object ... args)
    {
        if( !_debugOn ) return;
        
        Logs.debug(sessionDesc(session), args);
    }
    
    public void info(ISession session, Object ... args)
    {
        Logs.info(sessionDesc(session), args);
    }
    
    public void warn(ISession session, Object ... args)
    {
        Logs.warning(sessionDesc(session), args);
    }
    
    public void error(ISession session, Object ... args)
    {
        Logs.error(sessionDesc(session), args);
    }
    
    public void trace(ISession session, Throwable e)
    {
        StackTraceElement[] elem = e.getStackTrace();
        
        StringBuilder sb = new StringBuilder(elem.length * 64);
        
        sb.append(e);
        sb.append(" / ");
        sb.append(e.getMessage());
        
        for(int i = 0; i < elem.length; ++i)
        {
           sb.append(" << ")
             .append(elem[i]);
        }

        error(session, sb.toString());
    }
}
