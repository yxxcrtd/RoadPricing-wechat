package com.igoosd.service;

import com.igoosd.domain.Car;

public interface CarService {

    Car save(Car car);

    Car findByUserId(Long userId);
    
}
