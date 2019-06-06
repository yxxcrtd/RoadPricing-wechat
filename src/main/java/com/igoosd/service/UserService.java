package com.igoosd.service;

import com.igoosd.domain.User;

public interface UserService {

    User save(User user);

    User findByOpenId(String openId);

}
