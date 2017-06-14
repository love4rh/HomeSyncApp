package com.tool4us.homesync.file;



public class CompResult
{
    public static final int     I_HAVE = 1;     // 나한테만 있음
    public static final int     YOU_HAVE = 2;   // 쟤한테만 있음
    public static final int     BOTH_HAVE = 3;  // 속성이 같은 것을 가지고 있음
    public static final int     DIFF_HAVE = 4;  // 속성이 다른 것을 가지고 있음
    
    private Object      _relatedData = null;
    private int         _compResult;
    
    public CompResult(Object data, int comp)
    {
        _relatedData = data;
        _compResult = comp;
    }
    
    public int getCompResult()
    {
        return _compResult;
    }
    
    public Object getRelatedData()
    {
        return _relatedData;
    }
}
