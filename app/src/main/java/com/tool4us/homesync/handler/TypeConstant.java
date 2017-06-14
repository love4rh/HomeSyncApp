package com.tool4us.homesync.handler;


/**
 * 프로토콜 ID 정의. 짝수로 정의할 것. 홀수는 짝수 프로토콜의 응답 프로토콜로 사용할 예정임.
 * 
 * @author TurboK
 */
public class TypeConstant
{
    /** 클라이언트가 서버가 유효한 지 체크하기 위하여 보내는 메시지 */
    public static final int HELLO       = 0x1002;
    
    /** 클라이언트의 파일 목록을 보내 동기화 하기 위하여 수정되어야 할 파일을 비교한 후 반환 */
    public static final int COMPARE     = 0x1004;
    
    /** 클라이언트에서 서버에 파일을 요청함. 요청의 결과로 RES_FILE 프로토콜 전달 */
    public static final int REQ_FILE    = 0x1006;
    
    /** 파일을 받음 */
    public static final int RES_FILE    = 0x1008;
}
