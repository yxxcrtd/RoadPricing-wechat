package com.igoosd.controller;

import com.igoosd.util.XMLUtil;
import net.sf.json.JSONArray;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.igoosd.util.FastjsonUtil.jsoupPost;
import static com.igoosd.util.WXUtil.createSign;
import static com.igoosd.util.WXUtil.getPrepayId;
import static com.igoosd.util.WXUtil.getRequestXml;

@RestController
@RequestMapping("wx/arrear")
public class ArrearController extends BaseController {

    /**
     * 显示欠费列表
     */
    @GetMapping("")
    ModelAndView arrear() throws Exception {
        ModelAndView mav = new ModelAndView("arrear/ArrearList");

        // 获取微信的用户信息
        baseAuthInfo(mav);

        if (null == user) {
            return null;
        }
        car = carService.findByUserId(user.getId());
        if (null != car) {
            try {
                HashMap<String, String> map = new HashMap<>();
                map.put("carNumber", car.getNumber1() + car.getNumber2());
                map.put("companyId", companyId);
                mav.addObject("arrearList", jsoupPost(interfaceFindArrearList, map, returnCode, "2"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mav.addObject("active", "arrear");
        return mav;
    }

    @GetMapping("{id:[\\d]+}")
    ModelAndView detail(@PathVariable(value = "id") int id) {
        ModelAndView mav = new ModelAndView("arrear/ArrearDetail");

        // 获取微信的用户信息
        baseAuthInfo(mav);

        try {
            HashMap<String, String> map = new HashMap<>();
            map.put("id", String.valueOf(id));
            mav.addObject("arrearDetail", jsoupPost(interfaceFindArrearDetail, map, returnCode, "1"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        mav.addObject("active", "arrear");
        return mav;
    }

    /**
     * 微信支付
     */
    @PostMapping("pay")
    String pay(HttpServletRequest request, String id, String car_no, String totalPrice, String parkName, String space_no, String in_time, String data_type) throws Exception {
        ModelAndView mav = new ModelAndView();
        SortedMap<Object, Object> params = new TreeMap<>();
        Map<String, Object> m = (Map<String, Object>) mav.getModelMap().get("jsApiTicket");
        String nonceStr = String.valueOf(m.get("nonceStr")); // 随机字符串
        String timeStamp = String.valueOf(m.get("timestamp")); // 时间戳

        NumberFormat nf = NumberFormat.getNumberInstance();
        totalPrice = String.valueOf(Integer.valueOf(String.valueOf(nf.parse(totalPrice))) * 100);

        // 1，组装数据去获取 prepay_id
        params.put("appid", appId);
        params.put("mch_id", mchId);
        params.put("nonce_str", nonceStr);
        params.put("body", "停车缴费：" + parkName); // 支付的标题
        params.put("out_trade_no", timeStamp);
        params.put("total_fee", "1"); // 支付金额 // TODO 测试阶段是：1分钱。正式环境需要替换上面的 totalPrice
        params.put("spbill_create_ip", request.getRemoteAddr());
        params.put("notify_url", wxUrlCallback);
        params.put("trade_type", "JSAPI");
        params.put("openid", user.getOpenId());
        params.put("sign", createSign("UTF-8", params, payKey));
        String requestXML = getRequestXml(params);
        LOGGER.info("生成的requestXML：{}", requestXML);

        // 2，解析微信的返回值
        String result = getPrepayId(requestXML, payUrl);
        try {
            Map<String, String> map = XMLUtil.doXMLParse(result);
            SortedMap<Object, Object> stringMap = new TreeMap<>();
            stringMap.put("appId", appId);
            stringMap.put("timeStamp", timeStamp);
            stringMap.put("nonceStr", nonceStr);
            stringMap.put("package", "prepay_id=" + map.get("prepay_id"));
            stringMap.put("signType", "MD5");
            // stringMap.put("signature", String.valueOf(m.get("signature"))); // 这个不需要 ！！！
            stringMap.put("paySign", createSign("UTF-8", stringMap, payKey));
            stringMap.put("notify_url", wxUrlCallback);
            String json = JSONArray.fromObject(stringMap).toString();
            LOGGER.info("返回给页面：{}", json);
            return json;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 微信回调
     */
    @RequestMapping(value = "callback3")
    public String callback3(HttpServletRequest request, HttpServletResponse response) throws Exception {
        LOGGER.info("微信回调了！！！" + new Date());
        BufferedReader reader = request.getReader();
        String line = "";
        String xmlString = null;
        StringBuffer inputString = new StringBuffer();
        while (null != (line = reader.readLine())) {
            inputString.append(line);
        }
        xmlString = inputString.toString();
        request.getReader().close();
        LOGGER.info("接收微信回调的数据：" + xmlString);

        Map<String, String> map = XMLUtil.doXMLParse(xmlString);
        String openId = String.valueOf(map.get("openid"));
        user = userService.findByOpenId(openId);

//        trade = new Trade();
//        trade.setUser_id(user.getId());
//        trade.setPay_amount(Double.parseDouble(df.format(map.get("cash_fee"))));
//        trade.setPay_out_trade_no(String.valueOf(map.get("out_trade_no")));
//        trade.setPay_transaction_id(String.valueOf(map.get("transaction_id")));
//        trade.setPay_create_time(new Date());
//        tradeService.save(trade);

        // 支付成功发送消息
        // wxUtil.sendTemplateMsg();

        // 处理业务

        return "success";
    }

}
