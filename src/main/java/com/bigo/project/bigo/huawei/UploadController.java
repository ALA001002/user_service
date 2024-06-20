package com.bigo.project.bigo.huawei;

import com.bigo.framework.web.domain.AjaxResult;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping(value="/api/huawei/upload")
public class UploadController {

    @Resource
    OssConfigService ossConfigService;


    @GetMapping("/getPolicy")
    @ApiOperation(value = "获取华为云OBS上传策略", httpMethod = "GET", notes = "获取华为云OBS上传策略")
    @ResponseBody
    public AjaxResult getPolicy(HttpServletRequest request) {
        log.info("===========用户上传身份认证============");
        return ossConfigService.getPolicy(request);
    }


    @GetMapping("/clearPolicy")
    @ApiOperation(value = "获取华为云OBS上传策略", httpMethod = "GET", notes = "获取华为云OBS上传策略")
    @ResponseBody
    public AjaxResult clearPolicy(HttpServletRequest request) {
        return ossConfigService.clearPolicy(request);
    }
}
