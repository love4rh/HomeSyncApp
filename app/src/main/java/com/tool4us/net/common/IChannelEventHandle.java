package com.tool4us.net.common;

import io.netty.channel.ChannelHandlerContext;



public interface IChannelEventHandle
{
    /**
     * Channel이 오픈될 때 발생하는 이벤트 핸들러
     * @param ctx
     */
    public void onChannelActive(ChannelHandlerContext ctx);
    
	/**
	 * Channel이 닫힐 때 발생하는 이벤트 핸들러
	 * @param ctx
	 */
	public void onChannelInactive(ChannelHandlerContext ctx);

	/**
	 * Channel 처리 중 발생하는 Exception을 처리하기 위한 핸들러
	 * @param ctx
	 * @param cause
	 */
	public void onExceptionCaught(ChannelHandlerContext ctx, Throwable cause);
	
	/**
	 * 새로운 Protocol을 받았을 때 발생하는 이벤트 핸들러
	 * @param msg
	 */
	public void onRecvProtocol(Protocol msg);
}
