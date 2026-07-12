package com.musicplatform.repository;

import com.musicplatform.model.CopyrightRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 版权存证记录仓库。
 */
public interface CopyrightRecordRepository extends JpaRepository<CopyrightRecord, String> {

    /** 按创建时间倒序获取所有记录 */
    List<CopyrightRecord> findAllByOrderByCreatedAtDesc();
}
