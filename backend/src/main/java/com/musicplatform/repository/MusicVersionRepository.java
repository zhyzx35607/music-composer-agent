package com.musicplatform.repository;

import com.musicplatform.model.MusicVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 音乐版本数据访问层 — Spring Data JPA 自动实现。
 */
@Repository
public interface MusicVersionRepository extends JpaRepository<MusicVersion, String> {

    /** 按创建时间降序获取所有版本 */
    List<MusicVersion> findAllByOrderByCreatedAtDesc();

    /** 按创建时间降序分页获取版本 */
    Page<MusicVersion> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** 获取最大版本序号（用于生成下一个版本 ID），无版本时返回 0 */
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(v.versionId, 2) AS integer)), 0) FROM MusicVersion v")
    int findMaxVersionNumber();

}
