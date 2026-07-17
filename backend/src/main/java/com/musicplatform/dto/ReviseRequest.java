package com.musicplatform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * POST /api/revise 请求体
 */
public class ReviseRequest {

    /** 要修改的历史版本 ID，必填 */
    @NotBlank(message = "version_id 不能为空")
    @JsonProperty("version_id")
    private String versionId;

    /** 用户反馈意见，必填 */
    @NotBlank(message = "feedback 不能为空")
    private String feedback;

    /** 版本描述 — 选填，用户手动输入；不填则后端自动生成 */
    @JsonProperty("version_label")
    private String versionLabel;

    /** Optional uploaded reference file ids, especially MusicXML/MXL score files. */
    @JsonProperty("reference_file_ids")
    private List<Long> referenceFileIds;

    // ==================== Getter / Setter ====================

    public String getVersionId() { return versionId; }
    public void setVersionId(String versionId) { this.versionId = versionId; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getVersionLabel() { return versionLabel; }
    public void setVersionLabel(String versionLabel) { this.versionLabel = versionLabel; }

    public List<Long> getReferenceFileIds() { return referenceFileIds; }
    public void setReferenceFileIds(List<Long> referenceFileIds) { this.referenceFileIds = referenceFileIds; }
}
