package com.tool4us.net.handler;

import static com.tool4us.net.common.ErrorCode.*;
import static com.tool4us.net.common.NetSetting.NS;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import org.reflections.Reflections;

import com.tool4us.homesync.handler.CompareEvent;
import com.tool4us.homesync.handler.HelloEvent;
import com.tool4us.homesync.handler.ReceiveFileEvent;
import com.tool4us.homesync.handler.RequestFileEvent;
import com.tool4us.logging.Logs;
import com.tool4us.net.common.ErrorCode;
import com.tool4us.net.common.ISession;
import com.tool4us.net.common.Protocol;
import com.tool4us.net.common.ProtocolExecutor;
import com.tool4us.net.common.ProtocolHandle;



/**
 * 별도로 프로토콜 처리용 핸들러를 정의하고 이 클래스들을 읽어 자동으로 Protocol 핸들러로 매핑하여
 * 서버와 클라이언트의 요청을 처리하도록 구성한 클래스임.
 * 특정 패키지로 핸들러 클래스를 정의해 놓고
 * CommonExecutor.newInstance(패키지명)을 호출하여 생성된 Executor 클래스를 이용하면 됨.
 * 패키지 내 프로토콜 핸들러 클래스는 MessageHandler 클래스를 상속 받고,
 * MessageDefine Annotation으로 id가 정의되어야 함. id는 정수로 정의하고 중복되면 안됨.
 *  
 * @author TurboK
 */
public class CommonExecutor extends ProtocolExecutor
{
    // Unique한 ID를 생성하기 위한 ID
    private Integer         _idGen = new Integer(0);
    
    /**
     * 프로토콜 Type --> Object[] {프로토콜 수행 객체, 권한코드}.
     * 사용자 권한 코드가 여기에 정의된 권한코드보다 클 경우만 수행 가능함.
     */
    private Map<Integer, Object[]>    _localJobMap = null;
    
    
    public static CommonExecutor newInstance(String pkgName)
    {
        CommonExecutor exetor = new CommonExecutor();
        
        exetor.setupHandler(pkgName);
        
        return exetor;
    }
    
    
    private CommonExecutor()
    {
        _localJobMap = new ConcurrentSkipListMap<Integer, Object[]>();
    }
    
    protected void setupHandler(String pkgName)
    {
        Class<?>[] clazzes = ExecutorGenHelper.getClasses(pkgName);

        if( clazzes == null || clazzes.length == 0 )
        {
            clazzes = new Class<?>[] { HelloEvent.class, RequestFileEvent.class, ReceiveFileEvent.class, CompareEvent.class };
        }
        
        for(Class<?> clazz : clazzes)
        {
            if( clazz.getSuperclass() != MessageHandler.class )
                continue;
                    
            MessageDefine annot = clazz.getAnnotation(MessageDefine.class);
            
            if( annot == null )
                continue;
            
            Constructor<?> constructor = null;
            
            try
            {
                constructor = clazz.getConstructor(ProtocolExecutor.class);
                
                if( constructor != null )
                {
                    // Protocol Type을 잘못 지정한 것을 막기 위하여 넣었음.
                    if( _localJobMap.containsKey(annot.id()) )
                        throw new IllegalArgumentException("Duplicated Protocol Type: " + annot.id());

                    _localJobMap.put(annot.id()
                            , new Object[] {(MessageHandler) constructor.newInstance(this), annot.level()} );
                }
            }
            catch( Exception xe )
            {
                Logs.trace(xe); 
            }
        }
    }
    
    /**
     * 프로토콜 핸들러를 Reflection을 이용하지 않고 수동으로 추가
     * @param protocolType
     * @param handler
     * @param level
     */
    public void addEventHandler(int protocolType, MessageHandler handler, int level)
    {
        if( _localJobMap.containsKey(protocolType) )
        {
            throw new IllegalArgumentException("Protocol [" + protocolType + "] already defined.");
        }
        
        _localJobMap.put(protocolType, new Object[] {handler, level});
    }
    
    /**
     * jobType에 해당하는 {MessageHandler, Level Code) 반환. 없으면 null
     * @param jobType
     * @return
     */
    private Object[] getHandler(int jobType)
    {
        return _localJobMap.get(jobType);
    }
    
    @Override
    public void clear()
    {
        for(Entry<Integer, Object[]> elem : _localJobMap.entrySet())
        {
            Object[] jobHandle = elem.getValue();
            
            ((MessageHandler)jobHandle[0]).clear();
        }
    }
    
