package com.musicplatform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 音乐版本实体 — 每次生成/修改都会创建一条新记录。
 * 与计划书第12.4节的数据结构对齐。
 */
@Entity
@Table(name = "music_versions")
public class MusicVersion {

    /** 版本ID，例如 v1, v2, v3 */
    @Id
    @Column(name = "version_id", length = 32)
    private String versionId;

    /** 父版本ID — 如果是全新生成则为 null，如果是反馈修改则指向上一版 */
    @Column(name = "parent_version_id", length = 32)
    private String parentVersionId;

    /** 用户最初的中文需求 */
    @Column(name = "user_prompt", columnDefinition = "TEXT")
    private String userPrompt;

    /** 风格，例如 pop ballad */
    @Column(length = 64)
    private String style;

    /** 情绪，例如 nostalgic */
    @Column(length = 64)
    private String mood;

    /** 速度，例如 medium 或 82 */
    @Column(length = 32)
    private String tempo;

    /** 乐器列表，JSON数组字符串 */
    @Column(columnDefinition = "TEXT")
    private String instruments;

    /** 用户反馈修改意见（首次生成时为 null） */
    @Column(columnDefinition = "TEXT")
    private String feedback;

    /** 智能体生成的结构化作曲方案，JSON字符串 */
    @Column(columnDefinition = "TEXT")
    private String plan;

    /** Text2MIDI 使用的英文描述 */
    @Column(columnDefinition = "TEXT")
    private String caption;

    /** MIDI 文件路径 */
    @Column(name = "midi_path", length = 512)
    private String midiPath;

    /** WAV 音频文件路径 */
    @Column(name = "audio_path", length = 512)
    private String audioPath;

    /** 智能体给出的修改原因说明 */
    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 是否为 Mock 版本（管线不可用时降级生成） */
    @Column(name = "is_mock")
    private boolean mock;

    // ==================== 构造方法 ====================

    public MusicVersion() {
    }

    /**
     * 首次生成使用的构造方法
     */
    public MusicVersion(String versionId, String userPrompt,
                        String style, String mood, String tempo, String instruments) {
        this.versionId = versionId;
        this.userPrompt = userPrompt;
        this.style = style;
        this.mood = mood;
        this.tempo = tempo;
        this.instruments = instruments;
        this.createdAt = LocalDateTime.now();
    }

    // ==================== Getter / Setter ====================

    public String getVersionId() { return versionId; }
    public void setVersionId(String versionId) { this.versionId = versionId; }

    public String getParentVersionId() { return parentVersionId; }
    public void setParentVersionId(String parentVersionId) { this.parentVersionId = parentVersionId; }

    public String getUserPrompt() { return userPrompt; }
    public void setUserPrompt(String userPrompt) { this.userPrompt = userPrompt; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public String getTempo() { return tempo; }
    public void setTempo(String tempo) { this.tempo = tempo; }

    public String getInstruments() { return instruments; }
    public void setInstruments(String instruments) { this.instruments = instruments; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public String getMidiPath() { return midiPath; }
    public void setMidiPath(String midiPath) { this.midiPath = midiPath; }

    public String getAudioPath() { return audioPath; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }

    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isMock() { return mock; }
    public void setMock(boolean mock) { this.mock = mock; }
}
