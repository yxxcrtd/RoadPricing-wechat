package com.igoosd.util;

import java.util.HashMap;
import java.util.Map;

public class StringUtil {

	/**
	 * 过滤微信中的特殊符号
	 *
	 * @param s
	 * @return
	 */
	public static String filterWeiXinEmojiChar(String s) {
		if (null == s || "".equals(s)) {
			return "";
		}
		return s.replaceAll("[^\\u0000-\\uFFFF]", "").trim();
	}

	/**
	 * 将 Map 转换成字符串
	 * @param params
	 * @return
	 */
	public static String getParamesStr(HashMap<String, String> params) {
		String values = "";
		if (params != null && params.size() > 0) {
			for (Map.Entry<String, String> entry : params.entrySet()) {
				values = values + entry.getKey() + "=" + entry.getValue() + "&";
			}
			values = values.substring(0, values.length() - 1);
		}
		return values;
	}

}
