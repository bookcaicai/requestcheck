package com.book.requestcheck.exception;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

/**
 * @author chenbin
 *
 */
public class FilterExceptionHelper {
	// private static final ILog LOGGER =
	// LogFactory.getLog(FilterExceptionHelper.class);

	/**
	 * 处理BizException错误
	 * 
	 * @param bz
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	public static void handleBizException(BizException bz, HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		response.setStatus( bz.getCode() );
		sendErrorMessage( bz.getMessage(), request, response );
	}

	/**
	 * 处理BizException错误
	 * 
	 * @param bz
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	public static void handleBizException(BizException bz, int httpStatus, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.setStatus(httpStatus);
		sendErrorMessage( bz.getMessage(), request, response );
	}

	/**
	 * @param response
	 * @param uri
	 * @param msg
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	private static void sendErrorMessage(String msg, HttpServletRequest request, HttpServletResponse response)
			throws IOException, UnsupportedEncodingException {
		if ( StringUtils.isNotBlank( msg ) ) {
			OutputStream out = response.getOutputStream();
			out.write( msg.getBytes( "UTF-8" ) );
		}
	}

}
