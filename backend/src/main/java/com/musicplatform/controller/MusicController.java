package com.musicplatform.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicplatform.dto.ApiResponse;
import com.musicplatform.dto.GenerateRequest;
import com.musicplatform.dto.ReviseRequest;
import com.musicplatform.model.MusicVersion;
import com.musicplatform.service.MusicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 音乐创作平台 REST API。
 *
 * 接口总览（对应计划书第9节）：
 *   POST /api/generate          — 首次生成音乐
 *   POST /api/revise            — 反馈修改
 *   GET  /api/versions          — 历史版本列表
 *   GET  /api/version/{id}      — 版本详情
 *   GET  /outputs/{filename}    — 静态文件访问（MIDI / WAV）
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@Tag(name = "音乐创作", description = "音乐生成、反馈修改与版本管理接口")
public class MusicController {

    private static final Logger log = LoggerFactory.getLogger(MusicController.class);

    private final MusicService musicService;
    private final ObjectMapper objectMapper;

    public MusicController(MusicService musicService, ObjectMapper objectMapper) {
        this.musicService = musicService;
        this.objectMapper = objectMapper;
    }

    /**
     * POST /api/generate — 首次生成音乐
     */
    @Operation(
            summary = "首次生成音乐",
            description = "根据用户中文需求和音乐参数，生成作曲方案（plan）、英文描述（caption）、MIDI 和 WAV 文件路径。"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "音乐生成请求",
            content = @Content(examples = @ExampleObject(value = """
            {
              "user_prompt": "我想要一首毕业季的歌，抒情一点",
              "style": "pop ballad",
              "mood": "nostalgic",
              "tempo": "medium",
              "instruments": ["piano", "strings"]
            }"""))
    )
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generate(
            @Valid @RequestBody GenerateRequest request) throws JsonProcessingException {

        log.info("收到生成请求 | style={} | mood={} | tempo={}",
                request.getStyle(), request.getMood(), request.getTempo());

        long start = System.currentTimeMillis();

        // 设置默认值
        String style = request.getStyle() != null ? request.getStyle() : "pop";
        String mood = request.getMood() != null ? request.getMood() : "calm";
        String tempo = request.getTempo() != null ? request.getTempo() : "medium";
        List<String> instruments = request.getInstruments() != null
                ? request.getInstruments() : Arrays.asList("piano");
        String instrumentsJson = objectMapper.writeValueAsString(instruments);

        MusicVersion version = musicService.generate(
                request.getUserPrompt(), style, mood, tempo, instrumentsJson);

        long elapsed = System.currentTimeMillis() - start;
        log.info("生成完成 | version_id={} | 耗时={}ms", version.getVersionId(), elapsed);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(buildGenerateResponse(version)));
    }

    /**
     * POST /api/revise — 反馈修改
     */
    @Operation(
            summary = "反馈修改",
            description = "基于指定历史版本和用户反馈意见，生成新的音乐版本，返回修改后的 plan、caption、change_reason。"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "修改请求",
            content = @Content(examples = @ExampleObject(value = """
            {
              "version_id": "v1",
              "feedback": "这版太悲伤了，想要更有希望一点，加一点钢琴"
            }"""))
    )
    @PostMapping("/revise")
    public ResponseEntity<ApiResponse<Map<String, Object>>> revise(
            @Valid @RequestBody ReviseRequest request) throws JsonProcessingException {

        log.info("收到修改请求 | parent_version={} | feedback={}",
                request.getVersionId(), request.getFeedback());

        long start = System.currentTimeMillis();

        MusicVersion version = musicService.revise(request.getVersionId(), request.getFeedback());

        long elapsed = System.currentTimeMillis() - start;
        log.info("修改完成 | version_id={} | parent={} | 耗时={}ms",
                version.getVersionId(), version.getParentVersionId(), elapsed);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(buildReviseResponse(version)));
    }

    /**
     * GET /api/versions — 分页获取历史版本列表。
     */
    @Operation(summary = "获取版本列表", description = "分页获取所有历史版本摘要，按创建时间倒序排列。")
    @GetMapping("/versions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVersions(
            @Parameter(description = "页码，从 0 开始", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数，最大 100", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        log.info("查询版本列表 | page={} | size={}", page, size);

        // 限制每页最大条数
        if (size > 100) size = 100;
        if (size < 1) size = 10;

        Page<MusicVersion> pageResult = musicService.getVersionsPaged(page, size);
        List<Map<String, Object>> items = pageResult.getContent().stream()
                .map(this::buildVersionSummary)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", pageResult.getTotalElements());
        result.put("totalPages", pageResult.getTotalPages());
        result.put("page", page);
        result.put("size", size);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * GET /api/version/{version_id} — 获取某个版本的完整详情
     */
    @Operation(summary = "获取版本详情", description = "根据版本 ID 获取完整信息，包括 plan、caption、文件路径等。")
    @GetMapping("/version/{version_id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVersion(
            @Parameter(description = "版本 ID，如 v1", example = "v1")
            @PathVariable("version_id") String versionId) {

        log.info("查询版本详情 | version_id={}", versionId);

        return musicService.getVersion(versionId)
                .map(v -> ResponseEntity.ok(ApiResponse.success(buildVersionDetail(v))))
                .orElseThrow(() -> new NoSuchElementException("版本不存在: " + versionId));
    }

    // ===================== 响应构建辅助方法 =====================

    private Map<String, Object> buildGenerateResponse(MusicVersion v) throws JsonProcessingException {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("version_id", v.getVersionId());
        resp.put("caption", v.getCaption());
        resp.put("midi_url", "/outputs/" + v.getVersionId() + ".mid");
        resp.put("audio_url", "/outputs/" + v.getVersionId() + ".wav");
        resp.put("plan", objectMapper.readValue(v.getPlan(), Map.class));
        resp.put("mock", v.isMock());
        return resp;
    }

    private Map<String, Object> buildReviseResponse(MusicVersion v) throws JsonProcessingException {
        Map<String, Object> resp = buildGenerateResponse(v);
        resp.put("parent_version_id", v.getParentVersionId());
        resp.put("change_reason", v.getChangeReason());
        return resp;
    }

    private Map<String, Object> buildVersionSummary(MusicVersion v) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("version_id", v.getVersionId());
        summary.put("parent_version_id", v.getParentVersionId());
        summary.put("user_prompt", v.getUserPrompt());
        summary.put("style", v.getStyle());
        summary.put("mood", v.getMood());
        summary.put("tempo", v.getTempo());
        summary.put("created_at", v.getCreatedAt());
        summary.put("mock", v.isMock());
        if (v.getCaption() != null && v.getCaption().length() > 100) {
            summary.put("caption_preview", v.getCaption().substring(0, 100) + "...");
        } else {
            summary.put("caption_preview", v.getCaption());
        }
        return summary;
    }

    private Map<String, Object> buildVersionDetail(MusicVersion v) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("version_id", v.getVersionId());
        detail.put("parent_version_id", v.getParentVersionId());
        detail.put("user_prompt", v.getUserPrompt());
        detail.put("style", v.getStyle());
        detail.put("mood", v.getMood());
        detail.put("tempo", v.getTempo());
        try {
            detail.put("instruments", objectMapper.readValue(v.getInstruments(), List.class));
        } catch (JsonProcessingException e) {
            detail.put("instruments", v.getInstruments());
        }
        detail.put("feedback", v.getFeedback());
        detail.put("caption", v.getCaption());
        detail.put("midi_url", "/outputs/" + v.getVersionId() + ".mid");
        detail.put("audio_url", "/outputs/" + v.getVersionId() + ".wav");
        detail.put("change_reason", v.getChangeReason());
        detail.put("created_at", v.getCreatedAt());
        detail.put("mock", v.isMock());
        try {
            detail.put("plan", objectMapper.readValue(v.getPlan(), Map.class));
        } catch (JsonProcessingException e) {
            detail.put("plan", v.getPlan());
        }
        return detail;
    }
}
