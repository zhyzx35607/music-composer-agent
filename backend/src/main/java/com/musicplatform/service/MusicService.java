package com.musicplatform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicplatform.model.MusicVersion;
import com.musicplatform.model.UploadedFile;
import com.musicplatform.repository.MusicVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 音乐生成核心业务逻辑。
 * 调用智能体管线（GPT → music_json → MIDI → WAV）实现真实音乐生成。
 */
@Service
public class MusicService {

    private static final Logger log = LoggerFactory.getLogger(MusicService.class);

    private final MusicVersionRepository repository;
    private final ObjectMapper objectMapper;
    private final FileService fileService;

    /** 输出目录路径 */
    @Value("${music-platform.output-dir:./outputs}")
    private String outputDir;

    /** Python 解释器路径 */
    @Value("${music-platform.pipeline.python}")
    private String pythonPath;

    /** 管线脚本目录 */
    @Value("${music-platform.pipeline.script-dir:../agent/gpt_music_pipeline}")
    private String scriptDir;

    /** GPT 模型名 */
    @Value("${music-platform.pipeline.model:gpt-5.5}")
    private String model;

    /** API 超时（秒） */
    @Value("${music-platform.pipeline.timeout:240}")
    private int timeout;

    /** FluidSynth 路径（可选，为空则用管线默认值） */
    @Value("${music-platform.pipeline.fluidsynth-path:}")
    private String fluidsynthPath;

    /** SoundFont 路径（可选，为空则用管线默认值） */
    @Value("${music-platform.pipeline.soundfont-path:}")
    private String soundfontPath;

    /** GPT API Key（优先环境变量，未设置时使用配置文件值） */
    @Value("${music-platform.pipeline.api-key:}")
    private String configApiKey;

    /** GPT API Base URL */
    @Value("${music-platform.pipeline.api-base-url:}")
    private String configApiBaseUrl;

