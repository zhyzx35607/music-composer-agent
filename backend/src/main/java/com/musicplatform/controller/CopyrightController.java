package com.musicplatform.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicplatform.dto.ApiResponse;
import com.musicplatform.model.CopyrightRecord;
import com.musicplatform.model.MusicVersion;
import com.musicplatform.repository.CopyrightRecordRepository;
import com.musicplatform.repository.MusicVersionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 版权存证 REST API — 记录创作全流程，为版权归属提供证据链。
 *
 * 接口：
 *   POST /api/copyright/register      — 提交版权存证
 *   GET  /api/copyright/records       — 获取存证记录列表
 *   GET  /api/copyright/record/{id}   — 获取存证记录详情
 */
@RestController
@RequestMapping("/api/copyright")
@CrossOrigin(origins = "*")
@Tag(name = "版权存证", description = "AI 音乐版权存证接口")
public class CopyrightController {

    private static final Logger log = LoggerFactory.getLogger(CopyrightController.class);

    private final CopyrightRecordRepository repository;
    private final MusicVersionRepository versionRepository;
    private final ObjectMapper objectMapper;

    public CopyrightController(CopyrightRecordRepository repository,
                               MusicVersionRepository versionRepository,
                               ObjectMapper objectMapper) {
        this.repository = repository;
        this.versionRepository = versionRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * POST /api/copyright/register — 提交版权存证申请。
     * 当前为 Mock 实现，生成模拟哈希和区块高度。
     * TODO: 接入真实区块链存证服务。
     */
    @Operation(summary = "提交版权存证", description = "对指定版本的创作过程进行版权存证，记录提示词交互和修改历史。")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @RequestBody Map<String, String> request) throws JsonProcessingException {

        String versionId = request.getOrDefault("version_id", "");
        String creatorName = request.getOrDefault("creator_name", "");

        if (versionId.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("version_id 不能为空"));
        }
        if (creatorName.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("creator_name 不能为空"));
        }

        log.info("版权存证 | version_id={} | creator={}", versionId, creatorName);

        // 获取版本信息
        Optional<MusicVersion> versionOpt = versionRepository.findById(versionId);

        // 构建提示词交互历史和修改历史
        List<String> promptHistory = new ArrayList<>();
        List<String> revisionHistory = new ArrayList<>();

        // 追溯父版本链，收集所有提示词和反馈
        MusicVersion current = versionOpt.orElse(null);
        while (current != null) {
            promptHistory.add(current.getUserPrompt());
            if (current.getFeedback() != null && !current.getFeedback().isEmpty()) {
                revisionHistory.add(current.getFeedback());
            }
            String parentId = current.getParentVersionId();
            if (parentId != null && !parentId.isEmpty()) {
                current = versionRepository.findById(parentId).orElse(null);
            } else {
                break;
            }
        }

        // 生成记录 ID
        String recordId = "cr-" + System.currentTimeMillis();

        // 生成模拟证书哈希
        String certHash = generateMockHash(versionId + creatorName + recordId);

        // Mock 区块高度
        long blockHeight = System.currentTimeMillis() % 1000000;

        CopyrightRecord record = new CopyrightRecord(
                recordId, versionId, creatorName, certHash, blockHeight,
                objectMapper.writeValueAsString(promptHistory),
                objectMapper.writeValueAsString(revisionHistory),
                "pending");
        repository.save(record);

        // 构建响应
        Map<String, Object> result = buildRecordResponse(record, promptHistory, revisionHistory);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(result));
    }

    /**
     * GET /api/copyright/records — 获取所有存证记录列表。
     */
    @Operation(summary = "获取存证记录列表", description = "按时间倒序返回所有版权存证记录。")
    @GetMapping("/records")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecords() throws JsonProcessingException {
        List<CopyrightRecord> records = repository.findAllByOrderByCreatedAtDesc();

        List<Map<String, Object>> items = records.stream().map(r -> {
            try {
                List<String> prompts = parseStringList(r.getPromptHistory());
                List<String> revisions = parseStringList(r.getRevisionHistory());
                return buildRecordResponse(r, prompts, revisions);
            } catch (JsonProcessingException e) {
                log.warn("解析存证记录 JSON 失败 | record_id={}", r.getRecordId());
                return buildRecordResponseFallback(r);
            }
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", items.size());
        result.put("totalPages", 1);
        result.put("page", 0);
        result.put("size", items.size());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * GET /api/copyright/record/{record_id} — 获取单条存证记录详情。
     */
    @Operation(summary = "获取存证记录详情", description = "根据记录 ID 获取版权存证完整信息。")
    @GetMapping("/record/{record_id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecord(
            @PathVariable("record_id") String recordId) throws JsonProcessingException {

        return repository.findById(recordId)
                .map(r -> {
                    try {
                        List<String> prompts = parseStringList(r.getPromptHistory());
                        List<String> revisions = parseStringList(r.getRevisionHistory());
                        return ResponseEntity.ok(ApiResponse.success(buildRecordResponse(r, prompts, revisions)));
                    } catch (JsonProcessingException e) {
                        return ResponseEntity.ok(ApiResponse.success(buildRecordResponseFallback(r)));
                    }
                })
                .orElseThrow(() -> new NoSuchElementException("存证记录不存在: " + recordId));
    }

    // ===================== 辅助方法 =====================

    private Map<String, Object> buildRecordResponse(CopyrightRecord r,
                                                     List<String> prompts,
                                                     List<String> revisions) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("record_id", r.getRecordId());
        resp.put("version_id", r.getVersionId());
        resp.put("creator_name", r.getCreatorName());
        resp.put("certificate_hash", r.getCertificateHash());
        resp.put("block_height", r.getBlockHeight());
        resp.put("created_at", r.getCreatedAt().toString());
        resp.put("prompt_history", prompts);
        resp.put("revision_history", revisions);
        resp.put("status", r.getStatus());
        return resp;
    }

    private Map<String, Object> buildRecordResponseFallback(CopyrightRecord r) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("record_id", r.getRecordId());
        resp.put("version_id", r.getVersionId());
        resp.put("creator_name", r.getCreatorName());
        resp.put("certificate_hash", r.getCertificateHash());
        resp.put("block_height", r.getBlockHeight());
        resp.put("created_at", r.getCreatedAt().toString());
        resp.put("prompt_history", Collections.emptyList());
        resp.put("revision_history", Collections.emptyList());
        resp.put("status", r.getStatus());
        return resp;
    }

    private List<String> parseStringList(String json) throws JsonProcessingException {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        return objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }

    private String generateMockHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder("0x");
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "0x" + input.hashCode();
        }
    }
}
