package com.bigo.project.bigo.huawei.dao;

import com.bigo.project.bigo.huawei.entity.OssConfig;
import com.bigo.project.bigo.wallet.dao.BaseRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OssConfigRepository extends BaseRepository<OssConfig> {
    OssConfig findFirstByDelFlagFalse();

    OssConfig findFirstByDefaultFlagTrue();

    List<OssConfig> findAllByDelFlagFalse();

    @Query("select A from OssConfig A,OssArea B where A.bucketName=B.bucketName and B.areaName=:areaName")
    OssConfig queryOssConfigByAreaName(String areaName);
}
