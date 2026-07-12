package com.musicplatform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

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

    // ==================== Getter / Setter ====================

    public String getVersionId() { return versionId; }
    public void setVersionId(String versionId) { this.versionId = versionId; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}
