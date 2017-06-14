package com.tool4us.net.common;



/**
 * 닫혔음을 알려 주기 위한 인터페이스
 * 
 * @author TurboK
 */
public interface ICloseHandle<T>
{
    /**
     * 닫힘 이벤트 처리 메소드
     * 
     * @param closedObj     Close된 객체
     */
    public void onClosed(T closedObj);
}
