package com.tool4us.net.common;



/**
 * dataOn 서버 요청 시 발생할 수 있는 오류 정의
 * 
 * @author TurboK
 *
 */
public enum ErrorCode
{
      errSuccess(0, "Success")
    , errFail(1, "Operation failed.")
    
    , errApiInsufficientParameter(2001, "Insufficient parameter(s).")
    , errApiInvalidParameter(2002, "Parameter type mismatch or invalid parameter(s).")
    , errApiInvalidQuery(2003, "Invalid query id.")
    , errApiMismatchSize(2004, "DataGroup's size and Index's size is different.")
    , errApiInsufficientPrivilege(2005, "Insufficient privilege")
    , errApiInsufficientDisk(2006, "Insufficient free disk space")

    
    , errInvalidUser(9001, "Invalid user.")
    , errInvalidPassword(9002, "Invalid password.")
    , errUserNullData(9003, "There is null Data")
    , errUserDuplicatedName1(9004, "Try to add duplicated id #1")
    , errUserDuplicatedName2(9005, "Try to add duplicated id #2")
    , errUserNotExistId1(9006, "Not exist user id #1")
    , errUserNotExistId2(9007, "Not exist user id #2")
    , errUserDeleteFail(9008, "User Delete Fail")
    , errUserDeleteNoResult(9009, "There is no user to delete")
    , errUserInvalidPw(9010, "Should be 4 characters at least")
    , errUserPermissionError(9011, "Permission error")
    , errUserAdminAuthoriyChangeError(9012, "admin account should have admin authority.")
    
    , errFileNotExist(9101, "File not exist.")

    , errApiUnknown(9992, "Unknown API error")
    , errNetworkUnknown(9993, "Unknown networking error")
    , errUnknownProtocol(9994, "Unknown protocol recieved")
    , errTimeOut(9995, "Time out")
    , errSystemFault(9996, "System opration failed")
    , errUnknown(9999, "Unknown error")
    ;
    
      
    private int     _code = -1;
    private String  _message = "";
    
    
    private ErrorCode(int errorCode, String message)
    {
        _code = errorCode;
        _message = message;
    }
    
    public int code()
    {
        return _code;
    }
    
    public String message()
    {
        return _message;
    }
    
    @Override
    public String toString()
    {
    	return _message;
    }
    
    static
    public ErrorCode fromInt(int code)
    {
        for(ErrorCode errCode : ErrorCode.values())
        {
            if( errCode.code() == code )
                return errCode;
        }
        
        return errUnknown;
    }
}
