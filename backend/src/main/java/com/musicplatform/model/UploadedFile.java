package com.musicplatform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 上传文件实体 — 用户上传的参考文件（txt/docx/xlsx/musicxml）。
 * 关联到版本，提取内容后注入管线 prompt。
 */
@Entity
@Table(name = "uploaded_files")
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的版本 ID（选填，上传时可能还没生成版本） */
    @Column(name = "version_id", length = 32)
    private String versionId;

    /** Track ID（用于在生成前查找该 track 下的所有文件） */
    @Column(name = "track_id", length = 36)
    private String trackId;

    /** 原始文件名 */
    @Column(name = "original_name", length = 256)
    private String originalName;

    /** 文件类型：txt / docx / xlsx / musicxml */
    @Column(name = "file_type", length = 16)
    private String fileType;

    /** 服务器存储路径 */
    @Column(name = "stored_path", length = 512)
    private String storedPath;

    /** 提取的文本内容（供 prompt 使用） */
    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    /** MusicXML 的结构化解析结果（JSON 字符串） */
    @Column(name = "extracted_json", columnDefinition = "TEXT")
    private String extractedJson;

    /** 文件大小（字节） */
    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ==================== 构造方法 ====================

    public UploadedFile() {}

    public UploadedFile(String originalName, String fileType, String storedPath,
                        String extractedText, String extractedJson, Long fileSize,
                        String versionId, String trackId) {
        this.originalName = originalName;
        this.fileType = fileType;
        this.storedPath = storedPath;
        this.extractedText = extractedText;
        this.extractedJson = extractedJson;
        this.fileSize = fileSize;
        this.versionId = versionId;
        this.trackId = trackId;
        this.createdAt = LocalDateTime.now();
    }

    // ==================== Getter / Setter ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVersionId() { return versionId; }
    public void setVersionId(String versionId) { this.versionId = versionId; }

    public String getTrackId() { return trackId; }
    public void setTrackId(String trackId) { this.trackId = trackId; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getStoredPath() { return storedPath; }
    public void setStoredPath(String storedPath) { this.storedPath = storedPath; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public String getExtractedJson() { return extractedJson; }
    public void setExtractedJson(String extractedJson) { this.extractedJson = extractedJson; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
