package com.musicplatform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * POST /api/generate 请求体
 */
public class GenerateRequest {

    /** 用户中文需求，必填 */
    @NotBlank(message = "user_prompt 不能为空")
    @Size(max = 2000, message = "user_prompt 最长 2000 字符")
    @JsonProperty("user_prompt")
    private String userPrompt;

    /** 音乐名称，用户手动输入，如"毕业季的歌" */
    @JsonProperty("track_name")
    private String trackName;

    /** Track ID — 选填，传了则在已有 track 下追加版本；不传则新建 track */
    @JsonProperty("track_id")
    private String trackId;

    /** 版本描述 — 选填，用户手动输入（类似 Git commit message）；不填则后端自动生成 */
    @JsonProperty("version_label")
    private String versionLabel;

    /** 音乐风格，选填，默认 "pop" */
    private String style;

    /** 音乐情绪，选填，默认 "calm" */
    private String mood;

    /** 速度，选填，默认 "medium" */
    private String tempo;

    /** 目标时长（秒），选填。优先于从 user_prompt 中解析的时长 */
    @JsonProperty("duration_seconds")
    private Integer durationSeconds;

    /** 乐器列表，选填，默认 ["piano"] */
    private List<String> instruments;

    /** Optional uploaded reference file ids, especially MusicXML/MXL score files. */
    @JsonProperty("reference_file_ids")
    private List<Long> referenceFileIds;

    // ==================== Getter / Setter ====================

    public String getUserPrompt() { return userPrompt; }
    public void setUserPrompt(String userPrompt) { this.userPrompt = userPrompt; }

    public String getTrackName() { return trackName; }
    public void setTrackName(String trackName) { this.trackName = trackName; }

    public String getTrackId() { return trackId; }
    public void setTrackId(String trackId) { this.trackId = trackId; }

    public String getVersionLabel() { return versionLabel; }
    public void setVersionLabel(String versionLabel) { this.versionLabel = versionLabel; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public String getTempo() { return tempo; }
    public void setTempo(String tempo) { this.tempo = tempo; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public List<String> getInstruments() { return instruments; }
    public void setInstruments(List<String> instruments) { this.instruments = instruments; }

    public List<Long> getReferenceFileIds() { return referenceFileIds; }
    public void setReferenceFileIds(List<Long> referenceFileIds) { this.referenceFileIds = referenceFileIds; }
}
