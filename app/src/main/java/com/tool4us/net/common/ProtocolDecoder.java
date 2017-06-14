package com.tool4us.net.common;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.ReplayingDecoder;



/**
 * ByteBuf에서 ProtocolElem 클래스를 추출하기 위한 Decoder 클래스
 * 
 * @author TurboK
 */
public class ProtocolDecoder extends ReplayingDecoder<ProtocolDecoder.State> // (1)
{
	/**
	 * Decoder의 상태를 나타내기 위한 enumeration.
	 */
	enum State
	{
          PARAM_TYPE		//< 인수의 값을 읽는 단계
        , PARAM_SUBTYPE		//< 파일일 경우 부가 단계 코드 읽기
        , PARAM_LENGTH 		//< 인수의 값의 크기를 읽는 단계
        , PARAM_VALUE		//< 인수의 실제 값을 읽는 단계
        , PARAM_DONE		//< 인수값을 모두 읽어 왔음
    }
	
	/**
	 * 현재 Decoding 중인 Protocol 객체
	 */
	private ProtocolElem	_curElem = null;
	
	/**
	 * 현재 읽고 있는 인수의 값 형태
	 * 1: byteArray, 2: byte, 3: short, 4: integer, 5: long
	 * 6: float, 7: double, 8: String, 9: File, 10: Header, 11: Date
	 * 0: null
	 */
	private int				_valueType = 0;
	
	/**
	 * 파일을 읽는 경우 단계 코드
	 * 1: 전체 길이만 보냄
	 * 2: 파일의 일부 내용을 보냄. 보낸 길이가 뒤에 옮
	 * 3: 마지막 부분을 보냄. 한번에 전체를 보내는 경우에도 이 코드로 넘어옴
	 */
	private int				_subType = 0;
	
	/**
	 * 읽어야 하는 ByteArray
	 */
	private byte[]			_valueBuf = null;
	
	/**
	 * 읽어야 하는 데이터의 byte 수
	 */
	private int				_valueLen = 0;

	
	public ProtocolDecoder()
	{
		super(State.PARAM_TYPE);
	}
	
