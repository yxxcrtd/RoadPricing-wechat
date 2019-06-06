package com.igoosd.domain;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * Fee Object
 */
@Data
public class Fee {

    private int fee_id;

    /** 支付金额 */
    private double amount;

    /** 泊位号 */
    private int space_number;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date in_time;

    private int data_type;

    private String park_name;

    private int id;

    private String car_no;

}
