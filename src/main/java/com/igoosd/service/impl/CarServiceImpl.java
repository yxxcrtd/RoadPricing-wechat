package com.igoosd.service.impl;

import com.igoosd.domain.Car;
import com.igoosd.repository.CarRepository;
import com.igoosd.service.CarService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class CarServiceImpl implements CarService {

    @Resource
    private CarRepository carRepository;

    @Override
    public Car save(Car car) {
        return carRepository.save(car);
    }

    @Override
    public Car findByUserId(Long userId) {
        return carRepository.findByUserId(userId);
    }

}
