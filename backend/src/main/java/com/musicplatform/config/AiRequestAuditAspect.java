package com.musicplatform.config;

import com.musicplatform.dto.ApiResponse;
import com.musicplatform.dto.GenerateRequest;
import com.musicplatform.dto.ReviseRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Aspect
@Component
public class AiRequestAuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AiRequestAuditAspect.class);
    private static final int TEXT_LIMIT = 120;

    @Around("execution(* com.musicplatform.controller.MusicController.generate(..)) || " +
            "execution(* com.musicplatform.controller.MusicController.revise(..))")
    public Object auditMusicGenerationRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String timestamp = Instant.now().toString();
        String endpoint = currentEndpoint();
        String requestSummary = summarizeArgs(joinPoint.getArgs());

        try {
            Object result = joinPoint.proceed();
            long elapsedMs = System.currentTimeMillis() - start;
            log.info("AI_REQUEST_AUDIT | timestamp={} | endpoint={} | status=success | elapsed_ms={} | request={} | result={}",
                    timestamp, endpoint, elapsedMs, requestSummary, summarizeResult(result));
            return result;
        } catch (Throwable ex) {
            long elapsedMs = System.currentTimeMillis() - start;
            log.warn("AI_REQUEST_AUDIT | timestamp={} | endpoint={} | status=failed | elapsed_ms={} | request={} | error={}",
                    timestamp, endpoint, elapsedMs, requestSummary, sanitize(ex.getMessage()));
            throw ex;
        }
    }

    private String currentEndpoint() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        HttpServletRequest request = attrs.getRequest();
        return request.getMethod() + " " + request.getRequestURI();
    }

    private String summarizeArgs(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof GenerateRequest request) {
                return summarizeGenerateRequest(request);
            }
            if (arg instanceof ReviseRequest request) {
                return summarizeReviseRequest(request);
            }
        }
        return "{}";
    }

    private String summarizeGenerateRequest(GenerateRequest request) {
        return "{type=generate" +
                ", user_prompt=" + quote(shorten(request.getUserPrompt())) +
                ", style=" + quote(request.getStyle()) +
                ", mood=" + quote(request.getMood()) +
                ", tempo=" + quote(request.getTempo()) +
                ", duration_seconds=" + request.getDurationSeconds() +
                ", instruments=" + quote(joinList(request.getInstruments())) +
                ", track_id=" + quote(request.getTrackId()) +
                ", track_name=" + quote(request.getTrackName()) +
                ", version_label=" + quote(request.getVersionLabel()) +
                "}";
    }

    private String summarizeReviseRequest(ReviseRequest request) {
        return "{type=revise" +
                ", version_id=" + quote(request.getVersionId()) +
                ", feedback=" + quote(shorten(request.getFeedback())) +
                ", version_label=" + quote(request.getVersionLabel()) +
                "}";
    }

    @SuppressWarnings("unchecked")
    private String summarizeResult(Object result) {
        Object body = result;
        if (result instanceof ResponseEntity<?> responseEntity) {
            body = responseEntity.getBody();
        }
        if (body instanceof ApiResponse<?> apiResponse) {
            body = apiResponse.getData();
        }
        if (body instanceof Map<?, ?> map) {
            return "{version_id=" + quote(value(map, "version_id")) +
                    ", parent_version_id=" + quote(value(map, "parent_version_id")) +
                    ", mock=" + value(map, "mock") +
                    ", audio_url=" + quote(value(map, "audio_url")) +
                    ", midi_url=" + quote(value(map, "midi_url")) +
                    "}";
        }
        return "{data=" + quote(shorten(String.valueOf(body))) + "}";
    }

    private String value(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(",", values);
    }

    private String shorten(String value) {
        String cleaned = sanitize(value);
        if (cleaned == null || cleaned.length() <= TEXT_LIMIT) {
            return cleaned;
        }
        return cleaned.substring(0, TEXT_LIMIT) + "...";
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\r", " ").replace("\n", " ").replace("|", "/").trim();
    }

    private String quote(String value) {
        return value == null || value.isEmpty() ? "\"\"" : "\"" + sanitize(value) + "\"";
    }
}
