package com.musicplatform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 合规检测记录实体 — 每次合规检测保存一条记录。
 */
@Entity
@Table(name = "compliance_records")
public class ComplianceRecord {

    /** 自增主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 被检测的版本 ID */
    @Column(name = "version_id", length = 32, nullable = false)
    private String versionId;

    /** 检测类型：melody / lyrics / timbre / all */
    @Column(name = "check_type", length = 20, nullable = false)
    private String checkType;

    /** 风险等级：low / medium / high */
    @Column(name = "risk_level", length = 10, nullable = false)
    private String riskLevel;

    /** 总体相似度评分 (0-100) */
    @Column(name = "overall_score", nullable = false)
    private Double overallScore;

    /** 三维度详情，JSON 字符串 */
    @Column(columnDefinition = "TEXT")
    private String details;

    /** 匹配作品列表，JSON 字符串 */
    @Column(columnDefinition = "TEXT")
    private String matchedWorks;

    /** 检测时间 */
    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    public ComplianceRecord() {
    }

    public ComplianceRecord(String versionId, String checkType, String riskLevel,
                            Double overallScore, String details, String matchedWorks) {
        this.versionId = versionId;
        this.checkType = checkType;
        this.riskLevel = riskLevel;
        this.overallScore = overallScore;
        this.details = details;
        this.matchedWorks = matchedWorks;
        this.checkedAt = LocalDateTime.now();
    }

    // ==================== Getter / Setter ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVersionId() { return versionId; }
    public void setVersionId(String versionId) { this.versionId = versionId; }

    public String getCheckType() { return checkType; }
    public void setCheckType(String checkType) { this.checkType = checkType; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public Double getOverallScore() { return overallScore; }
    public void setOverallScore(Double overallScore) { this.overallScore = overallScore; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getMatchedWorks() { return matchedWorks; }
    public void setMatchedWorks(String matchedWorks) { this.matchedWorks = matchedWorks; }

    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }
}
