package com.tool4us.net.common;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Date;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;



/**
 * Protocol 클래스를 ByteBuf로 encoding 하는 클래스.
 * 현재는 사용하지 않음. ChunkedWriteHandler를 대신 이용하고 있음
 * 
 * @author TurboK
 */
public class ProtocolEncoder extends MessageToByteEncoder<Protocol>
{
    
    @Override
    protected void encode(ChannelHandlerContext ctx, Protocol msg, ByteBuf out) throws Exception
    {   
        out.writeByte(ProtocolElem._headerType_);
        out.writeInt(msg.id());
        out.writeInt(msg.type());
        out.writeInt(msg.sizeOfParam());
        
        // Parameter 보내기
        for(int i = 0; i < msg.sizeOfParam() && ctx.channel().isActive(); ++i)
        {
            Object paramVal = msg.getParameter(i);
            
            // 파일을 보내야 할 경우라면 1M정도씩 보냄
            if( paramVal instanceof File )
            {
                final int chunkSize = 1024 * 1024;  // 1MB
                File file = (File) paramVal;
                
                long fileLength = file.length();
                
                // 파일 헤더 정보 기록
                writeFileHeader(out, fileLength);
                
                byte[] segment = new byte[chunkSize];
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                
                //long lenCheck = 0;
                while( fileLength > 0 && ctx.channel().isActive() )
                {
                    byte subType = (byte) 0x02;
                    int readLen = raf.read(segment);
                    
                    fileLength -= readLen;
                    //lenCheck += readLen;
                    
                    if( fileLength <= 0 )
                        subType = (byte) 0x03;
                    
                    // System.out.println("Sending File Length: " + readLen);

                    // 파일 기록
                    writeFile(out, subType, segment, readLen);
                }
                
                // System.out.println("SEND FILE LENGTH: " + lenCheck);

                raf.close();
            }
            else
            {
                writeValue(out, paramVal);
            }
            
            // System.out.println("Send Param: " + i + " / " + paramVal);
        }
    }
    
    private void writeValue(ByteBuf out, Object value)
    {
        if( value == null )
        {
            out.writeByte(0);
        }
        else if( value instanceof byte[] )
        {
            byte[] rVal = (byte[]) value;
            
            out.writeByte(1);
            out.writeInt(rVal.length);
            out.writeBytes(rVal);
        }
        else if( value instanceof Byte )
        {
            out.writeByte(2);
            out.writeByte((Byte) value);
        }
        else if( value instanceof Boolean )
        {
            out.writeByte(2);
            out.writeByte((byte) ((Boolean) value ? 1 : 0));
        }
        else if( value instanceof Short )
        {
            out.writeByte(3);
            out.writeShort((Short) value);
        }
        else if( value instanceof Integer )
        {
            out.writeByte(4);
            out.writeInt((Integer) value);
        }
        else if( value instanceof Long )
        {
            out.writeByte(5);
            out.writeLong((Long) value);
        }
        else if( value instanceof Float )
        {
            out.writeByte(6);
            out.writeFloat((Float) value);
        }
        else if( value instanceof Double )
        {
            out.writeByte(7);
            out.writeDouble((Double) value);
        }
        else if( value instanceof Date )
        {
            out.writeByte(11);
            out.writeLong(((Date) value).getTime());
        }
        else if( value instanceof String )
        {
            byte[] rVal = ((String) value).getBytes();
            
            out.writeByte(8);
            
            if( rVal == null || rVal.length == 0 )
                out.writeInt((int) 0);
            else
            {
                out.writeInt(rVal.length);
                out.writeBytes(rVal);
            }
        }
        else
        {
            // 받은 value가 POJO라고 생각하고 Reflection 이용해서 멤버들을 보낼까?
            // 패키지.클래스명을 보내고 멤버 변수를 순차적으로 하나씩 보내고 난 뒤
            // 끝났음을 마킹함. 이건 좀 생각해 보자... 아직 이렇게 까지 할 필요는 못 느낌
            System.err.println("UNSUPPRTED TYPE. ASSIGN NULL");

            out.writeByte(0);
        }
    }
    
    private void writeFileHeader(ByteBuf out, long fileLength)
    {   
        out.writeByte(ProtocolElem._fileType_);
        out.writeByte((byte) 0x01);
            
        // 전체 길이 넣기
        out.writeLong(fileLength);
    }
    
    private void writeFile(ByteBuf out, byte subType, byte[] value, int realLen)
    {   
        out.writeByte(ProtocolElem._fileType_);
        out.writeByte(subType);
        
        // Segment를 보내거나(0x02) 마지막 것을 보내거나(0x03) 작어서 전체를 보내는 경우(0x04)
        out.writeInt(realLen);
        out.writeBytes(value, 0, realLen);
    }
}
