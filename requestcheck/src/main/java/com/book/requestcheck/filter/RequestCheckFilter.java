package com.book.requestcheck.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.book.requestcheck.exception.BizException;
import com.book.requestcheck.exception.FilterExceptionHelper;
import com.book.requestcheck.exception.RequestPermissionExceptionCodeEnum;
import com.book.requestcheck.util.ParamsSignUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * <p>
 * Project: requestUri CHECK
 * <p>
 * <p>
 * Module: api filter
 * <p>
 * <p>
 * Description: uri 签名检查
 * <p>
 *
 * @author chenbin
 * @date 2017年6月16日 下午4:00:45
 */
public class RequestCheckFilter implements Filter {


	private static final String SIGN_KEY = "sign";

	private static final String TIMESTAMP_KEY = "timestamp";

	private static final String APP_KEY = "app_key";

	private static final String APP_KEY_SECRET_PROPERTY_KEY = "app.key.secret";

	private static final String REQUEST_CHECKURI_KEY = "requestCheckUri";

	private Map<String, String> app_key_secret_property_key_map;

	protected String[] includeUri;

	private LRUCache<String, String> cache;

	private int validityTime;// 单位是分钟

	public void destroy() {

	}

	public void init( FilterConfig config ) throws ServletException {
		// 初始化includeUris
		String checkUriStr = config.getInitParameter( REQUEST_CHECKURI_KEY );
		if ( StringUtils.isNotEmpty( checkUriStr ) ) {
			includeUri = checkUriStr.split( "\\," );
		}
		// 初始化请求有效时间
		String validityTimeStr = config.getInitParameter( "validityTime" );
		validityTime = Integer.parseInt( validityTimeStr );
		// 初始化缓存
		String cacheSize = config.getInitParameter( "cacheSize" );
		if ( StringUtils.isNotEmpty( cacheSize ) ) {
			cache = new LRUCache<String, String>( Integer.valueOf( cacheSize ), ( long )validityTime );
		}

		// 初始化appkey与secret
		try {
			String appSecret = config.getInitParameter( APP_KEY_SECRET_PROPERTY_KEY );
			if ( StringUtils.isNotBlank( appSecret ) ) {
				app_key_secret_property_key_map = new HashMap<String, String>();
				String[] values = appSecret.split( ";" );
				for ( String value : values ) {
					String[] app_secret = value.split( "-" );
					app_key_secret_property_key_map.put( app_secret[ 0 ], app_secret[ 1 ] );
				}
			} else {
				// LOGGER.warn( "没有配置" + APP_KEY_SECRET_PROPERTY_KEY );
			}
		} catch( Exception e ) {
			// LOGGER.error( "获取appkey和secret失败:" + e.getMessage(), e );
		}
	}

