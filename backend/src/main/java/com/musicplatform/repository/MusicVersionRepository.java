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

    /** 获取全局最大版本序号（用于生成下一个版本 ID），无版本时返回 0 */
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(v.versionId, 2) AS integer)), 0) FROM MusicVersion v")
    int findMaxGlobalVersionNumber();

    // ---- Track 相关查询 ----

    /** Track 是否存在 */
    boolean existsByTrackId(String trackId);

    /** Track 内最大版本号 */
    @Query("SELECT COALESCE(MAX(v.versionNumber), 0) FROM MusicVersion v WHERE v.trackId = :trackId")
    int findMaxVersionNumberInTrack(String trackId);

    /** 所有 Track 摘要（每个 track 的最新版本），排除旧数据中 track_id 为 null 的记录 */
    @Query("SELECT v FROM MusicVersion v WHERE v.trackId IS NOT NULL AND (v.trackId, v.versionNumber) IN "
         + "(SELECT v2.trackId, MAX(v2.versionNumber) FROM MusicVersion v2 WHERE v2.trackId IS NOT NULL "
         + "GROUP BY v2.trackId) ORDER BY v.createdAt DESC")
    List<MusicVersion> findLatestPerTrack();

    /** 某 Track 的所有版本（按版本号倒序） */
    List<MusicVersion> findByTrackIdOrderByVersionNumberDesc(String trackId);

}
