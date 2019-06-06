
-- 1，创建登录的用户角色（包含密码）
DROP USER IF EXISTS road_pricing;
CREATE ROLE road_pricing WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE INHERIT NOREPLICATION CONNECTION LIMIT -1 PASSWORD 'road_pricing';

-- 2，远程终端执行：
创建目录：mkdir /home/data/road_pricing
目录授权：chown postgres /home/data/road_pricing

-- 3，创建表空间
DROP TABLESPACE IF EXISTS road_pricing;
CREATE TABLESPACE road_pricing OWNER road_pricing LOCATION '/home/data/road_pricing';

-- 4，创建数据库
DROP DATABASE IF EXISTS road_pricing;
CREATE DATABASE road_pricing WITH OWNER = road_pricing ENCODING = 'UTF8' TABLESPACE = road_pricing CONNECTION LIMIT = -1;


-- 微信用户表
DROP TABLE IF EXISTS wx_user;
CREATE TABLE wx_user (
    id                  serial          not null, -- 主键ID
    open_id             varchar(32)     not null, -- open_id
    nickname            varchar(32)     not null, -- 微信昵称
    header_img          varchar(256)    not null, -- 微信头像
    subscribe           int2            not null default 0, -- 是否关注公众号：0-未关注；1-已关注
    create_time         timestamp       not null default current_timestamp, -- 创建时间
    create_ip           varchar(16)     null, -- 创建IP
    update_time         timestamp       null, -- 更新头像或昵称的时间
    CONSTRAINT pk_wx_user PRIMARY KEY (id)
);

-- 微信用户绑定车牌号表
DROP TABLE IF EXISTS wx_car;
CREATE TABLE wx_car (
    id                  serial          not null, -- 主键ID
    user_id             int             not null, -- 用户ID
    number1             varchar(3)      not null, -- 车牌号-省份简称，默认是：皖（注意：一个中文的长度是3，否则会出现：<unreadable data>）
    number2             varchar(7)      not null, -- 车牌号-数字和字母（包含新能源号码）
    status              int2            not null default 0, -- 绑定状态：0-未绑定（默认）；1-已绑定
    create_time         timestamp       not null default current_timestamp, -- 创建时间
    CONSTRAINT pk_wx_car       PRIMARY KEY (id)
);