	private void init()
	{
		_curElem = null;
		_valueType = 0;
		_valueBuf = null;
		_valueLen = 0;
		_subType = 0;
		
		checkpoint(State.PARAM_TYPE);
	}
	
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    {
     	if( !in.isReadable() )
    		return;
     	
    	switch( state() )
    	{
    	case PARAM_TYPE:
    		assert _curElem == null;
    		
    		// 파라미터의 값의 종류를 읽어 오는 단계
    		if( in.readableBytes() >= 1 )
    		{
    			_valueType = in.readUnsignedByte();
    			
    			_curElem = new ProtocolElem();
    			
    			// byteArray와 String은 다음에 실제 데이터의 길이가 나와 있으므로 4byte를 읽어야 함
    			if( _valueType == 1 || _valueType == 8 )
    			{
    				checkpoint( State.PARAM_LENGTH );
    			}
    			else if( _valueType == 0 )
    			{
    				_curElem.setValue(null);
    				checkpoint( State.PARAM_DONE );
    			}
    			// 파일일 경우는 파일 크기를 읽는 루틴으로 이동
    			else if( _valueType == 9 )
    			{
    				checkpoint( State.PARAM_SUBTYPE );
    			}
    			else
    				checkpoint( State.PARAM_VALUE );
    		}
    		break;
    		
    	case PARAM_SUBTYPE:
    		if( in.readableBytes() >= 1 )
    		{
	    		_subType = in.readByte();
	    		
	    		if( _subType != 1 && _subType != 2 && _subType != 3 )
	    			throw new CorruptedFrameException("Invalid subtype code: " + _subType);

	    		checkpoint( State.PARAM_LENGTH );
	    	} break;
    		
    	case PARAM_LENGTH:
    		{
    			boolean lengthRead = false;
    			
    			// 파일일 경우
	    		if( _valueType == 9 )
	    		{
	    			// 파일의 크기를 읽는 경우임
	    			if( _subType == 1 && in.readableBytes() >= 8 )
	    			{
	    				_curElem.setFileLength( in.readLong(), (byte) _subType );
	    				checkpoint( State.PARAM_DONE );
	    			}
	    			else if( _subType != 1 && in.readableBytes() >= 4 )
	    			    lengthRead = true;
	    		}
	    		else
	    		    lengthRead = true;
	    		
	    		if( lengthRead && in.readableBytes() >= 4 )
	    		{
	    			_valueLen = in.readInt();
	    			if( _valueLen == 0 )
	    			{
	    			    if( _valueType == 9 )
	    			    {
	    			        _curElem.setFileLength(0, (byte)_subType);
	    			        _curElem.setValue(0);
	    			    }
	    			    else
	    			        _curElem.setValue(null);
	    			    
	    			    checkpoint( State.PARAM_DONE );
	    			}
	    			else
	    			{
    	    			_valueBuf = new byte[_valueLen];
    	    			checkpoint( State.PARAM_VALUE );
	    			}
	    		}
    		} break;
    		
    	case PARAM_VALUE:
    		{
	    		boolean endCheck = true;
	    		
	    		switch( _valueType )
	    		{
	    		case 11:
	    		    if( in.readableBytes() >= 8 )
                        _curElem.setValue( new Date(new Long(in.readLong())) );
                    break;
	    		case 10:	// Protocol Header
	    			if( in.readableBytes() >= 12 )
	    			{
		    			int[] headerVal = new int[3];
		    			headerVal[0] = in.readInt();
		    			headerVal[1] = in.readInt();
		    			headerVal[2] = in.readInt();
		    			
		    			_curElem.setHeaderValue(headerVal);
		    		} break;
	    		case 1:	// byteArray
	    		case 8:	// String
	    		case 9:	// File
		    		{
		    			int readLen = (int) _valueLen;
		    			int availableLen = in.readableBytes();
		    			if( availableLen < readLen )
		    				readLen = availableLen;
		    				
		    			in.readBytes(_valueBuf, _valueBuf.length - (int) _valueLen, readLen);
		    			_valueLen -= readLen;
		    			
		    			//if( _valueType == 9 )
		    			//	System.out.println("Reading Segment: " + readLen);
		    			
		    			// 모두 읽었음
		    			if( _valueLen == 0 )
		    			{
		    				if( _valueType == 1 )
		    				{
		    					_curElem.setValue( _valueBuf );
		    				}
		    				else if( _valueType == 9 )
		    				{
		    					_curElem.setFileSegment( _valueBuf, (byte) _subType );
		    				}
		    				else if( _valueType == 8 )
								try
		    					{
									_curElem.setValue( new String(_valueBuf, "UTF-8") );
								}
		    					catch( UnsupportedEncodingException xe )
								{
									xe.printStackTrace();
									_curElem.setValue( (String) null );
								}
		    				
		    				_valueBuf = null;
		    			}
		    			// 모두 읽지 않은 경우는 상태가 PARAM_VALUE로 유지되어야 함
		    			else
		    				endCheck = false;
		    		} break;
		    		
	    		case 2:	// byte
	    			if( in.readableBytes() >= 1 )
	    				_curElem.setValue( new Byte(in.readByte()) );
	    			break;
	    		case 3: // short
	    			if( in.readableBytes() >= 2 )
	    				_curElem.setValue( new Short(in.readShort()) );
	    			break;
	    		case 4: // integer
	    			if( in.readableBytes() >= 4 )
	    				_curElem.setValue( new Integer(in.readInt()) );
	    			break;
	    		case 5: // long
	    			if( in.readableBytes() >= 8 )
	    				_curElem.setValue( new Long(in.readLong()) );
	    			break;
	    		case 6: // float
	    			if( in.readableBytes() >= 4 )
	    				_curElem.setValue( new Float(in.readFloat()) );
	    			break;
	    		case 7: // double
	    			if( in.readableBytes() >= 8 )
	    				_curElem.setValue( new Double(in.readDouble()) );
	    			break;
	    		}
	    		
	    		if( endCheck )
	    			checkpoint( State.PARAM_DONE );
    		} break;
    		
		default:
			throw new CorruptedFrameException("Unsupported type code: " + _valueType);
    	}
    	
    	// ProtocolElem 객체 생성이 완료되었다면
    	if( state() == State.PARAM_DONE )
    	{
    		out.add(_curElem);
    		init();
    	}
    }
    
    @Override
    protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
    {
        decode(ctx, in, out);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
    	init();
    	
        ctx.fireExceptionCaught(cause);
    }
}
