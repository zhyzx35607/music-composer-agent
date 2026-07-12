package com.musicplatform.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicplatform.dto.ApiResponse;
import com.musicplatform.model.ComplianceRecord;
import com.musicplatform.repository.ComplianceRecordRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 合规检测 REST API — 检测 AI 生成音乐是否与已有版权作品相似。
 *
 * 接口：
 *   POST /api/compliance/check   — 执行合规检测
 *   GET  /api/compliance/history  — 获取检测历史
 */
@RestController
@RequestMapping("/api/compliance")
@CrossOrigin(origins = "*")
@Tag(name = "合规检测", description = "AI 音乐版权合规检测接口")
public class ComplianceController {

    private static final Logger log = LoggerFactory.getLogger(ComplianceController.class);

    private final ComplianceRecordRepository repository;
    private final ObjectMapper objectMapper;

    public ComplianceController(ComplianceRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * POST /api/compliance/check — 执行合规检测。
     * 当前为 Mock 实现，返回随机低风险结果。
     * TODO: 接入真实版权库检测服务。
     */
    @Operation(summary = "执行合规检测", description = "对指定版本的音乐进行版权合规检测，返回相似度评分和风险等级。")
    @PostMapping("/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> check(
            @RequestBody Map<String, String> request) throws JsonProcessingException {

        String versionId = request.getOrDefault("version_id", "unknown");
        String checkType = request.getOrDefault("check_type", "all");

        log.info("合规检测 | version_id={} | check_type={}", versionId, checkType);

        // ===== Mock 检测逻辑 =====
        // TODO: 接入真实版权库进行旋律/歌词/音色比对
        Map<String, Double> details = new LinkedHashMap<>();
        details.put("melody_similarity", Math.random() * 10);
        details.put("lyric_similarity", Math.random() * 5);
        details.put("timbre_similarity", Math.random() * 8);

        double overallScore = (details.get("melody_similarity")
                + details.get("lyric_similarity")
                + details.get("timbre_similarity")) / 3;

        String riskLevel;
        if (overallScore > 70) riskLevel = "high";
        else if (overallScore > 40) riskLevel = "medium";
        else riskLevel = "low";

        List<Map<String, Object>> matchedWorks = Collections.emptyList();

        // 保存检测记录
        ComplianceRecord record = new ComplianceRecord(
                versionId, checkType, riskLevel, overallScore,
                objectMapper.writeValueAsString(details),
                objectMapper.writeValueAsString(matchedWorks));
        repository.save(record);

        // 构建响应
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overall_score", Math.round(overallScore * 10) / 10.0);
        result.put("details", details);
        result.put("matched_works", matchedWorks);
        result.put("risk_level", riskLevel);
        result.put("checked_at", record.getCheckedAt().toString());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(result));
    }

    /**
     * GET /api/compliance/history — 获取检测历史列表。
     */
    @Operation(summary = "获取检测历史", description = "按时间倒序返回所有合规检测记录。")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHistory() {
        List<ComplianceRecord> records = repository.findAllByOrderByCheckedAtDesc();

        List<Map<String, Object>> items = records.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId().toString());
            item.put("version_id", r.getVersionId());
            item.put("check_type", r.getCheckType());
            item.put("risk_level", r.getRiskLevel());
            item.put("overall_score", r.getOverallScore());
            item.put("checked_at", r.getCheckedAt().toString());
            return item;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", items.size());
        result.put("totalPages", 1);
        result.put("page", 0);
        result.put("size", items.size());

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
