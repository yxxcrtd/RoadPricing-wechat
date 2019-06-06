package com.igoosd.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.igoosd.util.aes.SHA1;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 微信工具类
 */
@Service
public class WXUtil {

	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(WXUtil.class);

	/** Redis缓存 openId 用户信息的时间 */
	public static Long redisCacheUserInfoExpires = 300L;

	@Autowired
	private RedisUtil redisUtil;
	
	private Object oTicket_lock = new Object();

	/** 根据授权返回的code，获取用户的 accessToken 与 openId，并且保存到session中，进行记录 */
	public void saveAccessTokenAndOpenIdByCode(String code, String scope, String appId, String appSecret) {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		Map<String, String> resMap = getWebAccessToken(code, appId, appSecret);
		if (null != resMap && null != resMap.get("access_token")) {
			request.getSession().setAttribute("accessToken" + scope, resMap.get("access_token"));
			request.getSession().setAttribute("openId" + scope, resMap.get("openid"));
		}
	}

	/** 如果当前页面需要获取用户的基本信息，那就需要调用这个方法获取网页的accessToken，从session中获取当前访问用户的信息 */
	public Map<String, String> verificationAccessToken(String scope, String appId) {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
		Object accessToken = request.getSession().getAttribute("accessToken" + scope);
		Object openId = request.getSession().getAttribute("openId" + scope);
		if (null == accessToken || null == openId) {
			// 没有获取授权，需要跳转到专门的页面进行跳转
			String authUrl = createdAuthUrl(getNowUrl(true), scope, scope, appId);
			try {
				response.sendRedirect(authUrl);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		Map<String, String> resMap = new TreeMap<>();
		resMap.put("accessToken", accessToken.toString());
		resMap.put("openId", openId.toString());
		return resMap;
	}

	// 获取用户当前访问网址链接
	public String getNowUrl(Boolean clear) {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		String url = request.getRequestURL().toString();
		if (request.getQueryString() != null && !Objects.equals(request.getQueryString(), "")) {
			url += "?" + request.getQueryString();
		}
		if (clear) {
			// 删除链接中的 code 与 state 参数
			String codeRegex = "[?&]{1}code=[0-9a-zA-Z]{32}";
			String stateRegex = "[?&]{1}state=[a-zA-Z_]{11,15}";
			url = url.replaceAll(codeRegex, "");
			url = url.replaceAll(stateRegex, "");
		}
		return url;
	}

	/**
	 * 创建授权的跳转链接
	 * @param redirectUrl
	 * @param scope
	 * @param state
	 * @return
	 */
	public String createdAuthUrl(String redirectUrl, String scope, String state, String appId) {
		try {
			return String.format("https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s#wechat_redirect", appId, URLEncoder.encode(redirectUrl, "UTF-8"), scope, state);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 获取accessToken 凭证
	 * @return
	 * @throws Exception
	 */
	public String getAccessToken(String appId, String appSecret) {
		Object accessTokenObj = redisUtil.get("wx_access_token");
		if (accessTokenObj != null) {
			LOGGER.info("获取accessToken，redis直接返回 ：{}", accessTokenObj.toString());
			return accessTokenObj.toString();
		}
		String accessToken = null;
		synchronized ( oTicket_lock ) {
			accessTokenObj = redisUtil.get("wx_access_token");
			if (accessTokenObj != null) {
				LOGGER.info("获取accessToken，redis直接返回 ：{}", accessTokenObj.toString());
				return accessTokenObj.toString();
			}
			String url = String.format("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s", appId, appSecret);
			LOGGER.info("获取access_token的地址：{}", url);
			String httpRes = HttpHelper.doGet(url);
			LOGGER.info("获取access_token的响应：{}", httpRes);
			Map<String, String> resMap = WXMessageUtil.parseJson(httpRes);
			LOGGER.info("json解析后返回的map值：{}", resMap);
			if (null == resMap) {
				throw new RuntimeException("获取access_token错误");
			}
			if ("40164".equals(resMap.get("errcode"))) {
				throw new RuntimeException("当前IP地址不在开发者的IP白名单之列：" + resMap.get("errmsg"));
			}
			accessToken = resMap.get("access_token");
			Long expires = Long.valueOf(resMap.get("expires_in")); // 凭证有效时间，单位：秒
			redisUtil.set("wx_access_token", accessToken, expires - 200);
		}
		return accessToken;
	}

	/**
	 * 网页授权的accessToken获取
	 * @param code
	 * @return
	 * @throws Exception
	 */
	public Map<String, String> getWebAccessToken(String code, String appId, String appSecret) {
		String url = String.format("https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code", appId, appSecret, code);
		String httpRes = HttpHelper.doGet(url);
		LOGGER.info("根据code获取accessToken返回值：{}", httpRes);
		return WXMessageUtil.parseJson(httpRes);
	}

	/**
	 * 获取用户信息
	 * @param openId
	 * @return
	 */
	public Map<String, String> getUserInfoByOpenId(String openId, String appId, String appSecret) throws Exception {
		String redisKey = "wx-" + openId;
		// redis进行缓存
		Object httpRes = redisUtil.get(redisKey);
		if (httpRes == null) {
			String url = String.format("https://api.weixin.qq.com/cgi-bin/user/info?access_token=%s&openid=%s&lang=zh_CN", getAccessToken(appId, appSecret), openId);
			httpRes = HttpHelper.doGet(url);
			try {
				// 验证返回值是否正确，错误的话会抛出异常，正确的话，加到redis cache中
				WXMessageUtil.verifiedResults(httpRes.toString());
				redisUtil.set(redisKey, JSON.toJSONString(httpRes.toString(), SerializerFeature.BrowserCompatible), redisCacheUserInfoExpires);
			} catch (Exception e) {
				LOGGER.error("获取微信用户信息错误 {}", e);
				redisUtil.del("wx_access_token");
				return null;
			}
		} else {
			httpRes = JSON.parseObject(httpRes.toString(), String.class);
		}
		LOGGER.info("根据openId获取userInfo返回值：{}", httpRes.toString());
		return WXMessageUtil.parseJson(httpRes.toString());
	}

	/**
	 * 获取web用户信息
	 * @param accessToken
	 * @param openId
	 * @return
	 */
	public Map<String, String> getWebUserInfoByOpenId(String accessToken, String openId, String scope, String appId) {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		String redisKey = "web-" + openId;
		// Redis进行缓存
		Object httpRes = redisUtil.get(redisKey);
		if (httpRes == null) {
			String url = String.format("https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s&lang=zh_CN", accessToken, openId);
			httpRes = HttpHelper.doGet(url);
			try {
				// 验证返回值是否正确，错误的话会抛出异常，正确的话，加到redis cache中
				WXMessageUtil.verifiedResults(httpRes.toString());
				redisUtil.set(redisKey, JSON.toJSONString(httpRes.toString(), SerializerFeature.BrowserCompatible), redisCacheUserInfoExpires);
			} catch (Exception e) {
				LOGGER.error("获取微信web用户信息错误 {}", e);
				// accessToken 失效了，重新获取 TODO
				request.getSession().removeAttribute("accessToken" + scope);
				verificationAccessToken(scope, appId);
				return null;
			}
		} else {
			httpRes = JSON.parseObject(httpRes.toString(), String.class);
		}
		LOGGER.info("根据openId获取userInfo返回值：{}", httpRes.toString());
		Map<String, String> ret = WXMessageUtil.parseJson(httpRes.toString());
		if (ret == null || ret.get("nickname") == null) {
			redisUtil.del(redisKey);
		}
		return ret;
	}

	/**
	 * 获取JSAPI TICKET
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> getJsApiTicket(String reqUrl, String appId, String appSecret) {
		Object jsApiTicket = redisUtil.get("wx_js_api_ticket");
		if (null == jsApiTicket) {
			// 重新获取ticket
			String apiUrl = String.format("https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=%s&type=jsapi", getAccessToken(appId, appSecret));
			String httpRes = HttpHelper.doGet(apiUrl);
			LOGGER.info("获取JSAPI Ticket http返回信息：{}", httpRes);
			Map<String, String> resMap = WXMessageUtil.parseJson(httpRes);
			if (null == resMap) {
				try {
					throw new Exception("获取jsApiTicket失败");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			jsApiTicket = resMap.get("ticket");
			LOGGER.info("resMap.get('expires_in')：" + resMap.get("expires_in"));
			Long expires = Long.valueOf(resMap.get("expires_in"));
			redisUtil.set("wx_js_api_ticket", jsApiTicket.toString(), expires - 200);
		}
        String nonceStr = UUID.randomUUID().toString().replaceAll("-", "");
		String timestamp = Long.toString(System.currentTimeMillis() / 1000);
		//String reqUrl = request.getRequestURL().toString();
		if (reqUrl.contains("#")) {
			reqUrl = reqUrl.substring(0, reqUrl.indexOf("#"));
		}
		String sha1Str = String.format("jsapi_ticket=%s&noncestr=%s&timestamp=%s&url=%s", jsApiTicket.toString(), nonceStr, timestamp, reqUrl);
		LOGGER.info("获取到的 JSSDK 签名字符串: {}", sha1Str);
		String signature = null;
		try {
			signature = SHA1.getSHA1(sha1Str);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		LOGGER.info("signature: {}", signature);
		Map<String, Object> ticketInfo = new TreeMap<>();
		ticketInfo.put("appId", appId);
		ticketInfo.put("timestamp", timestamp);
		ticketInfo.put("nonceStr", nonceStr);
		ticketInfo.put("signature", signature);
		LOGGER.info("JSTicket 返回的信息 ：{}", ticketInfo);
		return ticketInfo;
	}

	/**
	 * 发送模板消息
	 * 
	 * @param status - 笔记状态
	 * @param createTime - 笔记创建时间
	 * @param message - 发送的消息主体
	 * @param toOpenId - 给谁发
	 * @param type - 模板类型
	 * @param jumpUrl - 点击后的地址
	 * @return 成功 - true，失败 - false
	 */
	public void sendTemplateMsg(String status, String createTime, String message, String toOpenId, String type, String jumpUrl, String appId, String appSecret) throws Exception {
		Map<String, TemplateData> map = new HashMap<>();
		TemplateData first = new TemplateData();
		first.setColor("#091960");
		first.setValue("个人消息通知");
		map.put("first", first);

		TemplateData name = new TemplateData();
		name.setColor("#091960");
		name.setValue("财猫投资笔记");
		map.put("HandleType", name);

		TemplateData data = new TemplateData();
		data.setColor("#091960");
		data.setValue(status);
		map.put("Status", data);

		TemplateData date = new TemplateData();
		date.setColor("#091960");
		date.setValue("发布于：" + createTime);
		map.put("RowCreateDate", date);

		TemplateData logType = new TemplateData();
		logType.setColor("#091960");
		logType.setValue(message);
		map.put("LogType", logType);

		String apiUrl = String.format("https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s", getAccessToken(appId, appSecret));
		Map<String, Object> reqMap = new TreeMap<>();
		reqMap.put("touser", toOpenId);
		reqMap.put("template_id", type);
		reqMap.put("url", jumpUrl);
		reqMap.put("data", map);
		String reqJson = WXMessageUtil.mapToJson(reqMap);
		String httpRes = HttpHelper.doPostSSL(apiUrl, reqJson);
		try {
			if (WXMessageUtil.verifiedResults(httpRes)) {
				LOGGER.info("【微信返回：{}，给{}推送消息成功！！】", httpRes, toOpenId);
			}
		} catch (Exception e) {
			LOGGER.info("【微信返回：{}，给{}推送消息失败！！】", httpRes, toOpenId);
		}
	}

	/**
	 * 获取32位随机字符串
	 */
	public static String getNonceStr() {
		Random random = new Random();
		return MD5Util.MD5Encode(String.valueOf(random.nextInt(10000)), "UTF-8");
	}

	/**
	 * 时间戳
	 */
	public static String getTimeStamp() {
		return String.valueOf(System.currentTimeMillis() / 1000);
	}

	/**
	 * sign签名
	 * @param characterEncoding
	 * @param parameters
	 * @return
	 */
	public static String createSign(String characterEncoding, SortedMap<Object, Object> parameters, String payKey) {
		StringBuffer sb = new StringBuffer();
		Set<Map.Entry<Object, Object>> es = parameters.entrySet();
		Iterator<Map.Entry<Object, Object>> it = es.iterator();
		while (it.hasNext()) {
			Map.Entry<Object, Object> entry = it.next();
			String k = (String) entry.getKey();
			Object v = entry.getValue();
			/** 如果参数为key或者sign，则不参与加密签名 */
			if (null != v && !"".equals(v) && !"sign".equals(k) && !"key".equals(k)) {
				sb.append(k + "=" + v + "&");
			}
		}
		/** 支付密钥必须参与加密，放在字符串最后面 */
		sb.append("key=" + payKey);
		/** 记得最后一定要转换为大写 */
		return MD5Util.MD5Encode(sb.toString(), characterEncoding).toUpperCase();
	}

	// SHA1加密，该加密是对wx.config配置中使用到的参数进行SHA1加密，这里不需要key参与加密
	public static String createSign_wx_config(String characterEncoding, SortedMap<Object, Object> parameters) {
		StringBuffer sb = new StringBuffer();
		Set<Map.Entry<Object, Object>> es = parameters.entrySet();
		Iterator<Map.Entry<Object, Object>> it = es.iterator();
		while (it.hasNext()) {
			Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>) it.next();
			String k = (String) entry.getKey();
			Object v = entry.getValue();
			if (null != v && !"".equals(v)) {
				sb.append(k + "=" + v + "&");
			}
		}
		String str = sb.toString();
		String sign = Sha1Util.getSha1(str.substring(0, str.length() - 1));
		return sign;
	}

	/**
	 * 将请求参数转换为xml格式的string
	 * @param parameters
	 * @return
	 */
	public static String getRequestXml(SortedMap<Object, Object> parameters) {
		StringBuffer sb = new StringBuffer();
		sb.append("<xml>");
		Set<Map.Entry<Object, Object>> es = parameters.entrySet();
		Iterator<Map.Entry<Object, Object>> it = es.iterator();
		while (it.hasNext()) {
			Map.Entry<Object, Object> entry = it.next();
			String k = (String) entry.getKey();
			String v = entry.getValue() + "";
			sb.append("<" + k + ">" + v + "</" + k + ">");
		}
		sb.append("</xml>");
		return sb.toString();
	}

	public static String map2xml(Map<String, String> map) {
		String xmlResult = "";
		StringBuffer sb = new StringBuffer();
		sb.append("<xml>");
		for (String key : map.keySet()) {
			String value = "<![CDATA[" + map.get(key) + "]]>";
			sb.append("<" + key + ">" + value + "</" + key + ">");
		}
		sb.append("</xml>");
		xmlResult = sb.toString();
		return xmlResult;
	}

	/**
	 * 获取微信的 prepay_id
	 * @param interfaceWechatPay
	 * @return
	 */
	public static String getPrepayId(String requestXML, String interfaceWechatPay) {
		HttpClient client = new HttpClient();
		PostMethod myPost = new PostMethod(interfaceWechatPay);
		client.getParams().setSoTimeout(300 * 1000);
		String result = null;
		try {
			myPost.setRequestEntity(new StringRequestEntity(requestXML, "text/xml", "utf-8"));
			int statusCode = client.executeMethod(myPost);
			if (statusCode == HttpStatus.SC_OK) {
				BufferedInputStream bis = new BufferedInputStream(myPost.getResponseBodyAsStream());
				byte[] bytes = new byte[1024];
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				int count = 0;
				while ((count = bis.read(bytes)) != -1) {
					bos.write(bytes, 0, count);
				}
				byte[] strByte = bos.toByteArray();
				result = new String(strByte, 0, strByte.length, "utf-8");
				bos.close();
				bis.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		myPost.releaseConnection();
		client.getHttpConnectionManager().closeIdleConnections(0);
		LOGGER.info("请求统一支付接口的返回结果：{}", result);
		return result;
	}

}
