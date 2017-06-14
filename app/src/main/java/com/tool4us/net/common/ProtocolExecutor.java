package com.tool4us.net.common;



/**
 * 받은 Protocol을 처리(실제 작업 수행)하기 위한 인터페이스 클래스
 * 
 * @author TurboK
 *
 */
public abstract class ProtocolExecutor
{
	private ISession		_session = null;
	
	
	public ProtocolExecutor()
	{
		
	}
	
	
	/**
     * Protocol의 작업 수행 시 ClientSession의 정보가 필요한 경우가 있을 경우 사용
     * @param cSession
     */
	public void setClientSession(ISession cSession)
	{
		_session = cSession;
	}
	
	/**
	 * 연결된 ClientSession 정보 객체 반환
	 * @return
	 */
	public ISession getClientSession()
	{
		return _session;
	}
	
	/**
     * 사용한 자원 반환
     */
	abstract public void clear();
    
    /**
     * Protocol을 구분하기 위한 ID 생성
     * @return
     */
	abstract public int generateUniqueId();
    
    /**
     * Protocol 처리
     * @param handle
     * @param msg
     * @return 여기서 입력받은 msg를 처리했다면 true, 아니라면 false
     */
	abstract public boolean executeProtocol( ProtocolHandle handle
										   , Protocol msg ) throws Exception;
}
