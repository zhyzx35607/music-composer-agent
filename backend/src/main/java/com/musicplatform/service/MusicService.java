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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
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

    public MusicService(MusicVersionRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * 生成下一个版本 ID（线程安全，数据库级别取最大序号）。
     */
    private synchronized String nextVersionId() {
        int maxNum = repository.findMaxVersionNumber();
        return "v" + (maxNum + 1);
    }

    /**
     * 首次生成音乐 — 尝试调用智能体管线，失败时降级为 Mock。
     */
    @Transactional
    public MusicVersion generate(String userPrompt, String style, String mood,
                                  String tempo, String instrumentsJson) {
        String versionId = nextVersionId();

        log.info("创建新版本 | version_id={} | style={} | mood={}", versionId, style, mood);

        // 创建版本记录
        MusicVersion version = new MusicVersion(versionId, userPrompt,
                style, mood, tempo, instrumentsJson);
        version.setCreatedAt(LocalDateTime.now());

        // 尝试调用智能体管线
        if (isPipelineAvailable()) {
            try {
                int duration = convertTempoToDuration(tempo);
                String instrumentsList = convertInstrumentsJsonToList(instrumentsJson);
                Map<String, Object> manifest = runPipeline(userPrompt, duration, style, mood,
                        instrumentsList, versionId);
                populateVersionFromManifest(version, manifest, versionId);
                version.setMock(false);
                log.info("管线生成完成 | version_id={}", versionId);
            } catch (Exception e) {
                log.warn("管线调用失败，降级为 Mock | version_id={} | error={}", versionId, e.getMessage());
                populateVersionMock(version, style, mood, tempo, instrumentsJson);
                version.setMock(true);
            }
        } else {
            log.info("管线不可用，使用 Mock 数据 | version_id={}", versionId);
            populateVersionMock(version, style, mood, tempo, instrumentsJson);
            version.setMock(true);
        }

        MusicVersion saved = repository.save(version);
        log.info("版本已保存 | version_id={} | midi_path={} | audio_path={} | mock={}",
                saved.getVersionId(), saved.getMidiPath(), saved.getAudioPath(), saved.isMock());
        return saved;
    }

    /**
     * 反馈修改 — 尝试调用管线，失败时降级为 Mock。
     */
    @Transactional
    public MusicVersion revise(String parentVersionId, String feedback) {
        log.info("反馈修改 | parent={} | feedback={}", parentVersionId, feedback);

        MusicVersion parent = repository.findById(parentVersionId)
                .orElseThrow(() -> new NoSuchElementException("版本不存在: " + parentVersionId));

        String versionId = nextVersionId();
        log.info("基于 {} 创建新版本 | version_id={}", parentVersionId, versionId);

        // 创建新版本（继承父版本参数）
        MusicVersion version = new MusicVersion(versionId,
                parent.getUserPrompt(),
                parent.getStyle(), parent.getMood(),
                parent.getTempo(), parent.getInstruments());
        version.setParentVersionId(parentVersionId);
        version.setFeedback(feedback);
        version.setCreatedAt(LocalDateTime.now());

        // 尝试调用智能体管线
        if (isPipelineAvailable()) {
            try {
                String revisedPrompt = buildRevisePrompt(parent.getUserPrompt(), feedback,
                        parent.getStyle(), parent.getMood());
                int duration = convertTempoToDuration(parent.getTempo());
                String instrumentsList = convertInstrumentsJsonToList(parent.getInstruments());
                Map<String, Object> manifest = runPipeline(revisedPrompt, duration,
                        parent.getStyle(), parent.getMood(), instrumentsList, versionId);
                populateVersionFromManifest(version, manifest, versionId);
                version.setMock(false);

                @SuppressWarnings("unchecked")
                Map<String, Object> newPlan = parseJsonToMap(version.getPlan());
                if (newPlan != null && newPlan.containsKey("title")) {
                    version.setChangeReason("根据用户反馈「" + feedback + "」，生成了新版本「"
                            + newPlan.get("title") + "」。");
                } else {
                    version.setChangeReason("根据用户反馈「" + feedback + "」生成新版本。");
                }
            } catch (Exception e) {
                log.warn("管线调用失败（revise），降级为 Mock | version_id={} | error={}",
                        versionId, e.getMessage());
                populateVersionMock(version, parent.getStyle(), parent.getMood(),
                        parent.getTempo(), parent.getInstruments());
                version.setMock(true);
                version.setChangeReason("根据用户反馈「" + feedback + "」，调整了音乐参数。"
                        + "（管线不可用，使用 Mock 数据）");
            }
        } else {
            log.info("管线不可用，使用 Mock 数据（revise）| version_id={}", versionId);
            populateVersionMock(version, parent.getStyle(), parent.getMood(),
                    parent.getTempo(), parent.getInstruments());
            version.setMock(true);
            version.setChangeReason("根据用户反馈「" + feedback + "」，调整了音乐参数。"
                    + "（管线不可用，使用 Mock 数据）");
        }

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

    // ===================== 管线调用 =====================

    /**
     * 调用 run_music_pipeline.py，解析返回的 manifest JSON。
     */
    private Map<String, Object> runPipeline(String request, int duration,
                                             String style, String mood,
                                             String instruments,
                                             String outputName)
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
        command.add("--request");
        command.add(request);
        command.add("--duration");
        command.add(String.valueOf(duration));
        command.add("--output-name");
        command.add(outputName);
        command.add("--model");
        command.add(model);
        command.add("--timeout");
        command.add(String.valueOf(timeout));

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

        // 读取 stdout（manifest JSON）
        StringBuilder stdout = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stdout.append(line).append("\n");
            }
        }

        // 读取 stderr（日志/错误）
        StringBuilder stderr = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stderr.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(timeout + 30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("管线超时（" + (timeout + 30) + "秒）");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.error("管线执行失败 | exitCode={} | stderr={}", exitCode, stderr);
            throw new IOException("管线执行失败 (exit " + exitCode + "): " + stderr);
        }

        // 解析 manifest JSON
        String manifestStr = stdout.toString().trim();
        // 从 stdout 中提取最后一个完整 JSON 对象
        int lastBrace = manifestStr.lastIndexOf('{');
        if (lastBrace >= 0) {
            manifestStr = manifestStr.substring(lastBrace);
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
    private void populateVersionFromManifest(MusicVersion version,
                                              Map<String, Object> manifest,
                                              String versionId) {
        Map<String, Object> urls = (Map<String, Object>) manifest.getOrDefault("urls",
                Collections.emptyMap());

        // 读取生成的 music_json 构建完整的 plan
        Map<String, Object> plan = new LinkedHashMap<>();
        String musicJsonPath = scriptDir + "/outputs/" + versionId + ".json";

        try {
            Map<String, Object> musicJson = objectMapper.readValue(
                    new File(musicJsonPath), Map.class);

            plan.put("theme", musicJson.getOrDefault("title", "Untitled"));
            plan.put("style", musicJson.getOrDefault("style", ""));
            plan.put("mood", musicJson.getOrDefault("mood", Collections.emptyList()));
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
     * 构建修改版本的 prompt——合并原始需求和用户反馈。
     */
    private String buildRevisePrompt(String originalPrompt, String feedback,
                                      String style, String mood) {
        StringBuilder sb = new StringBuilder();
        sb.append("Revise the following music based on user feedback.\n\n");
        sb.append("Original request: ").append(originalPrompt).append("\n");
        if (style != null && !style.isEmpty()) {
            sb.append("Style: ").append(style).append("\n");
        }
        if (mood != null && !mood.isEmpty()) {
            sb.append("Mood: ").append(mood).append("\n");
        }
        sb.append("\nUser feedback: ").append(feedback).append("\n\n");
        sb.append("Please generate a revised version that addresses this feedback "
                + "while maintaining the overall musical direction.");
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
     * 将速度提示词转为时长（秒）。
     */
    private int convertTempoToDuration(String tempo) {
        if (tempo == null) return 30;
        return switch (tempo.toLowerCase()) {
            case "slow" -> 60;
            case "fast" -> 15;
            default -> 30; // medium
        };
    }

    /**
     * 安全解析 JSON 字符串为 Map。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return null;
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
