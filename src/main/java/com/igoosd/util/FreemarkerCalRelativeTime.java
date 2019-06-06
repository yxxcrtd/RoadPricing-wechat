package com.igoosd.util;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModelException;

import java.util.List;

import static com.igoosd.util.DateUtil.getNow;

/**
 * 在 Freemarker 计算两个时间的间隔
 */
public class FreemarkerCalRelativeTime implements TemplateMethodModel {

	@Override
	public Object exec(List args) throws TemplateModelException {
		String s = "";
		if (null != args && 0 < args.size()) {
			String beginTime = (String) args.get(0);
			String endTime = "".equals(String.valueOf(args.get(1))) ? getNow() : String.valueOf(args.get(1));
			if (!"".equals(beginTime) && !"".equals(endTime)) {
				s = DateUtil.getRelativeTime(beginTime, endTime);
			}
		}
		return s;
	}

}
