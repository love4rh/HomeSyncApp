package com.tool4us.net.common;

import static com.tool4us.util.CommonTool.CT;

import java.util.ArrayList;
import java.util.List;



/**
 * Protocol을 정의하기 위한 클래스.
 * 
 * @author TurboK
 */
public class Protocol
{
    // Unique한 ID를 생성하기 위한 ID
    private static int      _idGen = 0;
    
	private int				_id = -1;
	
	/**
	 * Protocol의 종류. 0x####과 같이 정의된 숫자 형태임.
	 */
	private int				_type = -1;
	
	private int				_paramCount = -1;
	
	private List<Object>	_paramList = null;
	
	/**
	 * 프로토콜과 관계된 부가적인 값을 잠깐 할당하여 사용하기 위한 멤버.
	 * 프로토콜 처리할 때 사용함
	 */
	private Object          _commonUse = null;
	

	public Protocol(int type)
	{
	    this(generateUniqueId(), type, -1);
	}
	
	public Protocol(int id, int type)
	{
		this(id, type, -1);
	}
	
	public Protocol(int id, int type, int paramCount)
	{
		_id = id;
		_type = type;
		_paramCount = paramCount;
		
		if( _paramCount == -1 || _paramCount > 0 )
			_paramList = new ArrayList<Object>();
	}
	
	public static Protocol newProtocol(int type, Object ... args)
	{
	    Protocol p = new Protocol(type);
	    
	    for(Object param : args)
        {
	        p.addParameter(param);
        }
	    
	    return p;
	}
	
	synchronized
	private static int generateUniqueId()
    {
        if( ++_idGen >= Integer.MAX_VALUE )
            _idGen = 1;

        return _idGen;
    }
	
	public void printDebug()
	{
		System.out.println(this + " / " + _id + " / " + _type + " / " + sizeOfParam());
		
		for(int i = 0; i < sizeOfParam(); ++i)
		{
			System.out.println("--> [" + i + "] " + _paramList.get(i));
		}
	}

	public int id()
	{
		return _id;
	}
	
	public int type()
	{
		return _type;
	}
	
	/**
	 * Hexadecimal 형태로 type 코드를 반환
	 * @return
	 */
	public String typeHex()
	{
	    return String.format("%04x", _type);
	}
	
	public void setType(int type)
	{
	    _type = type;
	}
	
	/**
	 * NetOn에서 사용하는 프로토콜을 만들 때 요청은 짝수로 해당 요청에 대한 응답은 홀수로 지정함.
	 * 이에 따라 현재 타입에 1을 더하여 응답 프로토콜로 만들어 주는 메소드임.
	 * 현재 프로토콜 형태가 홀수 일 경우는 false를 반환함.
	 */
	public boolean setReplyType()
	{
	    if( (_type % 2) == 1 )
	        return false;
	    
	    _type += 1;
	    
	    return true;
	}
	
	/**
	 * 더 추가되어야 할 Parameter가 있는 지 여부 반환
	 */
	public boolean isNeedMoreParam()
	{
		return _paramCount > 0;
	}

	/**
	 * 현재 추가된 Parameter의 개수 반환
	 * @return
	 */
	public int sizeOfParam()
	{
		return _paramList == null ? 0 : _paramList.size();
	}
	
	public Object getParameter(int index)
	{
		return _paramList == null ? null : _paramList.get(index);
	}
	
	/**
	 * 추가할 수 있는 파라미터 형태: File, String, Integer, Long, Float, Double, Byte 
	 * @param paramVal
	 */
	public void addParameter(Object paramVal)
	{
		if( _paramList == null )
			_paramList = new ArrayList<Object>();
		
		_paramList.add(paramVal);
		
		if( _paramCount >= 0 )
			--_paramCount;
	}
	
	public void setParameter(int index, Object paramVal)
	{
		if( _paramList == null )
			return;
		
		_paramList.set(index, paramVal);
	}
	
	public Object getCommonUse()
	{
	    return _commonUse;
	}
	
	public void setCommonUse(Object obj)
	{
	    _commonUse = obj;
	}
	
	/**
	 * Protocol에 추가된 인수를 "|"로 구분된 문자형태로 변경하여 반환함
	 * @return
	 */
	public String getParameterAsString()
	{
	    // 입력 받은 인수 남기기
        StringBuilder sb = new StringBuilder(this.sizeOfParam() * 32);
        
        for(int i = 0; i < this.sizeOfParam(); ++i)
        {
            Object paramVal = this.getParameter(i);
            
            if( i != 0 )
                sb.append("`");
            
            if( paramVal == null ) 
                sb.append( "null" );
            else if( paramVal instanceof String )
                sb.append( CT.makeNoWrapped((String) paramVal) );
            else
                sb.append( paramVal.toString() );
        }
        
        return sb.toString();
	}
	
	public List<Object> getParamList()
	{
	    return _paramList;
	}

	/**
	 * 응답용 프로토콜 만들기.
	 * type이 +1된 Protocol 객체를 만들어 반환.
	 * @return
	 */
    public Protocol createReply()
    {
        return new Protocol(type() + 1);
    }
}
