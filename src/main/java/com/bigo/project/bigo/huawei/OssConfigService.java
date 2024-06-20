package com.bigo.project.bigo.huawei;

import com.bigo.common.utils.StringUtils;
import com.bigo.common.utils.ip.IpUtils;
import com.bigo.framework.web.domain.AjaxResult;
import com.bigo.project.bigo.huawei.dao.OssAreaRepository;
import com.bigo.project.bigo.huawei.dao.OssConfigRepository;
import com.bigo.project.bigo.huawei.entity.OssArea;
import com.bigo.project.bigo.huawei.entity.OssConfig;
import com.obs.services.ObsClient;
import com.obs.services.model.PostSignatureRequest;
import com.obs.services.model.PostSignatureResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OssConfigService {

    @Resource
    OssConfigRepository ossConfigRepository;

    @Resource
    OssAreaRepository ossAreaRepository;
    private final static Map<String,OssConfig> OSS_CONFIG_MAP = new HashMap<>();


    public synchronized void init(){
        if(!CollectionUtils.isEmpty(OSS_CONFIG_MAP)){
            return;
        }
        List<OssConfig> ossConfigs = ossConfigRepository.findAllByDelFlagFalse();
        List<OssArea> ossAreas = ossAreaRepository.findAllByDelFlagFalse();
        Map<String, OssConfig> configMap = ossConfigs.stream().collect(Collectors.toMap(t -> t.getBucketName(), t -> t, (t1, t2) -> t1));
        Optional<OssConfig> first = ossConfigs.stream().filter(t -> Optional.ofNullable(t.getDefaultFlag()).orElse(false)).findFirst();
        Map<String, OssConfig> ossConfigMap = ossAreas.stream().collect(Collectors.toMap(t -> t.getAreaName(), t -> configMap.get(t.getBucketName())));
        log.info("ossConfigMap={}",ossConfigMap);
        OSS_CONFIG_MAP.putAll(ossConfigMap);
        OSS_CONFIG_MAP.put("default",first.get());
        log.info("ossConfigMap default={}",ossConfigMap);
    }

    public AjaxResult getPolicy(HttpServletRequest request){
        OssConfig ossConfig = queryBucket(request);
        if(ossConfig==null){
            return AjaxResult.error("oss not exist");
        }
        OSSPolicyOutputDTO outputDTO = new OSSPolicyOutputDTO();
        try {
            String accessKeyId = ossConfig.getAccessKeyId();
            String accessKeySecret = ossConfig.getAccessKeySecret();
            String endPoint = ossConfig.getEndPoint();
            String bucketName = ossConfig.getBucketName();
            String protocol = ossConfig.getProtocol();
            // 创建ObsClient实例
            final ObsClient obsClient = new ObsClient(accessKeyId, accessKeySecret, endPoint);
            PostSignatureRequest postSignatureRequest = new PostSignatureRequest();
            // 设置表单参数
            Map<String, Object> formParams = new HashMap<String, Object>();
//             设置对outputDTO象访问权限为公共读
            formParams.put("x-obs-acl", "public-read");
//             设置对象MIME类型
            formParams.put("content-type", "text/plain");
            postSignatureRequest.setFormParams(formParams);
            // 设置表单上传请求有效期，单位：秒
            postSignatureRequest.setExpires(600);

            postSignatureRequest.setBucketName(bucketName);
            PostSignatureResponse response = obsClient.createPostSignature(postSignatureRequest);

            outputDTO.setAccessKeyId(accessKeyId);
            outputDTO.setPolicy(response.getPolicy());
            outputDTO.setSignature(response.getSignature());
            outputDTO.setHost(protocol + "://" + bucketName + "." + endPoint);

            return AjaxResult.success(outputDTO);
        } catch (Exception e) {
            log.error("获取华为云OBS上传策略失败：{}", e);
        }
        return AjaxResult.error("获取华为云OBS上传策略失败");
    }
    
    public OssConfig queryBucket(HttpServletRequest request){
        init();
        String ipAddr = IpUtils.getIpAddr(request);
        String address = IpUtils.getAddress(ipAddr);
        if(!StringUtils.isEmpty(address)){
            String[] splits = address.split("\\|");
            if(splits.length>0){
                String countryName = splits[0];
                log.info("countryName={}",countryName);
                return Optional.ofNullable(OSS_CONFIG_MAP.get(countryName)).orElse(OSS_CONFIG_MAP.get("default"));
            }
        }
        return OSS_CONFIG_MAP.get("default");
    }

    public AjaxResult clearPolicy(HttpServletRequest request) {
        OSS_CONFIG_MAP.clear();
        return AjaxResult.success();
    }
}