    public MusicService(MusicVersionRepository repository, ObjectMapper objectMapper,
                        FileService fileService) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.fileService = fileService;
    }

    /**
     * 生成下一个版本 ID（线程安全，数据库级别取最大序号）。
     */
    private synchronized String nextVersionId() {
        int maxNum = repository.findMaxGlobalVersionNumber();
        return "v" + (maxNum + 1);
    }

    /**
     * 首次生成音乐 — 支持 Track 模型，尝试调用智能体管线，失败时降级为 Mock。
     */
    @Transactional
    public MusicVersion generate(String userPrompt, String style, String mood,
                                  String tempo, String instrumentsJson,
                                  String trackName, String trackId, String userVersionLabel,
                                  Integer requestDurationSeconds,
                                  List<Long> referenceFileIds) {
        String versionId = nextVersionId();

        // ── Track 处理 ──
        String resolvedTrackId;
        int versionNumber;
        String resolvedTrackName;
        if (trackId != null && !trackId.isEmpty() && repository.existsByTrackId(trackId)) {
            // 已有 track：追加版本
            resolvedTrackId = trackId;
            versionNumber = repository.findMaxVersionNumberInTrack(trackId) + 1;
            // track_name：用户传了用新的，没传保持原 track 的名字
            if (trackName != null && !trackName.trim().isEmpty()) {
                resolvedTrackName = trackName.trim();
            } else {
                // 从该 track 的最新版本中取 track_name
                resolvedTrackName = repository.findByTrackIdOrderByVersionNumberDesc(trackId)
                        .stream().findFirst()
                        .map(MusicVersion::getTrackName)
                        .orElse("未命名音乐");
            }
        } else {
            // 新建 track
            resolvedTrackId = UUID.randomUUID().toString();
            versionNumber = 1;
            resolvedTrackName = (trackName != null && !trackName.trim().isEmpty())
                    ? trackName.trim() : "未命名音乐";
        }

        log.info("创建新版本 | track_id={} | track_name={} | v#={} | version_id={} | style={} | mood={}",
                resolvedTrackId, resolvedTrackName, versionNumber, versionId, style, mood);

        // 创建版本记录
        MusicVersion version = new MusicVersion(versionId, userPrompt,
                style, mood, tempo, instrumentsJson);
        version.setTrackId(resolvedTrackId);
        version.setTrackName(resolvedTrackName);
        version.setVersionNumber(versionNumber);
        version.setCreatedAt(LocalDateTime.now());

        // 尝试调用智能体管线
        if (isPipelineAvailable()) {
            try {
                int durationSeconds = resolveDurationSeconds(userPrompt, requestDurationSeconds);
                int tempoBpm = resolveTempoBpm(userPrompt, tempo);
                String instrumentsList = convertInstrumentsJsonToList(instrumentsJson);
                fileService.attachFiles(referenceFileIds, versionId, resolvedTrackId);
                Map<String, Object> manifest;
                Optional<UploadedFile> directScoreFile = fileService.findFirstScoreFile(
                        resolvedTrackId, versionId, referenceFileIds);
                if (directScoreFile.isPresent() && isDirectScoreConversionRequest(userPrompt)) {
                    log.info("Direct score conversion selected | version_id={} | file={}",
                            versionId, directScoreFile.get().getStoredPath());
                    manifest = runPipeline(userPrompt, durationSeconds, tempoBpm,
                            style, mood, instrumentsList, versionId, "generate",
                            directScoreFile.get().getStoredPath());
                } else {
                    String enrichedPrompt = userPrompt
                            + fileService.buildReferenceFilesSection(resolvedTrackId, versionId, referenceFileIds);
                    manifest = runPipeline(enrichedPrompt, durationSeconds, tempoBpm,
                            style, mood, instrumentsList, versionId, "generate", null);
                }
                populateVersionFromManifest(version, manifest, versionId);
                version.setMock(false);
                version.setVersionLabel(resolveVersionLabel(userVersionLabel, true, null,
                        parseJsonToMap(version.getPlan())));
                log.info("管线生成完成 | track_id={} | version_id={}", resolvedTrackId, versionId);
            } catch (Exception e) {
                log.warn("管线调用失败，降级为 Mock | version_id={} | error={}", versionId, e.getMessage());
                populateVersionMock(version, style, mood, tempo, instrumentsJson);
                version.setMock(true);
                version.setVersionLabel(resolveVersionLabel(userVersionLabel, true, null, null));
            }
        } else {
            log.info("管线不可用，使用 Mock 数据 | version_id={}", versionId);
            populateVersionMock(version, style, mood, tempo, instrumentsJson);
            version.setMock(true);
            version.setVersionLabel(resolveVersionLabel(userVersionLabel, true, null, null));
        }

        MusicVersion saved = repository.save(version);
        log.info("版本已保存 | track_id={} | version_id={} | v#={} | mock={}",
                resolvedTrackId, saved.getVersionId(), saved.getVersionNumber(), saved.isMock());
        return saved;
    }

    /**
     * 反馈修改 — 传入旧 plan 让 AI 做对比式修改，失败时降级为 Mock。
     */
    @Transactional
    public MusicVersion revise(String parentVersionId, String feedback, String userVersionLabel,
                               List<Long> referenceFileIds) {
        log.info("反馈修改 | parent={} | feedback={}", parentVersionId, feedback);

        MusicVersion parent = repository.findById(parentVersionId)
                .orElseThrow(() -> new NoSuchElementException("版本不存在: " + parentVersionId));

        // 兼容 old data：如果父版本没有 track_id，则创建新 track
        String resolvedTrackId;
        String resolvedTrackName;
        int versionNumber;
        if (parent.getTrackId() == null || parent.getTrackId().isEmpty()) {
            resolvedTrackId = UUID.randomUUID().toString();
            resolvedTrackName = "未命名音乐";
            versionNumber = 1;
            log.info("父版本 {} 无 track_id（旧数据），创建新 track | track_id={}",
                    parentVersionId, resolvedTrackId);
        } else {
            resolvedTrackId = parent.getTrackId();
            resolvedTrackName = parent.getTrackName();
            versionNumber = repository.findMaxVersionNumberInTrack(resolvedTrackId) + 1;
        }

        String versionId = nextVersionId();

        log.info("基于 {} 创建新版本 | track_id={} | v#={} | version_id={}",
                parentVersionId, resolvedTrackId, versionNumber, versionId);

        // 创建新版本（继承父版本的 Track 和参数）
        MusicVersion version = new MusicVersion(versionId,
                parent.getUserPrompt(),
                parent.getStyle(), parent.getMood(),
                parent.getTempo(), parent.getInstruments());
        version.setTrackId(resolvedTrackId);
        version.setTrackName(resolvedTrackName);
        version.setVersionNumber(versionNumber);
        version.setParentVersionId(parentVersionId);
        version.setFeedback(feedback);
        version.setCreatedAt(LocalDateTime.now());

        // 尝试调用智能体管线（传入旧 plan）
        if (isPipelineAvailable()) {
            try {
                fileService.attachFiles(referenceFileIds, versionId, resolvedTrackId);
                String parentMusicJson = loadMusicJsonText(parentVersionId);
                String scoreReferenceSection = fileService.buildReferenceFilesSection(
                        resolvedTrackId, parentVersionId, referenceFileIds);
                String revisedPrompt = buildRevisePrompt(parent, feedback, parentMusicJson, scoreReferenceSection);
                int durationSeconds = resolveDurationSeconds(parent.getUserPrompt(), durationFromPlan(parent));
                int tempoBpm = resolveTempoBpm(feedback + "\n" + parent.getUserPrompt(), parent.getTempo());
                String instrumentsList = convertInstrumentsJsonToList(parent.getInstruments());
                Map<String, Object> manifest = runPipeline(revisedPrompt, durationSeconds, tempoBpm,
                        parent.getStyle(), parent.getMood(), instrumentsList, versionId, "revise", null);
                populateVersionFromManifest(version, manifest, versionId);
                version.setMock(false);

                // 从 manifest 提取 AI 生成的 change_reason
                version.setChangeReason(extractChangeReason(manifest, feedback));

                // 从 manifest 提取或自行计算 parameter_diff
                version.setParameterDiff(extractOrComputeDiff(manifest, parent, version));

                // version_label
                version.setVersionLabel(resolveVersionLabel(userVersionLabel, false, feedback,
                        parseJsonToMap(version.getPlan())));

                log.info("回炉管线完成 | version_id={} | change_reason={}",
                        versionId, version.getChangeReason());
            } catch (Exception e) {
                log.warn("管线调用失败（revise），降级为 Mock | version_id={} | error={}",
                        versionId, e.getMessage());
                populateVersionMock(version, parent.getStyle(), parent.getMood(),
                        parent.getTempo(), parent.getInstruments());
                version.setMock(true);
                version.setChangeReason("管线不可用。根据用户反馈「" + feedback + "」调整了音乐参数。");
                try {
                    version.setParameterDiff(objectMapper.writeValueAsString(
                            computeDiff(parent, version)));
                } catch (JsonProcessingException ex) {
                    version.setParameterDiff(null);
                }
                version.setVersionLabel(resolveVersionLabel(userVersionLabel, false, feedback, null));
            }
        } else {
            log.info("管线不可用，使用 Mock 数据（revise）| version_id={}", versionId);
            populateVersionMock(version, parent.getStyle(), parent.getMood(),
                    parent.getTempo(), parent.getInstruments());
            version.setMock(true);
            version.setChangeReason("管线不可用。根据用户反馈「" + feedback + "」调整了音乐参数。");
            try {
                version.setParameterDiff(objectMapper.writeValueAsString(
                        computeDiff(parent, version)));
            } catch (JsonProcessingException e) {
                version.setParameterDiff(null);
            }
            version.setVersionLabel(resolveVersionLabel(userVersionLabel, false, feedback, null));
        }

        MusicVersion saved = repository.save(version);
        log.info("修改版本已保存 | track_id={} | version_id={} | parent={} | v#={}",
                saved.getTrackId(), saved.getVersionId(), saved.getParentVersionId(),
                saved.getVersionNumber());
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

    /** 获取所有 Track 的最新版本（用于 Track 列表） */
    public List<MusicVersion> getLatestPerTrack() {
        return repository.findLatestPerTrack();
    }

    /** 获取某 Track 的所有版本（按版本号倒序） */
    public List<MusicVersion> getTrackVersions(String trackId) {
        return repository.findByTrackIdOrderByVersionNumberDesc(trackId);
    }

    // ===================== 管线调用 =====================

    /**
     * 调用 run_music_pipeline.py，解析返回的 manifest JSON。
     */
    private Map<String, Object> runPipeline(String request, int duration, int tempoBpm,
                                             String style, String mood,
                                             String instruments,
                                             String outputName,
                                             String mode,
                                             String inputScorePath)
            throws IOException, InterruptedException {

        Path scriptDirPath = Path.of(scriptDir).toAbsolutePath().normalize();
        Path pipelineScript = scriptDirPath.resolve("run_music_pipeline.py");

        if (!pipelineScript.toFile().exists()) {
            throw new IOException("管线脚本不存在: " + pipelineScript);
        }

        // 构建命令
        List<String> command = new ArrayList<>();
        command.add(pythonPath);
        command.add(pipelineScript.toString());
        Path requestFile = null;
        if ("revise".equalsIgnoreCase(mode) || request.length() > 3000) {
            Path requestDir = scriptDirPath.resolve("outputs").resolve("_requests");
            Files.createDirectories(requestDir);
            requestFile = requestDir.resolve(outputName + ".request.txt");
            Files.writeString(requestFile, request, StandardCharsets.UTF_8);
            log.info("Pipeline request written to file | version_id={} | mode={} | chars={} | path={}",
                    outputName, mode, request.length(), requestFile);
            command.add("--request-file");
            command.add(requestFile.toString());
        } else {
            command.add("--request");
            command.add(request);
        }
        command.add("--duration");
        command.add(String.valueOf(duration));
        command.add("--output-name");
        command.add(outputName);
        command.add("--model");
        command.add(model);
        command.add("--timeout");
        command.add(String.valueOf(timeout));
        if (inputScorePath != null && !inputScorePath.isBlank()) {
            command.add("--input-score");
            command.add(inputScorePath);
        }

        if (style != null && !style.isEmpty()) {
            command.add("--style");
            command.add(style);
        }
        if (mood != null && !mood.isEmpty()) {
            command.add("--mood");
            command.add(mood);
        }
        if (instruments != null && !instruments.isEmpty()) {
            command.add("--instruments");
            command.add(instruments);
        }
        command.add("--tempo-bpm");
        command.add(String.valueOf(tempoBpm));
        if (mode != null && !mode.isEmpty()) {
            command.add("--mode");
            command.add(mode);
        }
        if (fluidsynthPath != null && !fluidsynthPath.isEmpty()) {
            command.add("--fluidsynth");
            command.add(fluidsynthPath);
        }
        if (soundfontPath != null && !soundfontPath.isEmpty()) {
            command.add("--soundfont");
            command.add(soundfontPath);
        }

        // 屏蔽 API Key 日志
        List<String> safeCmd = new ArrayList<>(command);
        log.info("执行管线 | command={}", String.join(" ", safeCmd));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(scriptDirPath.toFile());
        pb.redirectErrorStream(false);

        // 传入 API Key（环境变量优先，配置文件兜底）
        String resolvedKey = System.getenv("MUSIC_API_KEY");
        if (resolvedKey == null || resolvedKey.isEmpty()) {
            resolvedKey = System.getenv("OPENAI_API_KEY");
        }
        if (resolvedKey == null || resolvedKey.isEmpty()) {
            resolvedKey = configApiKey;
        }
        String resolvedUrl = System.getenv("MUSIC_API_BASE_URL");
        if (resolvedUrl == null || resolvedUrl.isEmpty()) {
            resolvedUrl = configApiBaseUrl;
        }
        if (resolvedKey != null && !resolvedKey.isEmpty()) {
            pb.environment().put("MUSIC_API_KEY", resolvedKey);
        }
        if (resolvedUrl != null && !resolvedUrl.isEmpty()) {
            pb.environment().put("MUSIC_API_BASE_URL", resolvedUrl);
        }

        Process process = pb.start();
        CompletableFuture<StringBuilder> stdoutFuture =
                CompletableFuture.supplyAsync(() -> readProcessStream(process, true));
        CompletableFuture<StringBuilder> stderrFuture =
                CompletableFuture.supplyAsync(() -> readProcessStream(process, false));

        // 读取 stdout（manifest JSON）
        StringBuilder stdout = new StringBuilder();

        // 读取 stderr（日志/错误）
        StringBuilder stderr = new StringBuilder();

        boolean finished = process.waitFor(timeout + 30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("管线超时（" + (timeout + 30) + "秒）");
        }

        stdout = stdoutFuture.join();
        stderr = stderrFuture.join();

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.error("管线执行失败 | exitCode={} | stderr={}", exitCode, stderr);
            throw new IOException("管线执行失败 (exit " + exitCode + "): " + stderr);
        }

        // 解析 manifest JSON
        String manifestStr = stdout.toString().trim();
        // 从 stdout 中提取最后一个完整 JSON 对象
        int firstBrace = manifestStr.indexOf('{');
        if (firstBrace >= 0) {
            manifestStr = manifestStr.substring(firstBrace);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> manifest = objectMapper.readValue(manifestStr, Map.class);
            log.info("管线完成 | version_id={} | title={}",
                    manifest.get("version_id"), manifest.get("title"));
            return manifest;
        } catch (JsonProcessingException e) {
            log.error("解析 manifest 失败 | stdout={}", stdout);
            throw new IOException("无法解析管线输出: " + e.getMessage(), e);
        }
    }

    // ===================== 数据填充 =====================

    /**
     * 从管线 manifest 填充 MusicVersion 的 plan / caption / 文件路径。
     */
    @SuppressWarnings("unchecked")
    private StringBuilder readProcessStream(Process process, boolean stdout) {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stdout ? process.getInputStream() : process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        } catch (IOException e) {
            output.append("[stream read failed] ").append(e.getMessage());
        }
        return output;
    }

    private void populateVersionFromManifest(MusicVersion version,
                                              Map<String, Object> manifest,
                                              String versionId) {
        // 读取生成的 music_json 构建完整的 plan
        Map<String, Object> plan = new LinkedHashMap<>();
        String musicJsonPath = scriptDir + "/outputs/" + versionId + ".json";

        try {
            Map<String, Object> musicJson = objectMapper.readValue(
                    new File(musicJsonPath), Map.class);

            plan.put("theme", musicJson.getOrDefault("title", "Untitled"));
            plan.put("style", musicJson.getOrDefault("style", ""));
            plan.put("mood", musicJson.getOrDefault("mood", Collections.emptyList()));
            plan.put("duration_seconds", musicJson.getOrDefault("duration_seconds", manifest.getOrDefault("duration_seconds", 30)));
            plan.put("tempo", musicJson.getOrDefault("tempo_bpm", 88));
            plan.put("key", musicJson.getOrDefault("key", "C major"));
            plan.put("description", musicJson.getOrDefault("description", ""));

            // 从 tracks 提取乐器
            Object tracksObj = musicJson.get("tracks");
            if (tracksObj instanceof List) {
                List<String> instruments = ((List<Map<String, Object>>) tracksObj).stream()
                        .map(t -> String.valueOf(t.getOrDefault("instrument", "")))
                        .filter(s -> !s.isEmpty())
                        .distinct()
                        .collect(Collectors.toList());
                plan.put("instruments", instruments);
            } else {
                plan.put("instruments", Collections.emptyList());
            }

            // chord_progression → structure
            Object chords = musicJson.get("chord_progression");
            if (chords instanceof List) {
                plan.put("structure", chords);
            } else {
                plan.put("structure", Arrays.asList("intro", "verse", "chorus", "outro"));
            }

        } catch (IOException e) {
            log.warn("无法读取 music_json，使用 manifest 默认值 | path={}", musicJsonPath);
            plan.put("theme", manifest.getOrDefault("title", "Untitled"));
            plan.put("style", manifest.getOrDefault("style", ""));
            plan.put("mood", Collections.emptyList());
            plan.put("duration_seconds", manifest.getOrDefault("duration_seconds", 30));
            plan.put("tempo", manifest.getOrDefault("tempo_bpm", 88));
            plan.put("key", "C major");
            plan.put("instruments", Collections.emptyList());
            plan.put("structure", Arrays.asList("intro", "verse", "chorus", "outro"));
        }

        try {
            version.setPlan(objectMapper.writeValueAsString(plan));
        } catch (JsonProcessingException e) {
            version.setPlan("{}");
        }

        // caption = music_json.description（英文描述，Text2MIDI 入口）
        String description = String.valueOf(
                plan.getOrDefault("description", "A beautiful instrumental piece."));
        version.setCaption(description);

        // 文件路径（管线使用扁平结构：outputs/{versionId}.ext）
        version.setMidiPath(outputDir + "/" + versionId + ".mid");
        version.setAudioPath(outputDir + "/" + versionId + ".wav");
    }

    /**
     * 构建回炉 prompt —— 传入父版本的完整 plan 参数，让 GPT 做对比式修改。
     */
    private String loadMusicJsonText(String versionId) {
        Path musicJsonPath = Path.of(scriptDir).toAbsolutePath().normalize()
                .resolve("outputs")
                .resolve(versionId + ".json");
        try {
            if (Files.exists(musicJsonPath)) {
                String musicJson = Files.readString(musicJsonPath, StandardCharsets.UTF_8);
                log.info("Loaded parent music_json for revision | version_id={} | chars={} | path={}",
                        versionId, musicJson.length(), musicJsonPath);
                return musicJson;
            }
            log.warn("Parent music_json not found for revision | version_id={} | path={}",
                    versionId, musicJsonPath);
        } catch (IOException e) {
            log.warn("Failed to read parent music_json for revision | version_id={} | path={} | error={}",
                    versionId, musicJsonPath, e.getMessage());
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private String buildRevisePrompt(MusicVersion parent, String feedback, String parentMusicJson,
                                     String scoreReferenceSection) {
        Map<String, Object> oldPlan = parseJsonToMap(parent.getPlan());

        StringBuilder sb = new StringBuilder();
        sb.append("You are revising an existing AI-generated instrumental score.\n");
        sb.append("This is an edit task, not a fresh composition task.\n\n");

        sb.append("=== ORIGINAL USER REQUEST ===\n");
        sb.append(parent.getUserPrompt()).append("\n\n");

        sb.append("=== CURRENT VERSION SUMMARY ===\n");
        sb.append("Style: ").append(oldPlan != null ? oldPlan.get("style") : parent.getStyle()).append("\n");
        sb.append("Mood: ").append(oldPlan != null ? oldPlan.get("mood") : parent.getMood()).append("\n");
        sb.append("Tempo (BPM): ").append(oldPlan != null ? oldPlan.get("tempo") : "88").append("\n");
        sb.append("Key: ").append(oldPlan != null ? oldPlan.get("key") : "C major").append("\n");
        sb.append("Instruments: ").append(oldPlan != null ? oldPlan.get("instruments") : "").append("\n");
        sb.append("Structure: ").append(oldPlan != null ? oldPlan.get("structure") : "").append("\n");
        sb.append("Current caption: ").append(parent.getCaption()).append("\n\n");

        sb.append("=== CURRENT COMPLETE MUSIC_JSON TO REVISE ===\n");
        if (parentMusicJson != null && !parentMusicJson.isBlank()) {
            sb.append(parentMusicJson).append("\n\n");
        } else {
            sb.append("{}\n");
            sb.append("The complete previous music_json file was not available. ");
            sb.append("Use the summary above as fallback, but keep the revision as close as possible to the current version.\n\n");
        }

        if (scoreReferenceSection != null && !scoreReferenceSection.isBlank()) {
            sb.append(scoreReferenceSection).append("\n");
            sb.append("If MusicXML score input is present, use it as the primary score evidence. ");
            sb.append("The revised music_json must be based on that score data plus the current music_json, not a fresh unrelated composition.\n\n");
        }

        sb.append("=== USER FEEDBACK ===\n");
        sb.append(feedback).append("\n\n");

        sb.append("=== YOUR TASK ===\n");
        sb.append("1. Revise the music_json based on the feedback. ");
        sb.append("Make TARGETED changes to specific parameters — do NOT regenerate from scratch.\n");
        sb.append("2. Also output a 'change_reason' field (natural language, in Chinese) ");
        sb.append("explaining what you changed and why.\n");
        sb.append("3. Also output a 'parameter_diff' field showing before/after differences.\n");

        sb.append("\nOutput format (JSON):\n{\n");
        sb.append("  \"music_json\": { \"version\": \"1.0\", \"title\": \"...\", \"description\": \"...\", \"duration_seconds\": 30, \"tempo_bpm\": 120, \"key\": \"...\", \"time_signature\": \"4/4\", \"ticks_per_beat\": 480, \"total_beats\": 60, \"style\": \"...\", \"mood\": [\"...\"], \"chord_progression\": [\"...\"], \"tracks\": [] },\n");
        sb.append("  \"change_reason\": \"中文说明：具体改了哪些地方，为什么这样改\",\n");
        sb.append("  \"parameter_diff\": {\n");
        sb.append("    \"mood\": {\"from\": [...], \"to\": [...]},\n");
        sb.append("    \"tempo\": {\"from\": N, \"to\": N},\n");
        sb.append("    \"key\": {\"from\": \"...\", \"to\": \"...\"},\n");
        sb.append("    \"instruments\": {\"add\": [...], \"remove\": [...]}\n");
        sb.append("  }\n");
        sb.append("}\n");

        sb.append("\nStrict revision rules:\n");
        sb.append("- Start from CURRENT COMPLETE MUSIC_JSON and return a complete revised music_json, not a patch.\n");
        sb.append("- Preserve duration_seconds, time_signature, ticks_per_beat, track/channel/program identity, and most notes unless feedback requires changing them.\n");
        sb.append("- Make targeted musical edits to notes, velocity, rhythm, register, harmony, instrument density, or section energy according to feedback.\n");
        sb.append("- Keep the main motif and structure recognizable unless the feedback asks for a large rewrite.\n");
        sb.append("- The response must be valid JSON only. Do not include Markdown or comments.\n");

        return sb.toString();
    }

    /**
     * 将 JSON 数组形式的乐器列表转为逗号分隔字符串（供管线使用）。
     */
    private String convertInstrumentsJsonToList(String instrumentsJson) {
        if (instrumentsJson == null || instrumentsJson.isEmpty()) return "";
        try {
            @SuppressWarnings("unchecked")
            List<String> instruments = objectMapper.readValue(instrumentsJson, List.class);
            return String.join(",", instruments);
        } catch (JsonProcessingException e) {
            log.warn("无法解析 instruments JSON: {}", instrumentsJson);
            return "";
        }
    }

    /**
     * 解析目标时长（秒）。
     * 优先级：接口传参 > 用户文本解析（"60秒"/"1分钟"/"30s"）> 默认 30 秒。
     */
    private int resolveDurationSeconds(String userPrompt, Integer requestDuration) {
        if (requestDuration != null && requestDuration > 0) {
            return requestDuration;
        }
        int parsed = parseDurationFromText(userPrompt);
        return parsed > 0 ? parsed : 30;
    }

    /** 从文本中提取时长：支持"xx秒"、"xx分钟"、"xx分"、"xxs"、"xxm" */
    private int parseDurationFromText(String text) {
        if (text == null) return -1;
        int compatibleParsed = parseDurationFromTextCompat(text);
        if (compatibleParsed > 0) return compatibleParsed;
        try {
            var m = java.util.regex.Pattern.compile("(\\d+)\\s*(?:秒|s|sec)").matcher(text.toLowerCase());
            if (m.find()) return Integer.parseInt(m.group(1));

            m = java.util.regex.Pattern.compile("(\\d+)\\s*分(?:钟)?(?:\\s*(\\d+)\\s*秒(?:钟)?)?").matcher(text);
            if (m.find()) {
                int total = Integer.parseInt(m.group(1)) * 60;
                if (m.group(2) != null) total += Integer.parseInt(m.group(2));
                return total;
            }

            m = java.util.regex.Pattern.compile("(\\d+)\\s*m(?:in)?").matcher(text.toLowerCase());
            if (m.find()) return Integer.parseInt(m.group(1)) * 60;
        } catch (NumberFormatException e) {
            // ignore
        }
        return -1;
    }

    /**
     * 解析目标 BPM。
     * 优先级：用户文本解析（"120bpm"/"120拍"）> 前端按钮映射（slow→72/medium→108/fast→144）> 默认 108。
     */
    private int parseDurationFromTextCompat(String text) {
        try {
            String lower = text.toLowerCase();
            List<Integer> candidates = new ArrayList<>();

            var m = java.util.regex.Pattern
                    .compile("(\\d+)\\s*(?:\\u5206\\u949f|\\u5206|min|mins|minute|minutes|m)(?:\\s*(\\d+)\\s*(?:\\u79d2|s|sec|secs|second|seconds))?")
                    .matcher(lower);
            while (m.find()) {
                int total = Integer.parseInt(m.group(1)) * 60;
                if (m.group(2) != null) {
                    total += Integer.parseInt(m.group(2));
                }
                candidates.add(total);
            }

            m = java.util.regex.Pattern
                    .compile("([\\u4e00\\u4e8c\\u4e24\\u4e09\\u56db\\u4e94\\u516d\\u4e03\\u516b\\u4e5d\\u5341]+)\\s*(?:\\u5206\\u949f|\\u5206)(?:\\s*([\\u4e00\\u4e8c\\u4e24\\u4e09\\u56db\\u4e94\\u516d\\u4e03\\u516b\\u4e5d\\u5341]+|\\d+)\\s*\\u79d2)?")
                    .matcher(text);
            while (m.find()) {
                int minutes = parseChineseNumber(m.group(1));
                if (minutes > 0) {
                    int total = minutes * 60;
                    if (m.group(2) != null) {
                        String secondsText = m.group(2);
                        int seconds = secondsText.matches("\\d+")
                                ? Integer.parseInt(secondsText)
                                : parseChineseNumber(secondsText);
                        if (seconds > 0) {
                            total += seconds;
                        }
                    }
                    candidates.add(total);
                }
            }

            m = java.util.regex.Pattern
                    .compile("(\\d+)\\s*(?:\\u79d2|s|sec|secs|second|seconds)")
                    .matcher(lower);
            while (m.find()) {
                candidates.add(Integer.parseInt(m.group(1)));
            }

            return candidates.stream()
                    .filter(value -> value >= 5 && value <= 180)
                    .max(Integer::compareTo)
                    .orElse(-1);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int parseChineseNumber(String text) {
        if (text == null || text.isBlank()) return -1;
        int tenIndex = text.indexOf('\u5341');
        if (tenIndex >= 0) {
            int tens = tenIndex == 0 ? 1 : chineseDigit(text.charAt(tenIndex - 1));
            int ones = tenIndex == text.length() - 1 ? 0 : chineseDigit(text.charAt(tenIndex + 1));
            if (tens < 0 || ones < 0) return -1;
            return tens * 10 + ones;
        }
        if (text.length() == 1) {
            return chineseDigit(text.charAt(0));
        }
        return -1;
    }

    private int chineseDigit(char value) {
        return switch (value) {
            case '\u4e00' -> 1;
            case '\u4e8c', '\u4e24' -> 2;
            case '\u4e09' -> 3;
            case '\u56db' -> 4;
            case '\u4e94' -> 5;
            case '\u516d' -> 6;
            case '\u4e03' -> 7;
            case '\u516b' -> 8;
            case '\u4e5d' -> 9;
            default -> -1;
        };
    }

    private int resolveTempoBpm(String userPrompt, String tempo) {
        int parsed = parseBpmFromText(userPrompt);
        if (parsed > 0) return parsed;

        if (tempo == null) return 108;
        return switch (tempo.toLowerCase()) {
            case "slow" -> 72;
            case "fast" -> 144;
            default -> 108; // medium
        };
    }

    /** 从文本中提取 BPM：支持"120bpm"、"120拍"、"120b"、"速度120" */
    private int parseBpmFromText(String text) {
        if (text == null) return -1;
        try {
            var m = java.util.regex.Pattern.compile("(\\d+)\\s*(?:bpm|拍|b\\b)").matcher(text.toLowerCase());
            if (m.find()) {
                int bpm = Integer.parseInt(m.group(1));
                if (bpm >= 20 && bpm <= 300) return bpm;
            }
            m = java.util.regex.Pattern.compile("速度\\s*(\\d+)").matcher(text);
            if (m.find()) {
                int bpm = Integer.parseInt(m.group(1));
                if (bpm >= 20 && bpm <= 300) return bpm;
            }
        } catch (NumberFormatException e) {
            // ignore
        }
        return -1;
    }

    /**
     * 安全解析 JSON 字符串为 Map。
     */
    @SuppressWarnings("unchecked")
    private Integer durationFromPlan(MusicVersion version) {
        Map<String, Object> plan = parseJsonToMap(version.getPlan());
        if (plan == null) return null;
        Object value = plan.get("duration_seconds");
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    // ===================== 回炉辅助方法 =====================

    /** 从 manifest 提取 AI 生成的 change_reason，找不到则用模板兜底 */
    private String extractChangeReason(Map<String, Object> manifest, String feedback) {
        Object cr = manifest.get("change_reason");
        if (cr != null && !cr.toString().trim().isEmpty()) {
            return cr.toString().trim();
        }
        return "根据用户反馈「" + feedback + "」生成新版本。";
    }

    /** 从 manifest 提取 parameter_diff，找不到则后端自行计算 */
    @SuppressWarnings("unchecked")
    private String extractOrComputeDiff(Map<String, Object> manifest,
                                         MusicVersion parent, MusicVersion child) {
        Object pd = manifest.get("parameter_diff");
        if (pd != null) {
            try {
                return objectMapper.writeValueAsString(pd);
            } catch (JsonProcessingException e) {
                log.warn("序列化 parameter_diff 失败");
            }
        }
        // 兜底：后端自行对比新旧 plan
        try {
            return objectMapper.writeValueAsString(computeDiff(parent, child));
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /** 对比父版本和新版本的 plan，生成 parameter_diff */
    @SuppressWarnings("unchecked")
    private Map<String, Object> computeDiff(MusicVersion parent, MusicVersion child) {
        Map<String, Object> oldPlan = parseJsonToMap(parent.getPlan());
        Map<String, Object> newPlan = parseJsonToMap(child.getPlan());
        Map<String, Object> diff = new LinkedHashMap<>();

        if (oldPlan == null || newPlan == null) return diff;

        // mood 对比
        Object oldMood = oldPlan.get("mood");
        Object newMood = newPlan.get("mood");
        if (oldMood != null || newMood != null) {
            diff.put("mood", Map.of("from", oldMood != null ? oldMood : Collections.emptyList(),
                                    "to", newMood != null ? newMood : Collections.emptyList()));
        }
        // tempo 对比
        Object oldTempo = oldPlan.get("tempo");
        Object newTempo = newPlan.get("tempo");
        if (oldTempo != null || newTempo != null) {
            diff.put("tempo", Map.of("from", oldTempo != null ? oldTempo : 0,
                                     "to", newTempo != null ? newTempo : 0));
        }
        // key 对比
        Object oldKey = oldPlan.get("key");
        Object newKey = newPlan.get("key");
        if (oldKey != null || newKey != null) {
            diff.put("key", Map.of("from", oldKey != null ? oldKey : "",
                                   "to", newKey != null ? newKey : ""));
        }
        // instruments 对比
        Object oldInst = oldPlan.get("instruments");
        Object newInst = newPlan.get("instruments");
        if (oldInst instanceof List && newInst instanceof List) {
            List<String> oldList = new ArrayList<>((List<String>) oldInst);
            List<String> newList = new ArrayList<>((List<String>) newInst);
            List<String> added = new ArrayList<>(newList);
            added.removeAll(oldList);
            List<String> removed = new ArrayList<>(oldList);
            removed.removeAll(newList);
            diff.put("instruments", Map.of("add", added, "remove", removed));
        }

        return diff;
    }

    /** 解析 version_label：用户传了就用用户的，没传则自动生成 */
    private String resolveVersionLabel(String userLabel, boolean isFirst,
                                        String feedback, Map<String, Object> plan) {
        if (userLabel != null && !userLabel.trim().isEmpty()) {
            return userLabel.trim();
        }
        if (isFirst) {
            String theme = (plan != null && plan.get("theme") != null)
                    ? plan.get("theme").toString() : "原创音乐";
            return "初次创作·" + theme;
        } else {
            String fb = feedback != null ? feedback : "";
            String shortFeedback = fb.length() > 20 ? fb.substring(0, 20) + "..." : fb;
            return "回炉·" + shortFeedback;
        }
    }

    /**
     * 检测管线是否可用（Python 解释器和管线脚本都存在）。
     */
    public boolean isPipelineAvailable() {
        if (pythonPath == null || pythonPath.isEmpty()) {
            log.info("管线未配置 python 路径");
            return false;
        }
        if (!new File(pythonPath).exists()) {
            log.info("Python 解释器不存在: {}", pythonPath);
            return false;
        }
        Path scriptPath = Path.of(scriptDir).resolve("run_music_pipeline.py");
        if (!scriptPath.toFile().exists()) {
            log.info("管线脚本不存在: {}", scriptPath);
            return false;
        }
        // 检查 API Key（环境变量优先，配置文件兜底）
        String apiKey = System.getenv("MUSIC_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("OPENAI_API_KEY");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = configApiKey;
        }
        if (apiKey == null || apiKey.isEmpty()) {
            log.info("API Key 未配置，管线不可用");
            return false;
        }
        return true;
    }

    /**
     * 使用 Mock 数据填充版本（管线不可用时的降级方案）。
     * 同时生成一个简短的占位 WAV 文件，确保前端播放器可播放。
     */
    private void populateVersionMock(MusicVersion version, String style, String mood,
                                      String tempo, String instrumentsJson) {
        version.setPlan(buildMockPlan(style, mood, tempo, instrumentsJson));
        version.setCaption(buildMockCaption(style, mood, tempo, instrumentsJson));
        version.setMidiPath(outputDir + "/" + version.getVersionId() + ".mid");
        String audioPath = outputDir + "/" + version.getVersionId() + ".wav";
        version.setAudioPath(audioPath);
        // 生成占位音频文件（简单的 C 大调和弦提示音）
        writePlaceholderWav(audioPath, 5.0);
    }

    /**
     * 生成一个简单的占位 WAV 文件（C 大调和弦，5 秒）。
     * 当管线不可用时，确保前端播放器有真实可播放的音频。
     */
    private void writePlaceholderWav(String filePath, double durationSec) {
        try {
            File outFile = new File(filePath);
            outFile.getParentFile().mkdirs();

            int sampleRate = 44100;
            int numSamples = (int) (sampleRate * durationSec);
            int numChannels = 1;
            int bitsPerSample = 16;
            int byteRate = sampleRate * numChannels * bitsPerSample / 8;
            int blockAlign = numChannels * bitsPerSample / 8;
            int dataSize = numSamples * blockAlign;

            // 写入 WAV 文件
            try (java.io.DataOutputStream dos = new java.io.DataOutputStream(
                    new java.io.BufferedOutputStream(new java.io.FileOutputStream(outFile)))) {
                // RIFF header
                dos.writeBytes("RIFF");
                writeLittleEndian(dos, 36 + dataSize, 4);
                dos.writeBytes("WAVE");
                // fmt chunk
                dos.writeBytes("fmt ");
                writeLittleEndian(dos, 16, 4);      // chunk size
                writeLittleEndian(dos, 1, 2);        // PCM
                writeLittleEndian(dos, numChannels, 2);
                writeLittleEndian(dos, sampleRate, 4);
                writeLittleEndian(dos, byteRate, 4);
                writeLittleEndian(dos, blockAlign, 2);
                writeLittleEndian(dos, bitsPerSample, 2);
                // data chunk
                dos.writeBytes("data");
                writeLittleEndian(dos, dataSize, 4);

                // 生成提示音：前 0.5s 静音 + C大三和弦扫弦（C-E-G 三个音符依次响起）
                for (int i = 0; i < numSamples; i++) {
                    double t = (double) i / sampleRate;
                    double sample = 0.0;

                    if (t > 0.5 && t < 1.8) {
                        sample += 0.25 * Math.sin(2 * Math.PI * 261.63 * t); // C4
                    }
                    if (t > 1.5 && t < 2.8) {
                        sample += 0.25 * Math.sin(2 * Math.PI * 329.63 * t); // E4
                    }
                    if (t > 2.5 && t < 4.0) {
                        sample += 0.25 * Math.sin(2 * Math.PI * 392.00 * t); // G4
                    }
                    // 淡出
                    if (t > durationSec - 0.3) {
                        sample *= (durationSec - t) / 0.3;
                    }

                    short val = (short) (sample * 32767 * 0.8);
                    writeLittleEndian(dos, val, 2);
                }
            }
            log.info("占位 WAV 已生成 | path={} | duration={}s", filePath, durationSec);
        } catch (IOException e) {
            log.warn("生成占位 WAV 失败，忽略 | path={}", filePath, e);
        }
    }

    /** 小端序写入 */
    private void writeLittleEndian(java.io.DataOutputStream dos, long value, int numBytes) throws IOException {
        for (int i = 0; i < numBytes; i++) {
            dos.writeByte((int) (value >> (8 * i)) & 0xFF);
        }
    }

    // ===================== Mock 数据辅助方法（降级用） =====================

    private String buildMockPlan(String style, String mood, String tempo,
                                  String instrumentsJson) {
        try {
            List<String> instruments = objectMapper.readValue(
                    instrumentsJson, objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, String.class));

            Map<String, Object> plan = new LinkedHashMap<>();
            plan.put("theme", "a beautiful musical piece");
            plan.put("style", style != null ? style : "pop");
            plan.put("mood", Arrays.asList(mood != null ? mood : "calm"));
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
                    style != null ? style : "pop",
                    mood != null ? mood : "calm",
                    tempo != null ? tempo : "medium",
                    instStr);
        } catch (JsonProcessingException e) {
            return "A beautiful instrumental piece.";
        }
    }

    private int convertTempo(String tempo) {
        if (tempo == null) return 88;
        return switch (tempo.toLowerCase()) {
            case "slow" -> 72;
            case "fast" -> 120;
            default -> 88;
        };
    }
}
