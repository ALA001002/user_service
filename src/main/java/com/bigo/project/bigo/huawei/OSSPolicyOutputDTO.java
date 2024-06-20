package com.bigo.project.bigo.huawei;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 获取OSS上传策略出参
 *
 * @author 08011495
 */
@Data
public class OSSPolicyOutputDTO {

    @ApiModelProperty("访问密钥ID")
    private String accessKeyId;

    @ApiModelProperty("上传策略")
    private String policy;

    @ApiModelProperty("签名")
    private String signature;

    @ApiModelProperty("上传路径前缀")
    private String pathPrefix;

    @ApiModelProperty("OSS服务器主机地址")
    private String host;

    @ApiModelProperty("过期时间，单位秒")
    private String expireTime;
}
