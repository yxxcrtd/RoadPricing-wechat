package com.igoosd.service.impl;

import com.igoosd.domain.User;
import com.igoosd.repository.UserRepository;
import com.igoosd.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserRepository userRepository;

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public User findByOpenId(String openId) {
        return userRepository.findByOpenId(openId);
    }

}
