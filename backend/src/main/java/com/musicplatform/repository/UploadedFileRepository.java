package com.musicplatform.repository;

import com.musicplatform.model.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 上传文件数据访问层。
 */
@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    /** 按版本 ID 查找所有关联文件 */
    List<UploadedFile> findByVersionIdOrderByCreatedAtAsc(String versionId);

    /** 按 Track ID 查找所有关联文件（用于生成时注入 prompt） */
    List<UploadedFile> findByTrackIdOrderByCreatedAtAsc(String trackId);

    /** 删除某版本的所有关联文件 */
    void deleteByVersionId(String versionId);
}
