package com.bigo.project.bigo.pay.channel.dayangpay;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.springframework.util.Assert;

/**
 * xm支付
 */
@Data
public class DayangpayConfig {

    public final static String CHANNEL_NAME  = "dayangpay";


    public static final String RETURN_VALUE_SUCCESS = "{\"code\":\"SUCCESS\"}";
    public static final String RETURN_VALUE_FAIL = "{\"code\":\"FAIL\"}";



    private String privateKey;

    private String channelId;
    // 支付Key
    private String clientKey;
    // 支付请求地址
    private String reqPayUrl;
    // 支付回调地址
    private String notifyPayUrl;

    // 代付请求地址
    private String reqTransUrl;
    // 代付回调地址
    private String notifyTransUrl;

    // 查询地址
    private String queryUrl;

    private String currency;

    private Long rate;
    public DayangpayConfig(String payParam) {
        Assert.notNull(payParam, "init dayangpay config error");
        JSONObject object = JSONObject.parseObject(payParam);
        this.channelId = object.getString("channelId");
        this.clientKey = object.getString("clientKey");
        this.privateKey = object.getString("privateKey");
        this.reqPayUrl = object.getString("reqPayUrl");
        this.notifyPayUrl = object.getString("notifyPayUrl");
        this.reqTransUrl = object.getString("reqTransUrl");
        this.notifyTransUrl = object.getString("notifyTransUrl");
        this.currency = object.getString("currency");
        this.rate = object.getLong("rate");
    }

}
