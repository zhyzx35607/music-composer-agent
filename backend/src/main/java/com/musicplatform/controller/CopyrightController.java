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
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Value("${music-platform.pipeline.script-dir:../agent/gpt_music_pipeline}")
    private String scriptDir;

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

    /**
     * GET /api/copyright/evidence-package/{version_id} — 给版权存证系统的统一证据包。
     *
     * 该接口不改变原有数据库结构，只把分散在数据库和 outputs 目录中的证据材料聚合成中间交换 JSON。
     */
    @Operation(summary = "获取版权存证统一证据包", description = "按 version_id 聚合生成记录、修订记录、版本链、输出文件和 SHA-256 哈希。")
    @GetMapping("/evidence-package/{version_id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEvidencePackage(
            @PathVariable("version_id") String versionId,
            @RequestParam(value = "creator_name", required = false) String creatorName,
            @RequestParam(value = "creator_id", required = false) String creatorId,
            @RequestParam(value = "external_project_id", required = false) String externalProjectId,
            @RequestParam(value = "project_title", required = false) String projectTitle) throws JsonProcessingException {

        MusicVersion target = versionRepository.findById(versionId)
                .orElseThrow(() -> new NoSuchElementException("版本不存在: " + versionId));

        List<MusicVersion> chain = loadVersionChain(target);
        Map<String, Object> project = buildProjectInfo(target, creatorName, creatorId,
                externalProjectId, projectTitle);

        List<Map<String, Object>> versions = chain.stream()
                .map(this::buildVersionChainItem)
                .toList();

        List<Map<String, Object>> records = chain.stream()
                .map(v -> buildEvidenceRecord(v, project))
                .toList();

        List<Map<String, Object>> generatedFiles = chain.stream()
                .flatMap(v -> buildGeneratedFiles(v.getVersionId()).stream())
                .toList();

        Map<String, Object> hashes = new LinkedHashMap<>();
        for (Map<String, Object> file : generatedFiles) {
            Object filename = file.get("filename");
            Object sha = file.get("sha256");
            if (filename != null && sha != null) {
                hashes.put(filename.toString(), sha);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("package_type", "AI_MUSIC_COPYRIGHT_EVIDENCE_PACKAGE");
        result.put("package_version", "1.0");
        result.put("exported_at", java.time.LocalDateTime.now().toString());
        result.put("target_version_id", versionId);
        result.put("project", project);
        result.put("versions", versions);
        result.put("records", records);
        result.put("generated_files", generatedFiles);
        result.put("file_hashes_sha256", hashes);
        result.put("logs", buildLogInfo());
        result.put("minimum_package_ready", !records.isEmpty()
                && generatedFiles.stream().anyMatch(f -> "WAV".equals(f.get("file_type")))
                && generatedFiles.stream().anyMatch(f -> "MIDI".equals(f.get("file_type")))
                && generatedFiles.stream().anyMatch(f -> "MUSIC_JSON".equals(f.get("file_type"))));

        return ResponseEntity.ok(ApiResponse.success(result));
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

    private List<MusicVersion> loadVersionChain(MusicVersion target) {
        List<MusicVersion> reversed = new ArrayList<>();
        MusicVersion current = target;
        Set<String> seen = new HashSet<>();
        while (current != null && seen.add(current.getVersionId())) {
            reversed.add(current);
            String parentId = current.getParentVersionId();
            if (parentId == null || parentId.isBlank()) break;
            current = versionRepository.findById(parentId).orElse(null);
        }
        Collections.reverse(reversed);
        return reversed;
    }

    private Map<String, Object> buildProjectInfo(MusicVersion version, String creatorName,
                                                  String creatorId, String externalProjectId,
                                                  String projectTitle) {
        Map<String, Object> project = new LinkedHashMap<>();
        String resolvedTitle = firstNonBlank(projectTitle, version.getTrackName(),
                titleFromPlan(version.getPlan()), version.getVersionId());
        project.put("external_project_id", firstNonBlank(externalProjectId, version.getTrackId(),
                "music-project-" + version.getVersionId()));
        project.put("project_title", resolvedTitle);
        project.put("creator_id", firstNonBlank(creatorId, "user-001"));
        project.put("creator_name", firstNonBlank(creatorName, "未填写"));
        project.put("track_id", version.getTrackId());
        project.put("track_name", version.getTrackName());
        return project;
    }

    private Map<String, Object> buildVersionChainItem(MusicVersion version) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("version_id", version.getVersionId());
        item.put("parent_version_id", version.getParentVersionId());
        item.put("version_label", version.getVersionLabel());
        item.put("operation_type", operationType(version));
        item.put("created_at", version.getCreatedAt() != null ? version.getCreatedAt().toString() : null);
        item.put("track_id", version.getTrackId());
        item.put("version_number", version.getVersionNumber());
        return item;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildEvidenceRecord(MusicVersion version, Map<String, Object> project) {
        Map<String, Object> aiRecord = readJsonObject(outputPath(version.getVersionId() + ".ai_record.json"));
        Map<String, Object> promptRecord = readJsonObject(outputPath(version.getVersionId() + ".prompt.json"));
        Map<String, Object> musicJson = readJsonObject(outputPath(version.getVersionId() + ".json"));

        Map<String, Object> conversation = mapValue(aiRecord.get("conversation"));
        Map<String, Object> promptUi = mapValue(promptRecord.get("ui_parameters"));
        Map<String, Object> conversationUi = mapValue(conversation.get("ui_parameters"));

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("external_project_id", project.get("external_project_id"));
        record.put("project_title", project.get("project_title"));
        record.put("creator_id", project.get("creator_id"));
        record.put("creator_name", project.get("creator_name"));

        record.put("version_id", version.getVersionId());
        record.put("parent_version_id", version.getParentVersionId());
        record.put("version_label", version.getVersionLabel());
        record.put("operation_type", operationType(version));
        record.put("created_at", version.getCreatedAt() != null ? version.getCreatedAt().toString() : null);
        record.put("operator_id", project.get("creator_id"));
        record.put("operator_name", project.get("creator_name"));
        record.put("model", firstNonBlank(asString(aiRecord.get("model")), asString(promptRecord.get("model"))));

        record.put("user_original_request", firstNonBlank(asString(conversation.get("user_original_request")),
                asString(promptRecord.get("original_request")), version.getUserPrompt()));
        record.put("ui_parameters", !conversationUi.isEmpty() ? conversationUi : fallbackUiParameters(version, promptUi));
        record.put("system_message", firstNonBlank(asString(conversation.get("system_message")),
                asString(promptRecord.get("system_prompt"))));
        record.put("final_prompt_sent_to_ai", firstNonBlank(asString(conversation.get("final_prompt_sent_to_ai")),
                asString(promptRecord.get("user_prompt"))));

        record.put("ai_response_summary", version.getCaption());
        record.put("ai_response_music_json", !musicJson.isEmpty() ? musicJson : parseJsonMap(version.getPlan()));
        record.put("feedback", version.getFeedback());
        record.put("change_reason", firstNonBlank(version.getChangeReason(),
                asString(conversation.get("change_reason"))));
        record.put("parameter_diff", firstNonBlank(version.getParameterDiff(),
                asString(conversation.get("parameter_diff"))));
        record.put("generated_files", buildGeneratedFiles(version.getVersionId()));
        record.put("file_hashes_sha256", hashesByFilename(version.getVersionId()));
        record.put("operation_note", operationNote(version));
        return record;
    }

    private Map<String, Object> fallbackUiParameters(MusicVersion version, Map<String, Object> promptUi) {
        if (!promptUi.isEmpty()) return promptUi;
        Map<String, Object> ui = new LinkedHashMap<>();
        ui.put("style", version.getStyle());
        ui.put("mood", version.getMood());
        ui.put("tempo", version.getTempo());
        ui.put("instruments", parseJsonList(version.getInstruments()));
        return ui;
    }

    private List<Map<String, Object>> buildGeneratedFiles(String versionId) {
        List<String> names = List.of(
                versionId + ".wav",
                versionId + ".mid",
                versionId + ".json",
                versionId + ".prompt.txt",
                versionId + ".prompt.json",
                versionId + ".manifest.json",
                versionId + ".ai_record.json"
        );
        List<Map<String, Object>> files = new ArrayList<>();
        for (String name : names) {
            Path path = outputPath(name);
            if (!Files.exists(path)) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("filename", name);
            item.put("file_type", fileType(name));
            item.put("relative_path", "/outputs/" + name);
            item.put("absolute_path", path.toAbsolutePath().normalize().toString());
            item.put("size_bytes", fileSize(path));
            item.put("sha256", sha256File(path));
            files.add(item);
        }
        return files;
    }

    private Map<String, String> hashesByFilename(String versionId) {
        Map<String, String> hashes = new LinkedHashMap<>();
        for (Map<String, Object> file : buildGeneratedFiles(versionId)) {
            hashes.put(file.get("filename").toString(), file.get("sha256").toString());
        }
        return hashes;
    }

    private Map<String, Object> buildLogInfo() {
        Map<String, Object> logs = new LinkedHashMap<>();
        Path audit = Path.of("logs", "ai-request-audit.log").toAbsolutePath().normalize();
        logs.put("ai_request_audit_log", Files.exists(audit) ? audit.toString() : null);
        logs.put("note", "日志只作为辅助材料；结构化证据以 records 与 generated_files 为准。");
        return logs;
    }

    private Path outputPath(String filename) {
        return Path.of(scriptDir).toAbsolutePath().normalize().resolve("outputs").resolve(filename);
    }

    private String operationType(MusicVersion version) {
        return version.getParentVersionId() == null || version.getParentVersionId().isBlank()
                ? "GENERATE" : "REVISE";
    }

    private String operationNote(MusicVersion version) {
        if ("REVISE".equals(operationType(version))) {
            return "用户基于父版本提交反馈并生成修订版本。";
        }
        return "用户完成首次 AI 音乐生成。";
    }

    private String fileType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".wav")) return "WAV";
        if (lower.endsWith(".mid")) return "MIDI";
        if (lower.endsWith(".ai_record.json")) return "AI_RECORD_JSON";
        if (lower.endsWith(".prompt.json")) return "PROMPT_JSON";
        if (lower.endsWith(".prompt.txt")) return "PROMPT_TEXT";
        if (lower.endsWith(".manifest.json")) return "MANIFEST_JSON";
        if (lower.endsWith(".json")) return "MUSIC_JSON";
        return "FILE";
    }

    private long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0L;
        }
    }

    private String sha256File(Path path) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(path)) {
                byte[] buffer = new byte[1024 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    md.update(buffer, 0, read);
                }
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJsonObject(Path path) {
        if (!Files.exists(path)) return Collections.emptyMap();
        try {
            return objectMapper.readValue(Files.readString(path), Map.class);
        } catch (IOException e) {
            log.warn("无法读取证据 JSON | path={} | error={}", path, e.getMessage());
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> parseJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Collections.emptyMap();
    }

    private String titleFromPlan(String planJson) {
        Map<String, Object> plan = parseJsonMap(planJson);
        Object title = plan.get("title");
        if (title == null) title = plan.get("theme");
        return title == null ? null : title.toString();
    }

    private String asString(Object value) {
        if (value == null) return null;
        if (value instanceof String text) return text;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return value.toString();
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }
}
