package com.igoosd.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;

import static com.igoosd.util.FastjsonUtil.jsoupPost;

/**
 * 停车缴费
 */
@RestController
@RequestMapping("wx/fee")
public class FeeController extends BaseController {

    /**
     * 显示当前停车信息
     */
    @GetMapping("")
    ModelAndView fee() {
        ModelAndView mav = new ModelAndView("fee/Fee");

        // 获取微信的用户信息
        baseAuthInfo(mav);

        if (null == user) {
            return null;
        }
        car = carService.findByUserId(user.getId());
        if (null != car) {
            int bindStatus = car.getStatus();
            mav.addObject("bindStatus", bindStatus);
            if (1 == bindStatus) {
                try {
                    HashMap<String, String> map = new HashMap<>();
                    map.put("carNumber", car.getNumber1() + car.getNumber2());
                    map.put("companyId", companyId);
                    mav.addObject("map", jsoupPost(interfaceFindByCarNo, map, returnCode, "1"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        mav.addObject("active", "fee");
        return mav;
    }

}
