package com.bigo.project.bigo.huawei.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name="oss_config",uniqueConstraints = {@UniqueConstraint(columnNames = {"bucket_name"})})
public class OssConfig {

    @Id
    @Column(name="id")
    Integer id;

    @Column(name="del_flag")
    Boolean delFlag;

    @Column(name="access_key_id")
    private String accessKeyId;

    @Column(name="access_key_secret")
    private String accessKeySecret;

    @Column(name="end_point")
    private String endPoint;

    @Column(name="protocol")
    private String protocol;

    @Column(name="bucket_name")
    private String bucketName;

    @Column(name="domain")
    private String domain;

    @Column(name="remark")
    private String remark;

    @Column(name="default_flag")
    private Boolean defaultFlag;
}
