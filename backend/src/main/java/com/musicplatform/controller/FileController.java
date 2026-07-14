package com.musicplatform.controller;

import com.musicplatform.dto.ApiResponse;
import com.musicplatform.model.UploadedFile;
import com.musicplatform.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * 文件上传 REST API。
 * 支持 txt / docx / xlsx / MusicXML 格式上传，内容自动提取。
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@Tag(name = "文件上传", description = "上传参考文件（txt/docx/xlsx/musicxml），内容自动提取并注入 prompt")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * POST /api/upload — 上传参考文件。
     */
    @Operation(
            summary = "上传参考文件",
            description = "上传 txt / docx / xlsx / MusicXML 文件，后端自动提取文本内容。"
    )
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> upload(
            @Parameter(description = "关联的版本 ID（选填）")
            @RequestParam(value = "version_id", required = false) String versionId,
            @Parameter(description = "关联的 Track ID（选填，用于生成前查找文件）")
            @RequestParam(value = "track_id", required = false) String trackId,
            @Parameter(description = "文件")
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("文件为空"));
        }

        log.info("收到上传请求 | name={} | size={} | version={} | track={}",
                file.getOriginalFilename(), file.getSize(), versionId, trackId);

        try {
            UploadedFile entity = fileService.upload(
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed",
                    file.getBytes(),
                    versionId,
                    trackId);

            Map<String, Object> data = buildUploadResponse(entity);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(data));

        } catch (IOException e) {
            log.error("上传失败 | name={} | error={}", file.getOriginalFilename(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * GET /api/uploads/{version_id} — 获取某版本关联的所有文件。
     */
    @Operation(summary = "获取版本关联文件", description = "查询某版本上传的所有参考文件列表。")
    @GetMapping("/uploads/version/{version_id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVersionFiles(
            @Parameter(description = "版本 ID", example = "v1")
            @PathVariable("version_id") String versionId) {

        List<UploadedFile> files = fileService.getFilesByVersion(versionId);
        List<Map<String, Object>> items = files.stream()
                .map(this::buildUploadResponse)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", items.size());
        result.put("version_id", versionId);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * DELETE /api/uploads/{file_id} — 删除文件。
     */
    @Operation(summary = "删除文件", description = "删除指定 ID 的上传文件。")
    @DeleteMapping("/uploads/{file_id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteFile(
            @Parameter(description = "文件 ID", example = "1")
            @PathVariable("file_id") Long fileId) {

        log.info("删除文件 | id={}", fileId);

        try {
            fileService.deleteFile(fileId);
            return ResponseEntity.ok(ApiResponse.success("文件已删除", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        } catch (IOException e) {
            log.warn("删除文件失败 | id={} | error={}", fileId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.success("文件记录已删除（物理文件清理失败）", null));
        }
    }

    // ===================== 响应构建 =====================

    private Map<String, Object> buildUploadResponse(UploadedFile f) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("file_id", f.getId());
        map.put("original_name", f.getOriginalName());
        map.put("file_type", f.getFileType());
        map.put("extracted_text", f.getExtractedText());
        map.put("extracted_json", f.getExtractedJson());
        map.put("file_size", f.getFileSize());
        map.put("version_id", f.getVersionId());
        map.put("track_id", f.getTrackId());
        map.put("created_at", f.getCreatedAt());
        return map;
    }
}
