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

    /** 音乐风格，选填，默认 "pop" */
    private String style;

    /** 音乐情绪，选填，默认 "calm" */
    private String mood;

    /** 速度，选填，默认 "medium" */
    private String tempo;

    /** 乐器列表，选填，默认 ["piano"] */
    private List<String> instruments;

    // ==================== Getter / Setter ====================

    public String getUserPrompt() { return userPrompt; }
    public void setUserPrompt(String userPrompt) { this.userPrompt = userPrompt; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public String getTempo() { return tempo; }
    public void setTempo(String tempo) { this.tempo = tempo; }

    public List<String> getInstruments() { return instruments; }
    public void setInstruments(List<String> instruments) { this.instruments = instruments; }
}
