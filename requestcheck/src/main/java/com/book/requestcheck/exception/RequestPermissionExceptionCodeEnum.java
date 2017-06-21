/**
 * 
 */
package com.book.requestcheck.exception;

/**
 * @author chenbin
 *
 */
public enum RequestPermissionExceptionCodeEnum {
	
	REQUEST_INVALID_CORE("请求已失效",4100),
	REQUEST_EXPIRED_CODE("请求已过期", 4101),
	REQUEST_SIGN_ERROR_CODE("签名错误", 4102),
	REQUEST_PARAM_ERROR_CODE("参数错误",4103);
	
	private int code;
	private String msg;
	
	private  RequestPermissionExceptionCodeEnum(String msg,int code) {
		this.code = code;
		this.msg = msg;
	}

	public int getCode() {
		return code;
	}

	public String getMsg() {
		return msg;
	}

}