	public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain )
			throws IOException,
			ServletException {
		doFilter( ( HttpServletRequest )request, ( HttpServletResponse )response, chain );
	}

	private void doFilter( HttpServletRequest request, HttpServletResponse response, FilterChain chain )
			throws IOException, ServletException {
		try {
			this.handleCheck( request );
			chain.doFilter( request, response );
		} catch( BizException bz ) {
			FilterExceptionHelper.handleBizException( bz, request, response );
		}

	}

	/**
	 * 检查uri是否需要做check<br/>
	 * 检查的顺序是先看请求是否已期，然后是签名是否正确，最后检查请求是否已发送过<br/>
	 * 添加请求是否已过期是为了保护防刷逻辑不会把缓存使用过量，导致防刷效果下降。<br/>
	 * 比如一个接口请求很大如果不加过期会导致防刷的缓存迅速达到容量的上限，导致防刷的时间变短.
	 * 
	 * @param request
	 */
	private void handleCheck( HttpServletRequest request ) {
		if ( isNeedCheckUri( request ) ) {
			String sign = request.getParameter( SIGN_KEY );
			String timeStamp = request.getParameter( TIMESTAMP_KEY );
			checkNull( sign, SIGN_KEY );
			checkNull( timeStamp, TIMESTAMP_KEY );
			if ( this.isExpire( Long.parseLong( timeStamp ) ) ) {// 请求已过期

				throw new BizException( RequestPermissionExceptionCodeEnum.REQUEST_EXPIRED_CODE.getCode(),
						RequestPermissionExceptionCodeEnum.REQUEST_EXPIRED_CODE.getMsg() );
			} else if ( this.checkSign( request, sign ) ) {
				// 先检查签名，再判断是否有使用sign，不能先写入cache，因为可能会导致使用了一个错误的sign(比如被人使用错误的sign来请求接口)
				// ，而导致正常的请求不能进入
				if ( cache.putIfAbsent( sign, "" ) != null ) {
					throw new BizException( RequestPermissionExceptionCodeEnum.REQUEST_INVALID_CORE.getCode(),
							RequestPermissionExceptionCodeEnum.REQUEST_INVALID_CORE.getMsg() );
				}
			} else { // 签名失效
				throw new BizException( RequestPermissionExceptionCodeEnum.REQUEST_SIGN_ERROR_CODE.getCode(),
						RequestPermissionExceptionCodeEnum.REQUEST_SIGN_ERROR_CODE.getMsg() );
			}
		}
	}

	private void checkNull( Object obj, String paramName ) {
		if ( obj == null ) {
			throw new BizException(
					RequestPermissionExceptionCodeEnum.REQUEST_PARAM_ERROR_CODE.getCode(),
					String.format( "%s param[%s]", RequestPermissionExceptionCodeEnum.REQUEST_PARAM_ERROR_CODE.getMsg(), paramName ) );
		}
	}

	/**
	 * 判断时间是否已过期
	 * 
	 * @param timeStamp
	 * @return
	 */
	private boolean isExpire( Long timeStamp ) {
		long now = System.currentTimeMillis() / 1000;
		long differ = now - timeStamp;
		if ( validityTime * 60 < differ ) {
			return true;
		}
		return false;
	}

	private boolean checkSign( HttpServletRequest request, String sign ) {
		if ( StringUtils.isEmpty( sign ) ) {
			return false;
		}
		Map<String, String[]> param = request.getParameterMap();
		ArrayList<String> list = new ArrayList<String>( param.keySet() );
		Collections.sort( list );
		List<String> values = new ArrayList<String>();
		values.add( request.getRequestURI() );
		String appKey = null;
		for ( String key : list ) {
			if ( key.equals( SIGN_KEY ) ) {
				continue;
			}
			if ( APP_KEY.equals( key ) ) {
				appKey = param.get( key )[ 0 ];
				continue;
			}
			values.add( StringUtils.join( param.get( key ) ) );
		}
		String webSign = "";
		if ( StringUtils.isNotEmpty( appKey ) ) { // 第三方
			String secret = app_key_secret_property_key_map.get( appKey );
			webSign = ParamsSignUtil.signWithSecretWithList( appKey, secret, values );
		} else {// use default
			webSign = ParamsSignUtil.signWithList( values );
		}
		return sign.equalsIgnoreCase( webSign );
	}

	private boolean isNeedCheckUri( HttpServletRequest request ) {
		/**
		 * 解决uri里包含空格导致没有进checkFilter的问题。 <br/>
		 * 因为使用startsWith方式check,所以去除空格不影响@PathVariable
		 **/
		String requestUri = StringUtils.deleteWhitespace( request.getRequestURI() );
		boolean flag = false;
		if ( includeUri != null && cache != null ) {
			for ( String checkUri : includeUri ) {
				flag = requestUri.startsWith( checkUri );
				if ( flag ) {
					break;
				}
			}
		}
		return flag;
	}

	/**
	 * 此LRUCache使用了guava的Cahe来实现.<br/>
	 * 虽然guava cache 的asMap方法，操作ConcurrentMap可能会有数据不一致的问题,但是在这个应用里是可以接受的。
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
	 * @date 2017年6月16日 下午8:22:36
	 */
	private class LRUCache<K, V> {

		private Cache<K, V> cache;

		private ConcurrentMap<K, V> cMap;

		public LRUCache( int maxSize, long expireTime ) {
			cache = CacheBuilder.newBuilder().maximumSize( maxSize ).expireAfterWrite( expireTime, TimeUnit.MINUTES ).build();

			cMap = cache.asMap();
		}


		/**
		 * 如果不存在则返回null,存在则返回之前的值
		 * 
		 * @Title: put
		 * @author: chenbin
		 * @date: 2017-5-22 下午8:25:35
		 * @return: V
		 */
		public V putIfAbsent( K k, V v ) {
			return cMap.putIfAbsent( k, v );
		}

		@Override
		public String toString() {
			return "LRUCache [cMap=" + cMap + "]";
		}

	}

}
