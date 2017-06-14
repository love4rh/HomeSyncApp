package com.tool4us.net.common;

import java.io.File;



/**
 * Protocol 내 필요한 값을 정의하기 위한 클래스이며 서버와 클라이언트간 주고 받는 데이터의 단위임.
 * 
 * @author TurboK
 */
public class ProtocolElem
{
	public static byte _emptyType_	= (byte) 0xff;
	public static byte _fileType_	= (byte) 0x09;
	public static byte _headerType_	= (byte) 0x0A;
	
	/**
	 * 인수 값. _type = _fileType_, _subType = 0x01 인 경우는 파일의 전체 크기가 들어감
	 */
	private Object		_value = null;
	
	private byte 		_type = _emptyType_;
	private byte 		_subType = 0;
	

	public ProtocolElem()
	{
		_value = null;
	}
	
	public ProtocolElem(Protocol header)
	{
		int[] headerVal = new int[3];
		
		headerVal[0] = header.id();
		headerVal[1] = header.type();
		headerVal[2] = header.sizeOfParam();

		_value = headerVal;
		_type = _headerType_;
	}
	
	public ProtocolElem(Object value)
	{
		_value = value;
	}
	
	public Object getValue()
	{
		return _value;
	}
	
	public void setValue(Object value)
	{
		_value = value;
	}
	
	public void setHeaderValue(int[] headerVal)
	{
		_value = headerVal;
		_type = _headerType_;
	}
	
	public void setFileLength(long bigLength, byte subType)
	{
		_value = bigLength;
		
		_type = _fileType_;
		_subType = subType;
	}
	
	public boolean isRelatedFile()
	{
		return _type == _fileType_;
	}
	
	public void setFileSegment(byte[] value, byte subType)
	{
		setValue(value);

		_type = _fileType_;
		_subType = subType;
	}
	
	public void printDebug()
	{
		System.out.println("Type: " + valueType() + " / " + _subType + " / " + _value);
	}
	
	/**
	 * 값의 형태 반환.
	 * 0: null,
	 * 1: byteArray, 2: byte, 3: short, 4: integer, 5: long,
	 * 6: float, 7: double, 8: String, 9: File,
	 * 10: Segment (byte array를 보낼 때의 부분들. byteArray, String, File 형태일 때 사용) 
	 * @return
	 */
	public byte valueType()
	{
		if( _value == null )
		{
			return (byte) 0;
		}
		else if( _type != _emptyType_ )
		{
			return _type;
		}
		else if( _value instanceof byte[] )
		{
			return (byte) 1;
		}
		else if( _value instanceof Byte )
		{
			return (byte) 2;
		}
		else if( _value instanceof Short )
		{
			return (byte) 3;
		}
		else if( _value instanceof Integer )
		{
			return (byte) 4;
		}
		else if( _value instanceof Long )
		{
			return (byte) 5;
		}
		else if( _value instanceof Float )
		{
			return (byte) 6;
		}
		else if( _value instanceof Double )
		{
			return (byte) 7;
		}
		else if( _value instanceof String )
		{
			return (byte) 8;
		}
		else if( _value instanceof File )
		{
			return (byte) 9;
		}
		
		return 0;
	}
	
	public byte subType()
	{
		return _subType;
	}
}
