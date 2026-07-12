package com.musicplatform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 版权存证记录实体 — 每次版权存证申请保存一条记录。
 */
@Entity
@Table(name = "copyright_records")
public class CopyrightRecord {

    /** 存证记录 ID，如 cr-1690000000000 */
    @Id
    @Column(name = "record_id", length = 64)
    private String recordId;

    /** 存证的版本 ID */
    @Column(name = "version_id", length = 32, nullable = false)
    private String versionId;

    /** 创作者姓名 */
    @Column(name = "creator_name", length = 128, nullable = false)
    private String creatorName;

    /** 证书哈希值 */
    @Column(name = "certificate_hash", length = 128, nullable = false)
    private String certificateHash;

    /** 区块高度（Mock 阶段为 0） */
    @Column(name = "block_height", nullable = false)
    private Long blockHeight;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 提示词交互历史，JSON 字符串 */
    @Column(name = "prompt_history", columnDefinition = "TEXT")
    private String promptHistory;

    /** 修改稿历史，JSON 字符串 */
    @Column(name = "revision_history", columnDefinition = "TEXT")
    private String revisionHistory;

    /** 存证状态：pending / confirmed / verified */
    @Column(length = 20, nullable = false)
    private String status;

    public CopyrightRecord() {
    }

    public CopyrightRecord(String recordId, String versionId, String creatorName,
                           String certificateHash, Long blockHeight,
                           String promptHistory, String revisionHistory, String status) {
        this.recordId = recordId;
        this.versionId = versionId;
        this.creatorName = creatorName;
        this.certificateHash = certificateHash;
        this.blockHeight = blockHeight;
        this.promptHistory = promptHistory;
        this.revisionHistory = revisionHistory;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    // ==================== Getter / Setter ====================

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getVersionId() { return versionId; }
    public void setVersionId(String versionId) { this.versionId = versionId; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public String getCertificateHash() { return certificateHash; }
    public void setCertificateHash(String certificateHash) { this.certificateHash = certificateHash; }

    public Long getBlockHeight() { return blockHeight; }
    public void setBlockHeight(Long blockHeight) { this.blockHeight = blockHeight; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getPromptHistory() { return promptHistory; }
    public void setPromptHistory(String promptHistory) { this.promptHistory = promptHistory; }

    public String getRevisionHistory() { return revisionHistory; }
    public void setRevisionHistory(String revisionHistory) { this.revisionHistory = revisionHistory; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
