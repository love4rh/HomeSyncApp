package com.tool4us.net.common;

import java.util.Date;



/**
 * 접속한 클라이언트 세션의 정보를 반환하기 위한 인터페이스
 * 
 * @author TurboK
 */
public interface ISession
{
	/** 최근 접속한 시간 반환 */
	public Date getConnectedTime();
	
	/** 접속한 클라이언트의 IP 반환 */
	public String getClientIP();
	
	/** 접속한 사용자의 정보를 한 문자열로 표현하여 반환 */
	public String getClientDescription();
	
	/** 유효한 상태에 있는 지 여부 반환. 접속이 종료되거나 한 경우 false를 반환해야 함 */
	public boolean isValid();
	
	/** 세션이 살아 있는 동안 공유하기 위하여 저장된 데이터 반환 */
	public Object getValue(String key);
	
	/** 세션이 살아 있는 동안 공유하기 위한 데이터 저장 */
	public void setValue(String key, Object value);
	
	/** 세션 값 제거 */
	public void removeValue(String key);
	
	/** 사용자 레벨 반환. 권한 관리를 위하여 사용함. */
	public int getUserLevel();
}
