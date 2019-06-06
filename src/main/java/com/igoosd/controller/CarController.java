package com.igoosd.controller;

import com.igoosd.domain.Car;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Date;

/**
 * 车牌绑定
 */
@RestController
@RequestMapping("wx/car")
public class CarController extends BaseController {

    /**
     * 绑车牌页面
     */
    @GetMapping("")
    ModelAndView index(HttpServletRequest req) {
        LOGGER.info("请求地址：" + req.getRequestURL());
        ModelAndView mav = new ModelAndView();

        // 获取微信的用户信息
        baseAuthInfo(mav);

        // 如果没有获取到用户授权信息，直接返回
        if (null == user) {
            return null;
        }

        Car car = carService.findByUserId(user.getId());
        LOGGER.info("用户【{}】的绑卡信息：{}", user.getNickname(), car);

        // 1，新用户
        if (null == car) {
            car = new Car();
            mav.setViewName("car/CarEdit");
        }
        // 2，已经解绑的用户，显示再次绑定页面
        else if (0 == car.getStatus()) {
            mav.setViewName("car/CarEdit");
        }
        // 3，已绑定的用户，显示解绑页面
        else if (1 == car.getStatus()) {
            mav.setViewName("car/CarSuccess");
        }
        mav.addObject("obj", car);
        mav.addObject("active", "car");
        return mav;
    }

    /**
     * 保存用户车牌号
     */
    @PostMapping("bind")
    ModelAndView bind(@ModelAttribute("obj") @Valid Car car, BindingResult result) {
        if (result.hasErrors()) {
            ModelAndView mav = new ModelAndView("car/CarEdit");

            // 获取微信的用户信息
            baseAuthInfo(mav);

            mav.addObject("car", car);
            mav.addObject("active", "car");
            mav.addObject("d", System.currentTimeMillis());
            return mav;
        }

        ModelAndView mav = new ModelAndView("car/CarSuccess");

        // 获取微信的用户信息
        baseAuthInfo(mav);

        if (null == user) {
            return null;
        }
        if (0 == car.getId()) {
            car.setUserId(user.getId());
        }
        car.setStatus(1);
        car.setNumber1("".endsWith(car.getNumber1()) ? "皖" : car.getNumber1());
        car.setNumber2(car.getNumber2().toUpperCase());
        car.setCreate_time(new Date());
        carService.save(car);
        mav.addObject("obj", car);
        mav.addObject("active", "car");
        return mav;
    }

    /**
     * 解绑车牌号
     */
    @PostMapping("unbind")
    ModelAndView unbind(@ModelAttribute("obj") @Valid Car car) {
        ModelAndView mav = new ModelAndView("car/CarEdit");
        mav.addObject("active", "car");
        mav.addObject("d", System.currentTimeMillis());
        mav.addObject("car", car);

        // 获取微信的用户信息
        baseAuthInfo(mav);

        car.setStatus(0);
        carService.save(car);
        return mav;
    }

}
