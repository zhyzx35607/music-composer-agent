package com.musicplatform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicplatform.model.MusicVersion;
import com.musicplatform.repository.MusicVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 音乐生成核心业务逻辑。
 * 第一阶段（Day 1-2）：使用 Mock 数据，保证前后端链路通畅。
 * 第二阶段：替换为调用智能体（成员C）和生成链路（成员D）的真实实现。
 */
@Service
public class MusicService {

    private static final Logger log = LoggerFactory.getLogger(MusicService.class);

    private final MusicVersionRepository repository;
    private final ObjectMapper objectMapper;

    /** 输出目录路径，来自 application.yml */
    @Value("${music-platform.output-dir:./outputs}")
    private String outputDir;

    public MusicService(MusicVersionRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * 生成下一个版本 ID（线程安全）。
     * 基于已有版本中的最大数字编号递增，而非 count()，
     * 避免删除版本后 ID 冲突和并发问题。
     */
    private synchronized String nextVersionId() {
        int maxNum = repository.findAll().stream()
                .map(v -> v.getVersionId().substring(1))  // 去掉 "v" 前缀
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);
        return "v" + (maxNum + 1);
    }

    /**
     * 首次生成音乐 — 根据用户需求和参数创建第一个版本。
     * 第一阶段返回 Mock 数据，后续接入成员C的智能体。
     */
    @Transactional
    public MusicVersion generate(String userPrompt, String style, String mood,
                                  String tempo, String instrumentsJson) {
        // 生成版本ID
        String versionId = nextVersionId();

        log.info("创建新版本 | version_id={} | style={} | mood={}", versionId, style, mood);

        // 创建版本记录
        MusicVersion version = new MusicVersion(versionId, userPrompt,
                style, mood, tempo, instrumentsJson);
        version.setCreatedAt(LocalDateTime.now());

        // ========== Mock 数据（第一阶段：Day 1-2）==========
        // TODO: 第二阶段替换为调用成员C的智能体
        version.setPlan(buildMockPlan(style, mood, tempo, instrumentsJson));
        version.setCaption(buildMockCaption(style, mood, tempo, instrumentsJson));
        version.setMidiPath(outputDir + "/" + versionId + "/" + versionId + ".mid");
        version.setAudioPath(outputDir + "/" + versionId + "/" + versionId + ".wav");

        // 确保版本目录存在
        new File(outputDir + "/" + versionId).mkdirs();

        // 存入数据库
        MusicVersion saved = repository.save(version);
        log.info("版本已保存 | version_id={} | midi_path={} | audio_path={}",
                saved.getVersionId(), saved.getMidiPath(), saved.getAudioPath());
        return saved;
    }

    /**
     * 反馈修改 — 根据用户反馈在指定版本基础上生成新版本。
     */
    @Transactional
    public MusicVersion revise(String parentVersionId, String feedback) {
        log.info("反馈修改 | parent={} | feedback={}", parentVersionId, feedback);

        // 查找父版本
        MusicVersion parent = repository.findById(parentVersionId)
                .orElseThrow(() -> new NoSuchElementException("版本不存在: " + parentVersionId));

        // 生成新版本ID
        String versionId = nextVersionId();

        log.info("基于 {} 创建新版本 | version_id={}", parentVersionId, versionId);

        // 基于父版本创建新版本
        MusicVersion version = new MusicVersion(versionId,
                parent.getUserPrompt(),
                parent.getStyle(), parent.getMood(),
                parent.getTempo(), parent.getInstruments());
        version.setParentVersionId(parentVersionId);
        version.setFeedback(feedback);
        version.setCreatedAt(LocalDateTime.now());

        // ========== Mock 数据（第二阶段实现真实逻辑）==========
        // TODO: 第二阶段替换为调用成员C的智能体（反馈修改模式）
        version.setPlan(buildMockRevisedPlan(parent.getPlan(), feedback));
        version.setCaption(buildMockRevisedCaption(parent.getCaption(), feedback));
        version.setMidiPath(outputDir + "/" + versionId + "/" + versionId + ".mid");
        version.setAudioPath(outputDir + "/" + versionId + "/" + versionId + ".wav");
        version.setChangeReason("根据用户反馈「" + feedback + "」，调整了音乐参数。"
                + "（第二阶段将接入智能体生成详细的修改原因）");

        // 确保版本目录存在
        new File(outputDir + "/" + versionId).mkdirs();

        MusicVersion saved = repository.save(version);
        log.info("修改版本已保存 | version_id={} | parent={}",
                saved.getVersionId(), saved.getParentVersionId());
        return saved;
    }

    /** 获取所有版本列表（按时间倒序） */
    public List<MusicVersion> getAllVersions() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    /** 获取指定版本详情 */
    public Optional<MusicVersion> getVersion(String versionId) {
        return repository.findById(versionId);
    }

    /** 分页获取版本列表（按时间倒序） */
    public Page<MusicVersion> getVersionsPaged(int page, int size) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    /** 获取版本总数 */
    public long getVersionCount() {
        return repository.count();
    }

    // ===================== Mock 数据辅助方法 =====================

    private String buildMockPlan(String style, String mood, String tempo,
                                  String instrumentsJson) {
        try {
            List<String> instruments = objectMapper.readValue(
                    instrumentsJson, objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, String.class));

            Map<String, Object> plan = new LinkedHashMap<>();
            plan.put("theme", "a beautiful musical piece");
            plan.put("style", style);
            plan.put("mood", Arrays.asList(mood));
            plan.put("tempo", convertTempo(tempo));
            plan.put("key", "C major");
            plan.put("instruments", instruments);
            plan.put("structure", Arrays.asList("intro", "verse", "chorus", "outro"));
            return objectMapper.writeValueAsString(plan);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String buildMockCaption(String style, String mood, String tempo,
                                     String instrumentsJson) {
        try {
            List<String> instruments = objectMapper.readValue(
                    instrumentsJson, objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, String.class));
            String instStr = String.join(", ", instruments);
            return String.format(
                    "A %s instrumental piece with a %s mood, %s tempo, featuring %s.",
                    style, mood, tempo, instStr);
        } catch (JsonProcessingException e) {
            return "A beautiful instrumental piece.";
        }
    }

    private String buildMockRevisedPlan(String oldPlanJson, String feedback) {
        // 第一阶段简单处理：返回原plan，附加一条修改备注
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> plan = objectMapper.readValue(oldPlanJson, Map.class);
            plan.put("revised_based_on_feedback", feedback);
            return objectMapper.writeValueAsString(plan);
        } catch (JsonProcessingException e) {
            return oldPlanJson;
        }
    }

    private String buildMockRevisedCaption(String oldCaption, String feedback) {
        return oldCaption + " (Revised based on feedback: " + feedback + ")";
    }

    private int convertTempo(String tempo) {
        return switch (tempo.toLowerCase()) {
            case "slow" -> 72;
            case "fast" -> 120;
            default -> 88; // medium
        };
    }
}
