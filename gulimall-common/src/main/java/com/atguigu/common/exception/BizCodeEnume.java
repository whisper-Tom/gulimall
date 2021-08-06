package com.atguigu.common.exception;

/**
 * enum和class、interface的地位一样
 使用enum定义的枚举类默认继承了java.lang.Enum，而不是继承Object类。枚举类可以实现一个或多个接口。
 枚举类的所有实例都必须放在第一行展示，不需使用new 关键字，不需显式调用构造器。自动添加public static final修饰。
 使用enum定义、非抽象的枚举类默认使用final修饰，不可以被继承。
 枚举类的构造器只能是私有的
 */
public enum BizCodeEnume {
     //枚举类中包含的对象
    UNKNOW_EXCEPTION(10000,"系统未知异常"),
    VAILD_EXCEPTION(10001,"参数格式校验失败"),
    TO_MANY_REQUEST(10002,"请求流量过大，请稍后再试"),
    SMS_CODE_EXCEPTION(10003,"验证码获取频率太高，请稍后再试"),
    READ_TIME_OUT_EXCEPTION(10004,"远程调用服务超时，请重新再试"),
    SECKILL_EXCEPTION(10005,"秒杀请求过多，请重新再试"),
    PRODUCT_UP_EXCEPTION(11000,"商品上架异常"),
    USER_EXIST_EXCEPTION(15001,"存在相同的用户"),
    PHONE_EXIST_EXCEPTION(15002,"存在相同的手机号"),
    NO_STOCK_EXCEPTION(21000,"商品库存不足"),
    LOGINACCT_PASSWORD_EXCEPTION(15003,"账号或密码错误"),;



    private int code;
    private String msg;
    BizCodeEnume(int code,String msg){
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
