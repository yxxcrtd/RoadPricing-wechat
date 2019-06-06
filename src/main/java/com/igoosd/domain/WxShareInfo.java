package com.igoosd.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 微信分享
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class WxShareInfo {

    private String title;

    private String desc;

    private String link;

    private String imgUrl;

    private String type;

    private String friend;

    private String qqlink;

}
