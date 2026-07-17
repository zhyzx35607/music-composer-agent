package com.musicplatform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicplatform.model.UploadedFile;
import com.musicplatform.repository.UploadedFileRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.ZipInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 文件上传与内容提取服务。
 * 支持 txt / docx / xlsx / MusicXML 四种格式。
 */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "txt", "docx", "xlsx", "musicxml", "xml", "mxl", "mid", "midi");

    private final UploadedFileRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${music-platform.upload.upload-dir:./outputs/uploads}")
    private String uploadDir;

    @Value("${music-platform.upload.max-size:10485760}")
    private long maxSize;

    @Value("${music-platform.pipeline.python:}")
    private String pythonPath;

    @Value("${music-platform.pipeline.script-dir:../agent/gpt_music_pipeline}")
    private String scriptDir;

    public FileService(UploadedFileRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    // ===================== 公共方法 =====================

    /** 上传并提取文件 */
    public UploadedFile upload(String originalName, byte[] content,
                                String versionId, String trackId) throws IOException {
        // 校验文件大小
        if (content.length > maxSize) {
            throw new IOException("文件过大: " + content.length + " bytes (最大 " + maxSize + " bytes)");
        }

        // 确定文件类型
        String ext = getExtension(originalName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IOException("不支持的文件格式: ." + ext + " (允许: " + String.join(", ", ALLOWED_EXTENSIONS) + ")");
        }

        // 归一化 musicxml / mxl → musicxml；.mxl 先解压
        String fileType = switch (ext) {
            case "xml" -> "musicxml";
            case "mxl" -> { content = decompressMxl(content); yield "musicxml"; }
            case "mid", "midi" -> "midi";
            default -> ext;
        };

        // 提取内容
        String extractedText;
        String extractedJson = null;

        try {
            if (isScoreFile(fileType)) {
                extractedJson = extractScoreJson(content, fileType, originalName);
                extractedText = buildScoreSummaryFromJson(fileType, extractedJson);
            } else if ("xlsx".equals(fileType)) {
                extractedText = extractText(fileType, content);
                extractedJson = extractXlsxJson(content);
            } else {
                extractedText = extractText(fileType, content);
            }
        } catch (Exception e) {
            log.error("文件提取失败 | file={} | error={}", originalName, e.getMessage());
            throw new IOException("无法解析文件内容: " + e.getMessage(), e);
        }

        // 存储文件
        Path uploadPath = Path.of(uploadDir, trackId != null ? trackId : "uploads")
                .toAbsolutePath()
                .normalize();
        Files.createDirectories(uploadPath);
        String storedName = System.currentTimeMillis() + "_" + sanitizeFileName(originalName);
        Path storedPath = uploadPath.resolve(storedName);
        Files.write(storedPath, content);

        // 创建记录
        UploadedFile entity = new UploadedFile(
                originalName, fileType, storedPath.toString(),
                extractedText, extractedJson, (long) content.length,
                versionId, trackId);

        UploadedFile saved = repository.save(entity);
        log.info("文件上传完成 | id={} | name={} | type={} | size={}",
                saved.getId(), originalName, fileType, content.length);
        return saved;
    }

    /** 查询某版本的所有文件 */
    public List<UploadedFile> getFilesByVersion(String versionId) {
        return repository.findByVersionIdOrderByCreatedAtAsc(versionId);
    }

    /** 查询某 Track 的所有文件 */
    public List<UploadedFile> getFilesByTrack(String trackId) {
        return repository.findByTrackIdOrderByCreatedAtAsc(trackId);
    }

    /** Find the first uploaded score file that can be passed to the Python pipeline. */
    public Optional<UploadedFile> findFirstScoreFile(String trackId, String versionId, List<Long> fileIds) {
        return collectReferenceFiles(trackId, versionId, fileIds).stream()
                .filter(f -> isScoreFile(f.getFileType()))
                .findFirst();
    }

    /** 删除文件 */
    public void deleteFile(Long fileId) throws IOException {
        UploadedFile file = repository.findById(fileId)
                .orElseThrow(() -> new NoSuchElementException("文件不存在: " + fileId));
        // 删除物理文件
        try {
            Files.deleteIfExists(resolveStoredPath(file));
        } catch (IOException e) {
            log.warn("删除物理文件失败 | path={}", file.getStoredPath());
        }
        repository.delete(file);
        log.info("文件已删除 | id={} | name={}", fileId, file.getOriginalName());
    }

    /** Bind uploaded files to the version/track that actually uses them. */
    public void attachFiles(List<Long> fileIds, String versionId, String trackId) {
        if (fileIds == null || fileIds.isEmpty()) return;
        List<UploadedFile> files = repository.findAllById(fileIds);
        for (UploadedFile file : files) {
            if (versionId != null && !versionId.isBlank()) {
                file.setVersionId(versionId);
            }
            if (trackId != null && !trackId.isBlank()) {
                file.setTrackId(trackId);
            }
        }
        repository.saveAll(files);
        log.info("Reference files attached | version_id={} | track_id={} | count={}",
                versionId, trackId, files.size());
    }

    /**
     * Resolve both new absolute upload paths and legacy relative paths.
     * Legacy records were written relative to the backend process working directory,
     * while the Python process runs from the agent script directory.
     */
    public Path resolveStoredPath(UploadedFile file) {
        if (file == null || file.getStoredPath() == null || file.getStoredPath().isBlank()) {
            throw new IllegalArgumentException("上传文件路径为空");
        }
        Path stored = Path.of(file.getStoredPath());
        Path resolved = stored.isAbsolute()
                ? stored.normalize()
                : Path.of("").toAbsolutePath().resolve(stored).normalize();
        if (!Files.exists(resolved)) {
            throw new IllegalArgumentException("上传文件不存在: " + resolved);
        }
        return resolved;
    }

    /** 将 Track 下所有文件的提取内容拼成 prompt 片段 */
    public String buildReferenceFilesSection(String trackId) {
        return buildReferenceFilesSection(trackId, null, null);
    }

    /** Build prompt section from explicit file ids plus version/track linked files. */
    public String buildReferenceFilesSection(String trackId, String versionId, List<Long> fileIds) {
        List<UploadedFile> files = collectReferenceFiles(trackId, versionId, fileIds);
        if (files.isEmpty()) return "";

        List<UploadedFile> scoreFiles = files.stream()
                .filter(f -> isScoreFile(f.getFileType()))
                .toList();
        List<UploadedFile> otherFiles = files.stream()
                .filter(f -> !isScoreFile(f.getFileType()))
                .toList();

        StringBuilder sb = new StringBuilder();
        if (!scoreFiles.isEmpty()) {
            sb.append("\n\n=== PRIMARY SCORE INPUT: PARSED SCORE FILE ===\n");
            sb.append("Treat the following parsed score data as the authoritative musical material. ");
            sb.append("When revising, preserve recognizable motifs, rhythm, harmony, sections, and instrumentation unless the user asks to change them.\n\n");
            for (UploadedFile f : scoreFiles) {
                appendReferenceFile(sb, f);
            }
            sb.append("=== END PRIMARY SCORE INPUT ===\n");
        }

        if (!otherFiles.isEmpty()) {
            sb.append("\n\n=== REFERENCE FILES ===\n\n");
            for (UploadedFile f : otherFiles) {
                appendReferenceFile(sb, f);
            }
            sb.append("=== END REFERENCE FILES ===\n");
        }
        return sb.toString();
    }

    private List<UploadedFile> collectReferenceFiles(String trackId, String versionId, List<Long> fileIds) {
        Map<Long, UploadedFile> map = new LinkedHashMap<>();
        if (fileIds != null && !fileIds.isEmpty()) {
            for (UploadedFile f : repository.findAllById(fileIds)) {
                map.put(f.getId(), f);
            }
        }
        if (versionId != null && !versionId.isBlank()) {
            for (UploadedFile f : repository.findByVersionIdOrderByCreatedAtAsc(versionId)) {
                map.put(f.getId(), f);
            }
        }
        if (trackId != null && !trackId.isBlank()) {
            for (UploadedFile f : repository.findByTrackIdOrderByCreatedAtAsc(trackId)) {
                map.put(f.getId(), f);
            }
        }
        return new ArrayList<>(map.values());
    }

    private void appendReferenceFile(StringBuilder sb, UploadedFile f) {
        sb.append("[").append(f.getOriginalName()).append(" | type=").append(f.getFileType())
                .append(" | id=").append(f.getId()).append("]\n");
        if (f.getExtractedText() != null && !f.getExtractedText().isEmpty()) {
            sb.append(f.getExtractedText()).append("\n");
        }
        if (f.getExtractedJson() != null && !f.getExtractedJson().isEmpty()) {
            sb.append(formatExtractedJson(f.getFileType(), f.getExtractedJson()));
        }
        sb.append("\n");
    }

    // ===================== 按格式提取 =====================

    /** 根据文件类型提取文本 */
    String extractText(String fileType, byte[] content) throws Exception {
        return switch (fileType) {
            case "txt" -> new String(content, StandardCharsets.UTF_8);
            case "docx" -> extractDocx(content);
            case "xlsx" -> extractXlsx(content);
            case "musicxml" -> extractMusicXml(content);
            default -> throw new IOException("不支持的文件类型: " + fileType);
        };
    }

    // ---- docx ----

    private String extractDocx(byte[] content) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(content))) {
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            for (XWPFParagraph p : paragraphs) {
                String text = p.getText();
                if (text != null && !text.trim().isEmpty()) {
                    sb.append(text).append("\n");
                }
            }
        }
        return sb.toString().trim();
    }

    // ---- xlsx ----

    private String extractXlsx(byte[] content) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            var sheet = wb.getSheetAt(0);
            for (var row : sheet) {
                List<String> cells = new ArrayList<>();
                for (var cell : row) {
                    cells.add(cell.toString());
                }
                sb.append(String.join("\t", cells)).append("\n");
            }
        }
        return sb.toString().trim();
    }

    /** xlsx 转结构化 JSON（首行作为表头） */
    @SuppressWarnings("unchecked")
    private String extractXlsxJson(byte[] content) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            var sheet = wb.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() == 0) return "[]";

            List<String> headers = new ArrayList<>();
            var headerRow = sheet.getRow(0);
            if (headerRow != null) {
                headerRow.forEach(cell -> headers.add(cell.toString()));
            }

            List<Map<String, String>> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                var row = sheet.getRow(i);
                if (row == null) continue;
                Map<String, String> map = new LinkedHashMap<>();
                for (int j = 0; j < row.getLastCellNum(); j++) {
                    var cell = row.getCell(j);
                    String key = j < headers.size() ? headers.get(j) : "col" + (j + 1);
                    map.put(key, cell != null ? cell.toString() : "");
                }
                if (!map.values().stream().allMatch(String::isEmpty)) {
                    rows.add(map);
                }
            }

            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rows);
            } catch (JsonProcessingException e) {
                return "[]";
            }
        }
    }

    /** JSON 转为可读文本（用于 prompt 注入） */
    private String formatExtractedJson(String fileType, String json) {
        if (json == null || json.isEmpty()) return "";
        if (isScoreFile(fileType)) {
            return "Parsed score context JSON:\n" + json + "\n";
        }
        return json + "\n";
    }

    private boolean isScoreFile(String fileType) {
        return fileType != null && Set.of("musicxml", "midi").contains(fileType.toLowerCase());
    }

    private String extractScoreJson(byte[] content, String fileType, String originalName) throws Exception {
        try {
            return extractScoreJsonWithMusic21(content, fileType, originalName);
        } catch (Exception e) {
            if ("musicxml".equals(fileType)) {
                log.warn("music21 score extraction failed, falling back to Java MusicXML parser | file={} | error={}",
                        originalName, e.getMessage());
                return extractMusicXmlJson(content);
            }
            throw e;
        }
    }

    private String extractScoreJsonWithMusic21(byte[] content, String fileType, String originalName) throws Exception {
        if (pythonPath == null || pythonPath.isBlank()) {
            throw new IOException("music-platform.pipeline.python is not configured");
        }

        Path scriptDirPath = Path.of(scriptDir).toAbsolutePath().normalize();
        Path script = scriptDirPath.resolve("score_to_gpt_context.py");
        if (!Files.exists(script)) {
            throw new IOException("score parser script not found: " + script);
        }

        String suffix = "midi".equals(fileType) ? ".mid" : ".musicxml";
        Path tempDir = scriptDirPath.resolve("outputs").resolve("_uploaded_scores");
        Files.createDirectories(tempDir);
        Path tempFile = Files.createTempFile(tempDir, "score_", suffix);
        Files.write(tempFile, content);

        List<String> command = new ArrayList<>();
        command.add(pythonPath);
        command.add(script.toString());
        command.add(tempFile.toString());
        command.add("--max-events");
        command.add("4000");

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(scriptDirPath.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("score parser timeout");
        }
        if (process.exitValue() != 0) {
            throw new IOException("score parser failed: " + output);
        }

        String json = output.toString().trim();
        int firstBrace = json.indexOf('{');
        if (firstBrace > 0) {
            json = json.substring(firstBrace);
        }
        objectMapper.readTree(json);
        log.info("Score extracted with music21 | file={} | type={} | chars={}",
                originalName, fileType, json.length());
        return json;
    }

    private String buildScoreSummaryFromJson(String fileType, String json) {
        try {
            var root = objectMapper.readTree(json);
            return String.format(
                    "Parsed score input. Format: %s, Parser: %s, Title: %s, Key: %s, Time: %s, Tempo: %s BPM, Parts: %s, Events included: %s, Duration beats: %s, Estimated seconds: %s, Truncated: %s",
                    root.path("score_format").asText(fileType),
                    root.path("parser").asText("unknown"),
                    root.path("title").asText("Untitled Score"),
                    root.path("key").asText("unknown"),
                    root.path("time_signatures").toString(),
                    root.path("tempo_bpm").isMissingNode() || root.path("tempo_bpm").isNull() ? "unknown" : root.path("tempo_bpm").asText(),
                    root.path("part_count").asText("unknown"),
                    root.path("event_count_included").asText("unknown"),
                    root.path("duration_quarter_length").asText("unknown"),
                    root.path("estimated_duration_seconds").isMissingNode() || root.path("estimated_duration_seconds").isNull() ? "unknown" : root.path("estimated_duration_seconds").asText(),
                    root.path("truncated").asText("false"));
        } catch (Exception e) {
            return "Parsed score input. Format: " + fileType;
        }
    }

    // ---- MusicXML ----

    private String extractMusicXml(byte[] content) throws Exception {
        Document doc = parseXml(content);
        Element root = doc.getDocumentElement();
        Map<String, Object> score = buildMusicXmlScore(root);

        return String.format(
                "MusicXML score input. Title: %s, Key: %s, Time: %s, Tempo: %s BPM, Notes: %s, Rests: %s, Range: %s, Estimated beats: %s\n"
                        + "Melody: %s\n"
                        + "Chords: %s",
                score.get("title"), score.get("key"), score.get("time_signature"),
                score.get("tempo_bpm"), score.get("pitched_note_count"), score.get("rest_count"),
                score.get("range"), score.get("estimated_total_beats"),
                String.join(", ", extractMelodyNotes(root, 24)),
                ((List<?>) score.get("chords")).isEmpty() ? "(none)" : score.get("chords"));
    }

    private String extractMusicXmlJson(byte[] content) throws Exception {
        Document doc = parseXml(content);
        Element root = doc.getDocumentElement();
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(buildMusicXmlScore(root));
    }

    private Map<String, Object> buildMusicXmlScore(Element root) {
        Map<String, Object> score = new LinkedHashMap<>();
        int defaultDivisions = Math.max(1, extractDivisions(root));
        List<Map<String, Object>> parts = extractMusicXmlParts(root, defaultDivisions);
        List<Map<String, Object>> notes = parts.stream()
                .flatMap(part -> ((List<Map<String, Object>>) part.get("notes")).stream())
                .toList();

        long pitchedCount = notes.stream().filter(n -> Boolean.FALSE.equals(n.get("rest"))).count();
        long restCount = notes.stream().filter(n -> Boolean.TRUE.equals(n.get("rest"))).count();
        double totalBeats = notes.stream()
                .mapToDouble(n -> numberAsDouble(n.get("start_beat")) + numberAsDouble(n.get("duration_beats")))
                .max().orElse(0.0);

        score.put("score_format", "MusicXML");
        score.put("standard", "W3C MusicXML");
        score.put("root_element", root.getTagName());
        score.put("title", extractTitle(root));
        score.put("key", extractKeySignature(root));
        score.put("time_signature", extractTimeSignature(root));
        score.put("tempo_bpm", extractTempo(root));
        score.put("divisions", defaultDivisions);
        score.put("measure_count", countMeasures(root));
        score.put("pitched_note_count", pitchedCount);
        score.put("rest_count", restCount);
        score.put("estimated_total_beats", roundBeats(totalBeats));
        score.put("range", extractRange(root));
        score.put("melody_notes", extractMelodyNotes(root, 32));
        score.put("chords", extractChords(root));
        score.put("parts", parts);
        return score;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractMusicXmlParts(Element root, int defaultDivisions) {
        Map<String, String> partNames = extractPartNames(root);
        NodeList partNodes = root.getElementsByTagName("part");
        List<Map<String, Object>> parts = new ArrayList<>();

        for (int i = 0; i < partNodes.getLength(); i++) {
            Element partEl = (Element) partNodes.item(i);
            String partId = partEl.getAttribute("id");
            String partName = partNames.getOrDefault(partId, partId == null || partId.isBlank() ? "Part " + (i + 1) : partId);
            List<Map<String, Object>> notes = new ArrayList<>();
            double cursorBeat = 0.0;
            double lastNoteStart = 0.0;
            int divisions = defaultDivisions;
            int measureCount = 0;

            for (Element measure : childElements(partEl, "measure")) {
                measureCount++;
                String measureNumber = measure.getAttribute("number");
                Integer updatedDivisions = extractMeasureDivisions(measure);
                if (updatedDivisions != null && updatedDivisions > 0) {
                    divisions = updatedDivisions;
                }

                for (Element child : childElements(measure, null)) {
                    String tag = child.getTagName();
                    if ("note".equals(tag)) {
                        double durationBeats = parseDurationBeats(child, divisions);
                        boolean chord = hasChild(child, "chord");
                        boolean rest = hasChild(child, "rest");
                        double startBeat = chord ? lastNoteStart : cursorBeat;
                        if (!chord) {
                            lastNoteStart = startBeat;
                        }

                        if (notes.size() < 320) {
                            Map<String, Object> note = new LinkedHashMap<>();
                            note.put("measure", measureNumber == null || measureNumber.isBlank() ? measureCount : measureNumber);
                            note.put("start_beat", roundBeats(startBeat));
                            note.put("duration_beats", roundBeats(durationBeats));
                            note.put("voice", firstDescendantText(child, "voice", ""));
                            note.put("rest", rest);
                            note.put("chord", chord);
                            if (!rest) {
                                Map<String, Object> pitch = extractPitch(child);
                                note.putAll(pitch);
                            }
                            notes.add(note);
                        }
                        if (!chord) {
                            cursorBeat += durationBeats;
                        }
                    } else if ("backup".equals(tag)) {
                        cursorBeat = Math.max(0.0, cursorBeat - parseDurationBeats(child, divisions));
                    } else if ("forward".equals(tag)) {
                        cursorBeat += parseDurationBeats(child, divisions);
                    }
                }
            }

            Map<String, Object> part = new LinkedHashMap<>();
            part.put("part_id", partId);
            part.put("name", partName);
            part.put("measure_count", measureCount);
            part.put("sampled_note_events", notes.size());
            part.put("notes", notes);
            parts.add(part);
        }
        return parts;
    }

    private Map<String, String> extractPartNames(Element root) {
        Map<String, String> names = new LinkedHashMap<>();
        NodeList scoreParts = root.getElementsByTagName("score-part");
        for (int i = 0; i < scoreParts.getLength(); i++) {
            Element scorePart = (Element) scoreParts.item(i);
            String id = scorePart.getAttribute("id");
            String name = firstDescendantText(scorePart, "part-name", id);
            if (id != null && !id.isBlank()) {
                names.put(id, name == null || name.isBlank() ? id : name);
            }
        }
        return names;
    }

    private String extractTitle(Element root) {
        String title = firstDescendantText(root, "work-title", "");
        if (title == null || title.isBlank()) {
            title = firstDescendantText(root, "movement-title", "");
        }
        return title == null || title.isBlank() ? "Untitled MusicXML Score" : title;
    }

    private int extractDivisions(Element root) {
        NodeList divisions = root.getElementsByTagName("divisions");
        if (divisions.getLength() == 0) return 1;
        return parseIntSafe(divisions.item(0).getTextContent(), 1);
    }

    private Integer extractMeasureDivisions(Element measure) {
        NodeList divisions = measure.getElementsByTagName("divisions");
        if (divisions.getLength() == 0) return null;
        return parseIntSafe(divisions.item(0).getTextContent(), 1);
    }

    private List<Element> childElements(Element parent, String tagName) {
        List<Element> result = new ArrayList<>();
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element element
                    && (tagName == null || tagName.equals(element.getTagName()))) {
                result.add(element);
            }
            child = child.getNextSibling();
        }
        return result;
    }

    private boolean hasChild(Element parent, String tagName) {
        return !childElements(parent, tagName).isEmpty();
    }

    private double parseDurationBeats(Element element, int divisions) {
        String text = firstDescendantText(element, "duration", "0");
        double raw = parseDoubleSafe(text, 0.0);
        return divisions <= 0 ? raw : raw / divisions;
    }

    private Map<String, Object> extractPitch(Element note) {
        Map<String, Object> pitchData = new LinkedHashMap<>();
        NodeList pitchNodes = note.getElementsByTagName("pitch");
        if (pitchNodes.getLength() == 0) return pitchData;

        Element pitch = (Element) pitchNodes.item(0);
        String step = firstDescendantText(pitch, "step", "");
        int alter = parseIntSafe(firstDescendantText(pitch, "alter", "0"), 0);
        int octave = parseIntSafe(firstDescendantText(pitch, "octave", "4"), 4);
        int midi = pitchToMidi(step, alter, octave);

        pitchData.put("pitch", formatPitch(step, alter, octave));
        pitchData.put("midi", midi);
        pitchData.put("step", step);
        pitchData.put("alter", alter);
        pitchData.put("octave", octave);
        return pitchData;
    }

    private String firstDescendantText(Element element, String tagName, String fallback) {
        NodeList list = element.getElementsByTagName(tagName);
        if (list.getLength() == 0) return fallback;
        String text = list.item(0).getTextContent();
        return text == null ? fallback : text.trim();
    }

    private int countMeasures(Element root) {
        return root.getElementsByTagName("measure").getLength();
    }

    private double numberAsDouble(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        if (value instanceof String text) return parseDoubleSafe(text, 0.0);
        return 0.0;
    }

    private double roundBeats(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private int parseIntSafe(String text, int fallback) {
        try {
            return Integer.parseInt(text == null ? "" : text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private double parseDoubleSafe(String text, double fallback) {
        try {
            return Double.parseDouble(text == null ? "" : text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private int pitchToMidi(String step, int alter, int octave) {
        int semitone = switch (step == null ? "" : step.toUpperCase()) {
            case "C" -> 0;
            case "D" -> 2;
            case "E" -> 4;
            case "F" -> 5;
            case "G" -> 7;
            case "A" -> 9;
            case "B" -> 11;
            default -> 0;
        };
        return Math.max(0, Math.min(127, (octave + 1) * 12 + semitone + alter));
    }

    private String formatPitch(String step, int alter, int octave) {
        String accidental = alter > 0 ? "#".repeat(alter) : alter < 0 ? "b".repeat(-alter) : "";
        return (step == null ? "" : step) + accidental + octave;
    }

    private Document parseXml(byte[] content) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(content));
    }

    /** 提取调号 */
    private String extractKeySignature(Element root) {
        NodeList fifthsList = root.getElementsByTagName("fifths");
        if (fifthsList.getLength() > 0) {
            String fifths = fifthsList.item(0).getTextContent().trim();
            return fifthsToKey(fifths);
        }
        return "C major";
    }

    /** 提取拍号 */
    private String extractTimeSignature(Element root) {
        NodeList timeList = root.getElementsByTagName("time");
        if (timeList.getLength() > 0) {
            Element time = (Element) timeList.item(0);
            NodeList beats = time.getElementsByTagName("beats");
            NodeList beatType = time.getElementsByTagName("beat-type");
            if (beats.getLength() > 0 && beatType.getLength() > 0) {
                return beats.item(0).getTextContent() + "/" + beatType.item(0).getTextContent();
            }
        }
        return "4/4";
    }

    /** 提取速度 */
    private int extractTempo(Element root) {
        NodeList soundList = root.getElementsByTagName("sound");
        for (int i = 0; i < soundList.getLength(); i++) {
            Element sound = (Element) soundList.item(i);
            String tempoAttr = sound.getAttribute("tempo");
            if (tempoAttr != null && !tempoAttr.isEmpty()) {
                return (int) Double.parseDouble(tempoAttr);
            }
        }
        return 120;
    }

    /** 提取和弦标记 */
    private List<String> extractChords(Element root) {
        NodeList harmonyList = root.getElementsByTagName("harmony");
        Set<String> chords = new LinkedHashSet<>();
        for (int i = 0; i < harmonyList.getLength(); i++) {
            Element harmony = (Element) harmonyList.item(i);
            NodeList roots = harmony.getElementsByTagName("root");
            NodeList kinds = harmony.getElementsByTagName("kind");
            if (roots.getLength() > 0) {
                NodeList rootSteps = ((Element) roots.item(0)).getElementsByTagName("root-step");
                NodeList rootAlters = ((Element) roots.item(0)).getElementsByTagName("root-alter");
                String rootNote = rootSteps.getLength() > 0 ? rootSteps.item(0).getTextContent() : "";
                String kind = kinds.getLength() > 0 ? kinds.item(0).getTextContent() : "";
                if (!rootNote.isEmpty()) {
                    chords.add(rootNote + kind);
                }
            }
        }
        return new ArrayList<>(chords);
    }

    /** 统计音符总数 */
    private int countNotes(Element root) {
        return root.getElementsByTagName("note").getLength();
    }

    /** 提取大概音域 */
    private String extractRange(Element root) {
        NodeList notes = root.getElementsByTagName("note");
        String lowest = null;
        String highest = null;

        for (int i = 0; i < notes.getLength(); i++) {
            Element note = (Element) notes.item(i);
            NodeList pitches = note.getElementsByTagName("pitch");
            if (pitches.getLength() > 0) {
                Element pitch = (Element) pitches.item(0);
                NodeList steps = pitch.getElementsByTagName("step");
                NodeList octaves = pitch.getElementsByTagName("octave");
                if (steps.getLength() > 0 && octaves.getLength() > 0) {
                    String noteName = steps.item(0).getTextContent() + octaves.item(0).getTextContent();
                    if (lowest == null || noteName.compareTo(lowest) < 0) lowest = noteName;
                    if (highest == null || noteName.compareTo(highest) > 0) highest = noteName;
                }
            }
        }
        return (lowest != null && highest != null)
                ? lowest + " ~ " + highest : "unknown";
    }

    /** 提取前 N 个旋律音符 */
    private List<String> extractMelodyNotes(Element root, int maxCount) {
        NodeList notes = root.getElementsByTagName("note");
        List<String> result = new ArrayList<>();
        for (int i = 0; i < notes.getLength() && result.size() < maxCount; i++) {
            Element note = (Element) notes.item(i);
            NodeList pitches = note.getElementsByTagName("pitch");
            if (pitches.getLength() > 0) {
                Element pitch = (Element) pitches.item(0);
                NodeList steps = pitch.getElementsByTagName("step");
                NodeList octaves = pitch.getElementsByTagName("octave");
                if (steps.getLength() > 0 && octaves.getLength() > 0) {
                    result.add(steps.item(0).getTextContent() + octaves.item(0).getTextContent());
                }
            }
        }
        return result;
    }

    // ===================== 工具方法 =====================

    /** 解压 .mxl 文件（ZIP 包），提取第一个 .xml 或 .musicxml 文件内容 */
    private byte[] decompressMxl(byte[] content) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(content))) {
            var entry = zis.getNextEntry();
            while (entry != null) {
                String name = entry.getName().toLowerCase();
                if (name.endsWith(".xml") || name.endsWith(".musicxml")) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = zis.read(buf)) != -1) {
                        bos.write(buf, 0, n);
                    }
                    log.info("MXL 解压完成 | entry={} | size={}", entry.getName(), bos.size());
                    return bos.toByteArray();
                }
                entry = zis.getNextEntry();
            }
        }
        throw new IOException("MXL 文件中未找到 XML 内容");
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1);
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /** 将 fifths 值转为调号名称 */
    private String fifthsToKey(String fifths) {
        try {
            int f = Integer.parseInt(fifths);
            return switch (f) {
                case -7 -> "Cb major / Ab minor";
                case -6 -> "Gb major / Eb minor";
                case -5 -> "Db major / Bb minor";
                case -4 -> "Ab major / F minor";
                case -3 -> "Eb major / C minor";
                case -2 -> "Bb major / G minor";
                case -1 -> "F major / D minor";
                case 0  -> "C major / A minor";
                case 1  -> "G major / E minor";
                case 2  -> "D major / B minor";
                case 3  -> "A major / F# minor";
                case 4  -> "E major / C# minor";
                case 5  -> "B major / G# minor";
                case 6  -> "F# major / D# minor";
                case 7  -> "C# major / A# minor";
                default -> f + " sharps/flats";
            };
        } catch (NumberFormatException e) {
            return "C major";
        }
    }
}
