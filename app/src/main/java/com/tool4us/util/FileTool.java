package com.tool4us.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;



/**
 * File 처리 관련 기능 모음
 * 
 * @author TurboK
 */
public class FileTool
{
    /**
     * path 내 폴더를 순차적으로 생성함.
     * path의 값이 /d1/d2/d3 일 경우 d1, d1 밑에 d2, d2 밑에 d3를 순차적으로 생성함. 
     * @param path          생성할 경로
     * @param includeFile   경로 제일 뒤에 파일 이름이 포함되었는 지 여부. true이면 마지막 것을 만들지 않는다.
     */
    public static boolean makeDir(String path, boolean includeFile)
    {
        if( includeFile )
            path = path.substring(0, Math.max(path.lastIndexOf("\\"), path.lastIndexOf("/")) );
        
        File dir = new File(path);
        
        return dir.exists() || dir.mkdirs();
    }    
    
    /**
     * 파일 이동
     * @param s
     * @param d
     * @throws Exception
     */
    public static void moveFile(File s, File d) throws Exception
    {
        if( !s.renameTo( d ) )
        {
            byte[] buf = new byte[4096];
            
            FileInputStream in = new FileInputStream(s);
            FileOutputStream out = new FileOutputStream(d);
         
            int read = 0;
            while( (read = in.read(buf, 0, buf.length))!=-1 )
            {
                out.write(buf, 0, read);
            }

            in.close();
            out.close();
            s.delete();
        }
    }
    
    public static void copyFile(File s, File d) throws IOException
    {
        // NOTE: targetFile 이 이미 존재하고 있었다면, srcFile size만큼만 overwrite되고, 나머지 부분은 남아있게 된다.
        // 따라서, 사전에 삭제할 필요가 있다.

        if ( d.exists() == true )
            d.delete();
        
        RandomAccessFile inFile = new RandomAccessFile(s, "r");
        RandomAccessFile outFile = new RandomAccessFile(d, "rw");
        
        FileChannel in = inFile.getChannel();
        FileChannel out = outFile.getChannel();
        
        in.transferTo(0, inFile.length(), out);
        
        in.close();
        out.close();
        
        inFile.close();
        outFile.close();
    }
}
