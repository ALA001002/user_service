
package com.bigo.project.bigo.config.dao;

import com.bigo.project.bigo.config.jpaEntity.EmailConfig;
import com.bigo.project.bigo.wallet.dao.BaseRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EmailConfigRepository extends BaseRepository<EmailConfig> {

    @Query(value = "select * from bg_email_config where status=0",nativeQuery = true)
    List<EmailConfig> selectStatusList();


}
