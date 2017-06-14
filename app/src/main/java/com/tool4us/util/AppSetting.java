package com.tool4us.util;

import static com.tool4us.util.CommonTool.CT;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;



/**
 * Application을 실행하기 위하여 필요한 설정 값을 관리하기 위한 클래스.
 * 
 * @author TurboK
 */
public class AppSetting
{
    private final String            _charEncoding = "UTF-8";
    
    private String                  _configFile;
    private Map<String, String>     _option;
    
    
    public AppSetting(String configFile)
    {
        _configFile = configFile;
        _option = new TreeMap<String, String>();
    }
    
    public void save()
    {
        BufferedWriter out = null;
        
        try
        {
            out = new BufferedWriter( new OutputStreamWriter(
                    new FileOutputStream(new File(_configFile)), _charEncoding) );
            
            for(Entry<String, String> elem : _option.entrySet())
            {
                out.write(elem.getKey());
                out.write("=");
                out.write(elem.getValue());
                out.write("\n");
            }
        }
        catch(Exception xe)
        {
            xe.printStackTrace();
        }
        finally
        {
            if( out != null )
            {
                try{ out.close(); } catch(Exception xe2) {}
            }
        }
    }
    
    public void load() throws Exception
    {
        _option.clear();
        
        BufferedReader in = null;
        
        try
        {
            in = new BufferedReader( new InputStreamReader(
                    new FileInputStream(new File(_configFile)), _charEncoding) );
            
            String lineText = in.readLine();
            while( lineText != null && !lineText.isEmpty() )
            {
                int sPos = lineText.indexOf('=');
                
                if( sPos != -1 )
                {
                    _option.put(lineText.substring(0,  sPos), lineText.substring(sPos + 1));
                }
                
                lineText = in.readLine();
            }
        }
        catch(Exception xe)
        {
            xe.printStackTrace();
        }
        finally
        {
            if( in != null )
                in.close();
        }
    }
    
    public String setValue(String name, String value)
    {
        return _option.put(name, CT.makeNoWrapped(value));
    }
    
    public String getValue(String name)
    {
        return CT.makeWrapped(_option.get(name));
    }
    
    public String getValue(String name, String defaultValue)
    {
        String value = _option.get(name);
        
        if( value == null || value.isEmpty() )
        {
            setValue(name, defaultValue);
            value = defaultValue;
        }
        else
            value = CT.makeWrapped(value);
        
        return value;
    }
    
    public String removeValue(String name)
    {
        return _option.remove(name);
    }
    
    public void foo() throws Exception
    {
        final String orgFile = "E:\\EclipseData\\ToolSpace\\dialog-100-days.md";
        final String jsonFile = "E:\\EclipseData\\ToolSpace\\dialog-100-days.json";
        
        BufferedReader in = null;
        BufferedWriter out = null;
        
        try
        {
            in = new BufferedReader( new InputStreamReader(
                    new FileInputStream(new File(orgFile)), _charEncoding) );
            
            out = new BufferedWriter( new OutputStreamWriter(
                    new FileOutputStream(new File(jsonFile)), _charEncoding) );
            
            out.write("{");
            out.write("\"header\":{ \"authour\":\"TurboK\", \"version\":\"1.0\", \"date\":\"2017.03.09\", \"title\":\"English\" }");
            
            out.write(",\"contents\":[");
            
            String lineText = in.readLine();
            
            int chapNo = 0, dialogIdx = 0;
            StringBuilder sb = null;
            
            while( lineText != null )
            {
                if( lineText.isEmpty() )
                {
                    lineText = in.readLine();
                    continue;
                }

                int sPos = lineText.indexOf('#');
                
                // 새로운 챕터 시작
                if( sPos == 0 )
                {
                    if( sb != null )
                    {
                        if( chapNo > 1 )
                            out.write(",");
                        
                        sb.append("]}");
                        out.write(sb.toString());
                    }
                    
                    chapNo += 1;
                    dialogIdx = 0;
                    sb = new StringBuilder();
                    
                    sb.append("{\"chapter\":\"").append(lineText.substring(1, 5).trim())
                      .append("\", \"title\":\"").append(lineText.substring(5).trim())
                      .append("\", \"dialog\":[");
                }
                // 대화내용
                else
                {
                    dialogIdx += 1;
                    sPos = lineText.indexOf("|");
                    
                    if( sPos < 0 )
                    {
                        System.out.println("Check");
                    }
                    
                    if( dialogIdx > 1 )
                        sb.append(",");

                    sb.append("{").append("\"index\":").append(dialogIdx)
                      .append(",\"who\":\"").append(lineText.substring(0, 2).trim()).append("\"")
                      .append(",\"english\":\"").append(lineText.substring(2, sPos).trim()).append("\"")
                      .append(",\"korean\":\"").append(lineText.substring(sPos + 1).trim()).append("\"")
                      .append("}")
                      ;
                }
                
                lineText = in.readLine();
            }
            
            out.write("]}");
        }
        catch(Exception xe)
        {
            xe.printStackTrace();
        }
        finally
        {
            if( out != null )
                out.close();
            
            if( in != null )
                in.close();
        }
    }
}
