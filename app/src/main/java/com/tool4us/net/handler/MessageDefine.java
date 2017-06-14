package com.tool4us.net.handler;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;



/**
 * MessageHandler를 정의하기 위한 Annotation
 * @author TurboK
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageDefine
{
    /**
     * 프로토콜 ID
     * @return
     */
    int id();
    
    /**
     * 프로토콜이 서버용인지 여부를 나타냄.
     */
    // boolean server() default true;
    
    /**
     * 프로토콜이 클라이언트용인지 여부를 나타냄.
     */
    // boolean client() default false;
    
    /**
     * 프로토콜 사용 권한 값.
     */
    int level() default 0;
}
