/**
 * 
 */
package com.book.requestcheck.util;

import java.util.List;

/**
 * <p>
 * Project:
 * <p>
 * <p>
 * Module:
 * <p>
 * <p>
 * Description:
 * <p>
 *
 * @author chenbin
 * @date 2017年6月16日 下午3:20:10
 */
public class ParamsSignUtil {

	private static final String UTF_8 = "UTF-8";

	private static final String APP_KEY = "BOOK";

	private static final String SECRET = "CAICAI";

	private static final String SPLIT_STR_THIRD = "$";

	/**
	 * 对sign进行检查
	 * 
	 * @param sign
	 * @param params
	 * @return
	 */
	public static boolean checkVideoSign( String sign, Object... params ) {
		String decodeToken = sign( params );
		return decodeToken.equalsIgnoreCase( sign );
	}

	/**
	 * 生成原始的sign
	 * 
	 * @param params
	 * @return
	 */
	public static String sign( Object... params ) {
		return signWithSecret( APP_KEY, SECRET, params );
	}

	/**
	 * 生成原始的sign
	 * 
	 * @param params
	 * @return
	 */
	public static String signWithList( List<? extends Object> params ) {
		return signWithSecretWithList( APP_KEY, SECRET, params );
	}

	/**
	 * 接受外部传入appkey和Secret 生成sign
	 * 
	 * @param prefix
	 * @param suffix
	 * @param params
	 * @return
	 */
	public static String signWithSecret( String appKey, String secret, Object... params ) {
		StringBuffer sb = new StringBuffer( appKey );
		int size = params.length;
		for ( int i = 0; i < size; i++ ) {
			Object param = params[ i ];
			sb.append( param.toString() );
			if ( i < size - 1 ) {
				sb.append( SPLIT_STR_THIRD );
			}
		}
		sb.append( secret );
		return MD5Util.MD5Encode( sb.toString(), UTF_8 );
	}

	/**
	 * 接受外部传入appkey和Secret 生成sign
	 * 
	 * @param prefix
	 * @param suffix
	 * @param params
	 * @return
	 */
	public static String signWithSecretWithList( String appKey, String secret, List<? extends Object> params ) {
		StringBuffer sb = new StringBuffer( appKey );
		int size = params.size();
		for ( int i = 0; i < size; i++ ) {
			Object param = params.get( i );
			sb.append( param.toString() );
			if ( i < size - 1 ) {
				sb.append( SPLIT_STR_THIRD );
			}
		}
		sb.append( secret );
		return MD5Util.MD5Encode( sb.toString(), UTF_8 );
	}

}
