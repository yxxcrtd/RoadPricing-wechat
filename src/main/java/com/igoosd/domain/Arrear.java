package com.igoosd.domain;

import lombok.Data;

/**
 * Arrear Object
 */
@Data
public class Arrear {

    /**
     * 欠费ID
     */
    private int id;

    /**
     * ⽋费⾦额
     */
    private double arrearsAmount;

    /**
     * 停车场名称
     */
    private String parkingName;

    /**
     * 驶⼊时间
     */
    private String enterTime;

    /**
     * 驶出时间
     */
    private String exitTime;

}
