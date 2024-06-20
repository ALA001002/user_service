package com.bigo.project.bigo.pay.channel.dayangpay;

import com.alibaba.fastjson.JSONObject;
import com.bigo.common.utils.DateUtils;
import com.bigo.common.utils.OkHttpUtil;
import com.bigo.common.utils.PayDigestUtil;
import com.bigo.project.bigo.pay.domain.PayOrder;
import com.bigo.project.bigo.pay.entity.BasePayment;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @description:
 * @author: wenxm
 * @date: 2021/5/21 17:54
 */
@Slf4j
@Service
public class DayangpayPaymentService extends BasePayment {



    @Override
    public String getChannelName() {
        return DayangpayConfig.CHANNEL_NAME;
    }

    public JSONObject pay(PayOrder payOrder) {
        DayangpayConfig config = new DayangpayConfig(getPayParam(payOrder));
        JSONObject retObj = new JSONObject();
        JSONObject paramMap = new JSONObject();


        Long currencyAmount = payOrder.getAmount() * config.getRate();
        payOrder.setCurrencyAmount(new BigDecimal(currencyAmount));

        String payOrderId = payOrder.getPayOrderId();
        paramMap.put("client_key", config.getClientKey());
        paramMap.put("amount", payOrder.getCurrencyAmount().toString());
        paramMap.put("channel_id", config.getChannelId());
        paramMap.put("out_trade_no", payOrderId);
        paramMap.put("notify_url", config.getNotifyPayUrl());

        String signValue = PayDigestUtil.HmacSHA256(paramMap,config.getPrivateKey()).toLowerCase();
        log.info("Sign Result:" + signValue);
        paramMap.put("signature", signValue);

        try {
            JSONObject params = paramMap;
            log.info("[{}]请求数据:{}", getChannelName(), params);
            String reqUrl = config.getReqPayUrl();
            log.info("[{}]请求URL:{}", getChannelName(), reqUrl);
            String result = OkHttpUtil.postJsonParams(reqUrl, paramMap.toJSONString());
            log.info("[{}]返回数据:{}", getChannelName(), result);
            if (StringUtils.isBlank(result)) {
                log.error("[{}]上游通道返回空订单号",getChannelName());
                retObj.put("errDes", "upstream_channel_returns_empty");
                retObj.put("retCode", "FAIL");
                return retObj;
            }
            JSONObject resObj = JSONObject.parseObject(result);
            Integer respCode = resObj.getInteger("status");
            if(respCode == 0) {
                String payUrl = resObj.getString("payment_url");
                int updateCount = payOrderService.updateStatusAndAmount(payOrder.getPayOrderId(), currencyAmount);
                log.info("[{}]更新订单状态为支付中:payOrderId={},payId={},result={}", getChannelName(), payOrder.getPayOrderId(), null, updateCount);
                retObj.put("payUrl", payUrl);
                retObj.put("payOrderId", payOrder.getPayOrderId()); // 设置支付订单ID
                retObj.put("errDes", "");
                retObj.put("retCode", "SUCCESS");
                return retObj;
            }else {
                String msg = resObj.getString("msg");
                retObj.put("errDes", "upstream_channel_returns_empty");
                retObj.put("retMsg", msg);
                retObj.put("retCode", "FAIL");
                return retObj;
            }
        }catch (Exception e) {
            retObj.put("retMsg", e.getMessage());
            retObj.put("errDes", "upstream_channel_returns_empty");
            retObj.put("retCode", "FAIL");
            return retObj;
        }
    }

}
