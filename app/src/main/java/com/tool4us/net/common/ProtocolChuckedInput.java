package com.tool4us.net.common;

import static com.tool4us.net.common.NetSetting.NS;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Date;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;



/**
 * Protocol이 클 경우 조금씩 나눠서 보내기 위한 클래스
 * 
 * @author TurboK
 */
public class ProtocolChuckedInput implements ChunkedInput<ByteBuf>
{
    private Protocol        _msg = null;
    
    /**
     * 쓰기 단계 - 0: 초기, 1: 쓰는 중, 99: 쓰기 완료
     */
    private int             _step = 0;
    
    private int             _paramIndex = 0;
    
    /**
     * 파일을 보내야 할 경우 해당 파일 핸들
     */
    private RandomAccessFile    _raf = null;
    
    private long                _fileLength = 0;
    
    private byte[]              _segment = null;
    
    private boolean             _onServer = false;
    
    static private ByteBufAllocator    _alloc = null;
    
    
    public ProtocolChuckedInput(Protocol msg, boolean onServer)
    {
        _msg = msg;
        _onServer = onServer;
        
        if( onServer && _alloc == null )
            _alloc = new PooledByteBufAllocator();
    }
    
    private void writeProtocolHeader(ByteBuf out)
    {   
        out.writeByte(ProtocolElem._headerType_);
        out.writeInt(_msg.id());
        out.writeInt(_msg.type());
        out.writeInt(_msg.sizeOfParam());
    }
    
    private void writeValue(ByteBuf out, Object value)
    {
        if( value == null )
        {
            out.writeByte(0);
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
        else if( value instanceof Byte )
        {
            out.writeByte(2);
            out.writeByte((Byte) value);
        }
        else if( value instanceof byte[] )
        {
            byte[] rVal = (byte[]) value;
            
            out.writeByte(1);
            out.writeInt(rVal.length);
            out.writeBytes(rVal);
        }
        else if( value instanceof Date )
        {
            out.writeByte(11);
            out.writeLong(((Date) value).getTime());
        }
        else if( value instanceof Integer )
        {
            out.writeByte(4);
            out.writeInt((Integer) value);
        }
        else if( value instanceof Double )
        {
            out.writeByte(7);
            out.writeDouble((Double) value);
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
    
    /**
     * 파일을 다 읽어서 보낸 경우 true을 반환함.
     */
    private boolean writeFileContent(ByteBuf out) throws Exception
    {
        int readLen = _raf.read(_segment);
        
        _fileLength -= readLen;
        
        boolean done =  _fileLength <= 0;

        // 파일 기록
        writeFile(out, (byte) (done ? 0x03 : 0x02), _segment, readLen);
        
        if( done )
        {
            _raf.close();
            _raf = null;
        }
        
        return done;
    }
    
    @Override
    public boolean isEndOfInput() throws Exception
    {
        return _step == 99;
    }

    @Override
    public void close() throws Exception
    {
        _segment = null;
        
        if( _raf != null )
            _raf.close();
        _raf = null;
    }
    
    @Override
    public ByteBuf readChunk(ByteBufAllocator allocator) throws Exception
    {
        return procReadChunk( allocator.heapBuffer(NS.chunkedSize()) );
    }

    @Override
    public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception
    {
        ByteBuf out = null;
        
        if( _onServer && _alloc != null )
            out = _alloc.heapBuffer(NS.chunkedSize());
        else
            out = ctx.alloc().heapBuffer(NS.chunkedSize());
        
        return procReadChunk(out);
    }
    
    private ByteBuf procReadChunk(ByteBuf out) throws Exception
    {   
        // 처음 단계에 헤더 정보 기록하기
        if( _step == 0 )
        {
            writeProtocolHeader(out);
            _step = 1;
        }

        // 파일을 보내던 것이 있는 경우임.
        if( _raf != null && _segment != null )
        {
            if( writeFileContent(out) )
                _paramIndex += 1;
        }
        
        while( _paramIndex < _msg.sizeOfParam()
                && out.writerIndex() < NS.chunkedSize() )
        {
            Object paramVal = _msg.getParameter(_paramIndex);
            
            // 파일을 보내야 할 경우라면 1M정도씩 보냄
            if( paramVal instanceof File )
            {
                File file = (File) paramVal;
                
                _fileLength = file.length();
                
                // 파일 헤더 정보 기록
                writeFileHeader(out, _fileLength);
                
                if( _fileLength > 0 )
                {
                    if( _segment == null )
                        _segment = new byte[NS.chunkedSize()];
                    
                    _raf = new RandomAccessFile(file, "r");
                    
                    if( writeFileContent(out) )
                        _paramIndex += 1;
                }
                // 0이라면
                else
                {
                    out.writeByte(ProtocolElem._fileType_);
                    out.writeByte((byte) 0x03);

                    // Segment를 보내거나(0x02) 마지막 것을 보내거나(0x03) 작어서 전체를 보내는 경우(0x04)
                    out.writeInt(0);
                    
                    _paramIndex += 1;
                }
            }
            else
            {
                writeValue(out, paramVal);
                _paramIndex += 1;
            }
        }

        if( _paramIndex >= _msg.sizeOfParam() )
            _step = 99; // 끝

        return out;
    }

    @Override
    public long length()
    {
        return -1;
    }

    @Override
    public long progress()
    {
        // TODO Auto-generated method stub
        return 0;
    }
    
}
