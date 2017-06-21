/**
 * 
 */
package com.book.requestcheck.exception;


/**
 * <p>Project:			<p>
 * <p>Module:			<p>
 * <p>Description:		<p>
 *
 * @author chenbin
 * @date 2017年6月21日 下午10:48:30
 */
public class BizException extends RuntimeException {

	/**  */
	private static final long serialVersionUID = -298311232780808293L;

	private int code = 400;

	private String message;

	public BizException() {
		super();
	}

	public BizException( int code, String message ) {
		this.code = code;
		this.message = message;
	}

	public BizException( int code ) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public void setCode( int code ) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage( String message ) {
		this.message = message;
	}

}