    @Override
    public int generateUniqueId()
    {
        if( ++_idGen >= Integer.MAX_VALUE )
            _idGen = 1;
        
        return _idGen;
    }
    
    /**
     * 프로토콜이 정상적인 결과를 반환한 것이지 체크.
     * 
     * @param msg
     * @return 오류가 있는 반환값이라면 true, 정상이라면 false반환
     * @throws Exception
     */
    static
    public boolean hasError(Protocol msg) throws Exception
    {
        if( msg.sizeOfParam() >= 3 )
        {
            byte b = (Byte) msg.getParameter(0);
            
            if( b == (byte) 0x00 )
            {
                System.out.println("Error returned: "
                        + msg.getParameter(1)
                        + " / " + msg.getParameter(2) );

                return true;
            }
        }
        
        return false;
    }

    static
    public void writeReplyProtocol(Protocol rMsg, ErrorCode errCode)
    {
        rMsg.addParameter((byte) (errCode == errSuccess ? 0x01 : 0x00) ); // 성공여부
        
        rMsg.addParameter(errCode.code());
        rMsg.addParameter(errCode.message());
    }
    
    static
    public void writeReplyProtocol(Protocol rMsg, int errCode)
    {
        rMsg.addParameter((byte) (errCode == errSuccess.code() ? 0x01 : 0x00) ); // 성공여부
        
        rMsg.addParameter(errCode);
        rMsg.addParameter("Error " + errCode + " occurred.");
    }
    
    protected void sendReplyError( ProtocolHandle handle
                                 , int id, int type
                                 , ErrorCode errCode ) throws Exception
    {
        Protocol rMsg = new Protocol(id, type);
        
        writeReplyProtocol(rMsg, errCode);
        
        handle.send(rMsg);
    }
    
    protected void sendReplyError( ProtocolHandle handle
                               , int id, int type
                               , String errMsg ) throws Exception
    {
        Protocol rMsg = new Protocol(id, type);
        
        rMsg.addParameter( (byte) 0x00 );
        rMsg.addParameter(errUnknown.code());
        rMsg.addParameter(errMsg);
        
        handle.send(rMsg);
    }
    
    @Override
    public boolean executeProtocol(ProtocolHandle handle, Protocol msg) throws Exception
    {
        int jobType = msg.type();
        
        // Local Map에서 찾아보고
        Object[] rawHandle = getHandler(jobType);

        // Job 객체가 있으면 처리
        if( rawHandle != null )
        {
            MessageHandler jobWorker = (MessageHandler) rawHandle[0];
            ISession userSession = this.getClientSession();
            
            ErrorCode errCode = errSuccess;

            try
            {
                try
                {   
                    errCode = jobWorker.setAndCheck(msg, userSession);
                }
                catch( ClassCastException castErr )
                {
                    // Query문 실행하다가 여기서 걸리면 Query문 실행 루틴 살펴 보아야 함. 
                    errCode = errApiInvalidParameter;
                }

                if( errCode != errSuccess )
                    sendReplyError(handle, msg.id(), msg.type() + 1, errCode);
                else
                {
                    // TODO 작업 Queue에 던지고 그냥 빠져 나갈까?
                    Protocol rMsg = jobWorker.work(msg, userSession);

                    if( rMsg != null && this.getClientSession().isValid() )
                    {
                        handle.send(rMsg);
                    }
                }
            }
            catch(Exception xe)
            {
                NS.trace(userSession, xe);
                
                sendReplyError(handle, msg.id(), msg.type() + 1, xe + ": " + xe.getMessage());
            }            

            return true;
        }

        return false;
    }
}

class ExecutorGenHelper
{
    private static Map<String, Class<?>[]>      _classes = new ConcurrentSkipListMap<String, Class<?>[]>();
    
    private ExecutorGenHelper()
    {
        //
    }
    
    /**
     * 지정한 패키지 내에 있는 모든 Class를 찾아 반환하는 메소드
     * @param pkgName
     * @return
     * @throws ClassNotFoundException
     */
    public static Class<?>[] getClasses(String pkgName)
    {
        if( _classes.containsKey(pkgName) )
            return _classes.get(pkgName);
        
        Reflections reflections = new Reflections(pkgName);

        Set<Class<? extends MessageHandler>> allClasses = reflections.getSubTypesOf(MessageHandler.class);
        
        int i = 0;
        Class<?>[] classList = new Class<?>[allClasses.size()];
        
        for(Class<? extends MessageHandler> klazz : allClasses)
            classList[i++] = klazz;
        
        _classes.put(pkgName, classList);

        return classList;
    }
}
