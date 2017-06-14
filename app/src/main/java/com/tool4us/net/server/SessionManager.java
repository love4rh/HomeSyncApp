package com.tool4us.net.server;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;



/**
 * 서버에 접속된 클라이언트 세션을 일괄 관리하기 위한 클래스
 * 
 * @author TurboK
 */
public class SessionManager
{
	/**
     * 접속 중인 클라이언트를 관리하기 위한 멤버
     */
    private List<ClientSession>		_connectedSession = null;
    
    /**
     * 사용하지 않는 ID 관리를 위한 멤버
     */
    private Set<Integer>			_unusedID = null;
    
    private int                     _connectedCount = 0;


    public SessionManager()
    {
    	_connectedSession = new ArrayList<ClientSession>();
    	
    	_unusedID = new TreeSet<Integer>();
    }
    
    /**
     * _connectedSession에 빈 영역이 있으면 해당 인덱스를 반환하고 없다면 -1을 반환함
     */
    private int getEmptyIndex()
    {
    	if( _unusedID.isEmpty() )
    		return -1;
    	
    	Iterator<Integer> it = _unusedID.iterator();
    	
    	return it.next();
    }

    /**
     * 새로운 세션 추가
     */
	public void add(ClientSession clientSession)
	{
		synchronized( _connectedSession )
		{
			int index = getEmptyIndex();
			
			// 새로 생성
			if( index == -1 )
			{
				index = _connectedSession.size();
				_connectedSession.add(clientSession);
			}
			else
			{
				_connectedSession.set(index, clientSession);
				_unusedID.remove(index);
			}
			
			clientSession.setClientId(index + 1);
			
			_connectedCount += 1;
		}
	}

    /**
     * 세션 제거
     * @param clientSession
     */
	public void remove(ClientSession clientSession)
	{
		synchronized( _connectedSession )
		{
			int index = clientSession.getClientID() - 1;

			_connectedSession.set(index, null);
			_unusedID.add(index);
			
			_connectedCount -= 1;
		}		
	}
	
	/**
	 * 현재 접속 중인 클라인언트 객체 반환.
	 * 새로운 List에 값을 복사해서 넘겨 줌. 동시성을 위하여 CopyOnWrite 형태로 구현함.
	 */
	public List<ClientSession> getConnectedList()
	{
		List<ClientSession> conList = new ArrayList<ClientSession>();
		
		synchronized( _connectedSession )
		{
			for(ClientSession session : _connectedSession)
			{
				// session이 null인 애들은 Connection이 종료된 애임.
				if( session != null )
					conList.add(session);
			}
		}

		return conList;
	}
	
	public int getConnectedSessionSize()
	{
	    synchronized( _connectedSession )
	    {
	        return _connectedCount;
	    }
	}
}
