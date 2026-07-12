package com.musicplatform.repository;

import com.musicplatform.model.ComplianceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 合规检测记录仓库。
 */
public interface ComplianceRecordRepository extends JpaRepository<ComplianceRecord, Long> {

    /** 按检测时间倒序获取所有记录 */
    List<ComplianceRecord> findAllByOrderByCheckedAtDesc();

    /** 根据版本 ID 查询检测记录 */
    List<ComplianceRecord> findByVersionIdOrderByCheckedAtDesc(String versionId);
}
