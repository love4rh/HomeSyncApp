package com.tool4us.net.handler;

import com.tool4us.net.common.ErrorCode;
import com.tool4us.net.common.ISession;
import com.tool4us.net.common.Protocol;
import com.tool4us.net.common.ProtocolExecutor;


/**
 * 서버 / 클라이언트 간 패킷(Protocol 클래스)을 처리하기 위한 핸들러 베이스 클래스
 * 
 * @author TurboK
 */
public abstract class MessageHandler
{
    /**
     * 이 작업 객체를 호출한(소유한) ProtocolExecutor 클래스의 레퍼런스
     */
    private ProtocolExecutor    _executor = null;
    
    
    public MessageHandler(ProtocolExecutor executor)
    {
        _executor = executor;
    }
    
    public ProtocolExecutor getExecutor()
    {
        return _executor;
    }
    
    public int getUniqueID()
    {
        return _executor.generateUniqueId();
    }
    
    /**
     * rMsg에 ErrorCode를 쓰기 기능 수행
     * @param rMsg
     * @param errCode
     */
    public void writeReplyProtocol(Protocol rMsg, ErrorCode errCode)
    {
        CommonExecutor.writeReplyProtocol(rMsg, errCode);
    }
    
    public void writeReplyProtocol(Protocol rMsg, int errCode)
    {
        CommonExecutor.writeReplyProtocol(rMsg, errCode);
    }
    

    /**
     * 사용한 자원 반환. 멤버 변수 등이 있는 경우 구현 필요.
     */
    public abstract void clear();

    /**
     * 프로토콜의 유효성 판단. 제대로 입력된 프로토콜인지 체크함
     * 
     * @param msg   프로토콜의 유효성 검사 및 인수 할당
     * @return 정상적인 프로토콜이라면 ErrorCode.errSuccess를 반환,
     *         유효하지 않다면 ErrorCode 내 다른 에러 코드 반환
     */
    public abstract ErrorCode setAndCheck(Protocol msg, ISession session) throws Exception;
    
    /**
     * 프로토콜 실제 작업 실행
     * 
     * @param msg       수행할 프로토콜
     * @param session   작업을 요청한 클라이언트 정보
     * @return 결과를 반환하기 위한 프로토콜 객체 반환. null이면 반환할 결과가 없는 것으로 간주함.
     */
    public abstract Protocol work(Protocol msg, ISession session) throws Exception;
}
