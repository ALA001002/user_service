package com.bigo.project.bigo.pay.entity;

import com.alibaba.fastjson.JSONObject;

/**
 * @author: dingzhiwei
 * @date: 18/08/16
 * @description: 转账通知接口
 */
public interface TransNotifyInterface {

    JSONObject doNotify(Object notifyData);


}
