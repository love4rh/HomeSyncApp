package com.tool4us.net.exception;


/**
 * Time-Out Exception
 * 
 * @author TurboK
 */
@SuppressWarnings("serial")
public class TimeOutException extends Exception
{
    public TimeOutException(String message)
    {
        super(message);
    }
}
