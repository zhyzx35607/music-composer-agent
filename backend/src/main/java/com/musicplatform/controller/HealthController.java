package com.musicplatform.controller;

import com.musicplatform.dto.ApiResponse;
import com.musicplatform.service.MusicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查端点 — 快速判断服务、数据库、文件存储是否正常。
 */
@RestController
@Tag(name = "系统", description = "健康检查与系统状态")
public class HealthController {

    private final MusicService musicService;
    private final DataSource dataSource;
    private final File outputDir;

    public HealthController(MusicService musicService, DataSource dataSource,
                            @org.springframework.beans.factory.annotation.Value("${music-platform.output-dir:./outputs}") String outputPath) {
        this.musicService = musicService;
        this.dataSource = dataSource;
        this.outputDir = new File(outputPath);
    }

    /**
     * GET /api/health — 系统健康检查
     */
    @Operation(summary = "系统健康检查", description = "返回服务状态、数据库连接、版本总数、存储空间等信息。")
    @GetMapping("/api/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> status = new LinkedHashMap<>();

        // 服务状态
        status.put("service", "UP");

        // 数据库状态
        status.put("database", checkDatabase());

        // 版本数量
        status.put("totalVersions", musicService.getVersionCount());

        // 输出目录
        Map<String, Object> storage = new LinkedHashMap<>();
        storage.put("path", outputDir.getAbsolutePath());
        storage.put("exists", outputDir.exists());
        storage.put("writable", outputDir.canWrite());
        if (outputDir.exists()) {
            storage.put("freeSpaceMB", outputDir.getFreeSpace() / 1024 / 1024);
        }
        status.put("storage", storage);

        // 整体健康状态
        boolean healthy = "UP".equals(status.get("database"));
        status.put("healthy", healthy);

        return ResponseEntity.ok(ApiResponse.success(status));
    }

    private String checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(3) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN (" + e.getMessage() + ")";
        }
    }
}
