package com.igoosd.util;

import org.apache.commons.lang3.StringUtils;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Date操作工具类
 * 
 * @author yxxcrtd@gmail.com
 */
public class DateUtil extends BaseUtil {

	/** 标准的时间格式 */
	public static final String YYYYMMDDHHMMSS = "yyyy-mm-dd hh:mm:ss";

    /**
     * 显示当前时间
     * @return 时间格式为：yyyy-mm-dd hh:mm:ss
     */
	public static final String getNow() {
//		LocalDateTime ldt = LocalDateTime.now();
//		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(YYYYMMDDHHMMSS);
//		return ldt.format(dtf);

		return String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS", System.currentTimeMillis());
	}

	/**
	 * 获取系统当前时间
	 * 
	 * @return 返回规定格式的数据，如：20140710170735665
	 */
	public static final String getCurrentTime() {
		return String.format("%1$tY%1$tm%1$td%1$tH%1$tM%1$tS%1$tL", System.currentTimeMillis());
	}

	public static final String getStringDate(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat(YYYYMMDDHHMMSS);
		return sdf.format(date);
	}
	
	/**
	 * 获取系统当前时间
	 * 
	 * @return 返回短时间格式，如：2014-07-10
	 */
	public static final Date getCurruentDate() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String dateString = sdf.format(date);
		ParsePosition pp = new ParsePosition(0);
		return sdf.parse(dateString, pp);
	}

	public static final Date getDate(String datetime, String pattern) {
		if (StringUtils.isBlank(datetime)) {
			return null;
		}
		Date date = null;
		try {
			SimpleDateFormat df = new SimpleDateFormat(pattern);
			date = df.parse(datetime);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return date;
	}

	public static final String getRelativeTime(String beginDate, String endDate) {
		SimpleDateFormat sdf = new SimpleDateFormat(YYYYMMDDHHMMSS);
		long between = 0;
		try {
			Date beginTime = sdf.parse(beginDate);
			Date endTime = sdf.parse(endDate);
			between = (endTime.getTime() - beginTime.getTime()); // 得到两者的毫秒数
		} catch (Exception e) {
			e.printStackTrace();
		}
//		long day = between / (24 * 60 * 60 * 1000);
//		long hour = (between / (60 * 60 * 1000) - day * 24);
//		long min = ((between / (60 * 1000)) - day * 24 * 60 - hour * 60);
//		long s = (between / 1000 - day * 24 * 60 * 60 - hour * 60 * 60 - min * 60);
//		long ms = (between - day * 24 * 60 * 60 * 1000 - hour * 60 * 60 * 1000 - min * 60 * 1000 - s * 1000);
//		return day + "天" + hour + "小时" + min + "分" + s + "秒" + ms + "毫秒";
		long hour = (between / (60 * 60 * 1000));
		long min = ((between / (60 * 1000)) - hour * 60);
		return (0 == hour ? "" : hour + "小时") + min + "分钟";
	}

	/**
	 * Main Method Test
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
//		System.out.println(getCurrentTime());
//		System.out.println();
//		System.out.println(getCurruentDate());
		System.out.println(getRelativeTime("2016-11-20 18:22:22", "2016-11-20 20:02:20"));
//		System.out.println(getNow());
//		System.out.println(getCurrentTime());

//		System.out.println(getDate("2016-11-20 18:22:22", "yyyy-MM-dd"));
	}

}
