package com.igoosd.controller;

import com.igoosd.domain.Car;
import com.igoosd.domain.Fee;
import com.igoosd.domain.Trade;
import com.igoosd.domain.User;
import com.igoosd.service.CarService;
import com.igoosd.service.UserService;
import com.igoosd.util.WXUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Map;

/**
 * Base Controller
 */
public class BaseController {

	/** LOGGER */
	protected static final Logger LOGGER = LoggerFactory.getLogger(BaseController.class);

	/** 金额保留2位 */
	protected DecimalFormat df = new DecimalFormat("#.00");

	protected @Value("${wx.domain}") String domain;
	protected @Value("${wx.app-id}") String appId;
	protected @Value("${wx.app-secret}") String appSecret;
	protected @Value("${wx.mch-id}") String mchId;
	protected @Value("${wx.pay-key}") String payKey;
	protected @Value("${wx.url.pay}") String payUrl;
	protected @Value("${wx.message-template}") String messageTemplate;
	protected @Value("${wx.interface.company-id}") String companyId;
	protected @Value("${wx.interface.return-code}") String returnCode;
	protected @Value("${wx.interface.find-by-car-no}") String interfaceFindByCarNo;
	protected @Value("${wx.interface.find-arrear-list}") String interfaceFindArrearList;
	protected @Value("${wx.interface.find-arrear-detail}") String interfaceFindArrearDetail;
	protected @Value("${wx.url.callback}") String wxUrlCallback;

	/** web授权作用域 */
	public static String scopeBase = "snsapi_base";
	public static String scopeUser = "snsapi_userinfo";

	/** 更新数据库中微信用户的基本信息时间（单位：小时）*/
	private static Long updateWeixinUserInfoHours = 24L;

	/** Redis缓存 openId 用户信息的时间 */
	public static Long redisCacheUserInfoExpires = 300L;

	protected User user;
	protected Car car;
	protected Fee fee;
	protected Trade trade;

	@Autowired
	protected WXUtil wxUtil;

	@Autowired
	protected UserService userService;

	@Autowired
	protected CarService carService;

	/**
	 * 获取微信用户授权信息
	 *
	 * @param mav
	 * @return
	 * @throws Exception
	 */
	protected User baseAuthInfo(ModelAndView mav) {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		mav.addObject("d", System.currentTimeMillis());

		// 根据网页传递 范围，定义范围
		String scope = request.getParameter("state");
		if (scope == null)
			scope = scopeUser;
		if (!(scope.equals(scopeBase) || scope.equals(scopeUser))) {
			scope = scopeBase;
		}

		String code = request.getParameter("code");
		if (null != code) {
			wxUtil.saveAccessTokenAndOpenIdByCode(code, scope, appId, appSecret);
		}
		Map<String, String> authInfo = wxUtil.verificationAccessToken(scope, appId);
		// 如果返回null，则跳转到授权页面
		if (null == authInfo) {
			return null;
		}
		mav.addObject("authInfo", authInfo);

		// 获取 JSSDK 需要的配置
		String currentUrl = wxUtil.getNowUrl(false);
		mav.addObject("jsApiTicket", wxUtil.getJsApiTicket(currentUrl, appId, appSecret));
		LOGGER.info("当前URL：{}，jsApiTicket：{}", currentUrl, mav.getModelMap().get("jsApiTicket"));

		// 只有关注之后才能到这里
		mav.addObject("subscribe", 1);
		// 根据Web授权，获取用户信息，如果没有注册，则注册到数据库
		Map<String, String> webUserInfo = wxUtil.getWebUserInfoByOpenId(authInfo.get("accessToken"), authInfo.get("openId"), scope, appId);
		if (null == webUserInfo)
			return null;
		try {
			Map<String, String> wxUserInfo = wxUtil.getUserInfoByOpenId(authInfo.get("openId"), appId, appSecret);
			if (null != wxUserInfo) {
				mav.addObject("subscribe", wxUserInfo.get("subscribe"));
				mav.addObject("nickname", wxUserInfo.get("nickname"));
				mav.addObject("headimgurl", wxUserInfo.get("headimgurl"));
				webUserInfo.put("subscribe", wxUserInfo.get("subscribe"));
			}
		} catch (Exception ignore) {
			//
		}
		user = registerNewUser(webUserInfo, request);
		mav.addObject("user", user);
		return user;
	}

	/**
	 * 根据网页获取的webUserInfo 信息，查找数据库中用户，没有则注册
	 *
	 * @param wxUser
	 */
	protected User registerNewUser(Map<String, String> wxUser, HttpServletRequest request) {
		String openId = wxUser.get("openid");
		if (null == openId) {
			wxUtil.verificationAccessToken(scopeUser, appId);
			return null;
		}
		User user = userService.findByOpenId(openId);
		if (null == user) {
			if (wxUser.get("nickname") == null) {
				this.wxUtil.verificationAccessToken(scopeUser, appId);
				return null;
			}
			User u = new User();
			u.setOpenId(wxUser.get("openid"));
			u.setNickname(wxUser.get("nickname"));
			u.setHeader_img(wxUser.get("headimgurl"));
			u.setSubscribe(wxUser.get("subscribe") == null ? 0 : Integer.valueOf(wxUser.get("subscribe")));
			u.setCreate_time(new Date());
			u.setCreate_ip(request.getRemoteAddr());
			u.setUpdate_time(new Date());
			userService.save(u);
			user = userService.findByOpenId(openId);
		} else {
			updateUserInfo(user, request);
		}
		return user;
	}

	/**
	 * 更新数据库中用户信息（昵称、头像），如果最后更新时间大于24小时的，更新一下
	 */
	protected void updateUserInfo(User user, HttpServletRequest request) {
		try {
			if (null == user) {
				return;
			}
			if (new Date().getTime() - user.getUpdate_time().getTime() >= (updateWeixinUserInfoHours * 60 * 60 * 1000)) {
				Map<String, String> wxMap = wxUtil.getUserInfoByOpenId(user.getOpenId(), appId, appSecret);
				if (null == wxMap) {
					return;
				}
				if (null == wxMap.get("nickname")) {
					return;
				}
				User u = new User();
				u.setId(user.getId());
				u.setOpenId(user.getOpenId());
				u.setNickname(wxMap.get("nickname"));
				u.setHeader_img(wxMap.get("headimgurl"));
				u.setSubscribe(null == wxMap.get("subscribe") ? 0 : Integer.valueOf(wxMap.get("subscribe")));
				u.setCreate_time(user.getCreate_time());
				u.setUpdate_time(new Date());
				u.setCreate_ip(request.getRemoteAddr());
				userService.save(u);
			}
		} catch (Exception e) {
			LOGGER.error("更新数据库中微信用户信息异常 {}", e);
		}
	}

}
