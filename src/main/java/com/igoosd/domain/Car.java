package com.igoosd.domain;

import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Data
@Entity
@Table(name = "wx_car")
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private long userId;

//    @Length(min = 1, max = 1, message = "车牌号不正确")
    private String number1;

    @Length(min = 6, max = 7, message = "车牌号不正确")
    private String number2;

    // 绑定状态：0-未绑定（默认）；1-已绑定
    private int status;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date create_time;

}
