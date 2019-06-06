package com.igoosd.domain;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * Pay Object
 */
@Data
public class Trade {

    private long pay_id;

    /**
     * 用户ID
     */
    private long user_id;

    /**
     * 支付金额
     */
    private double pay_amount;

    /**
     * 交易号
     */
    private String pay_out_trade_no;

    /**
     * 交易事务号
     */
    private String pay_transaction_id;

    /**
     * 交易时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date pay_create_time;

}
