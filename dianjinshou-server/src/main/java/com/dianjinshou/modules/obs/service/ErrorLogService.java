package com.dianjinshou.modules.obs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.modules.obs.dto.ReportErrorRequest;
import com.dianjinshou.modules.obs.entity.ErrorLog;
import com.dianjinshou.modules.obs.mapper.ErrorLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ErrorLogService {

    private static final Logger log = LoggerFactory.getLogger(ErrorLogService.class);

    /** 单条 message 最长截断，避免恶意大 payload。 */
    private static final int MAX_MESSAGE = 1024;
    /** 单条 stack 最长截断。 */
    private static final int MAX_STACK = 16 * 1024;

    private final ErrorLogMapper mapper;
    private final ObjectMapper json;

    public ErrorLogService(ErrorLogMapper mapper, ObjectMapper json) {
        this.mapper = mapper;
        this.json = json;
    }

    /**
     * 批量接收客户端上报；失败条目静默吞掉（不让一条错数据拖垮整批）。
     */
    public int ingest(Long userId, Long orgId, String ip, ReportErrorRequest req) {
        if (req == null || req.getItems() == null || req.getItems().isEmpty()) return 0;
        int ok = 0;
        for (ReportErrorRequest.ErrorItem it : req.getItems()) {
            try {
                ErrorLog row = new ErrorLog();
                row.setUserId(userId);
                row.setOrgId(orgId);
                row.setLevel(nullDefault(it.getLevel(), "error"));
                row.setScope(nullDefault(it.getScope(), "unknown"));
                row.setSource(nullDefault(it.getSource(), "desktop-renderer"));
                row.setClientVersion(trimOrNull(it.getClientVersion(), 32));
                row.setPlatform(trimOrNull(it.getPlatform(), 64));
                row.setUserAgent(trimOrNull(it.getUserAgent(), 1024));
                row.setMessage(trimOrNull(it.getMessage(), MAX_MESSAGE));
                row.setStack(trimOrNull(it.getStack(), MAX_STACK));
                row.setRecordingId(it.getRecordingId());
                row.setTaskId(it.getTaskId());
                row.setModelVersion(trimOrNull(it.getModelVersion(), 64));
                if (it.getDetails() != null)     row.setDetails(json.writeValueAsString(it.getDetails()));
                if (it.getBreadcrumbs() != null) row.setBreadcrumbs(json.writeValueAsString(it.getBreadcrumbs()));
                row.setIp(ip);
                row.setOccurredAt(it.getOccurredAt() != null ? it.getOccurredAt() : LocalDateTime.now());
                row.setReceivedAt(LocalDateTime.now());
                mapper.insert(row);
                ok++;
            } catch (Exception e) {
                log.warn("error_logs.ingest failed for one item: {}", e.getMessage());
            }
        }
        return ok;
    }

    /** Admin 查询（管理员后台用）。 */
    public PageResult<ErrorLog> list(String level, String scope, String source,
                                     Long userId, String clientVersion,
                                     LocalDateTime since, LocalDateTime until,
                                     String keyword, int page, int size) {
        LambdaQueryWrapper<ErrorLog> q = new LambdaQueryWrapper<>();
        if (level != null)         q.eq(ErrorLog::getLevel, level);
        if (scope != null)         q.eq(ErrorLog::getScope, scope);
        if (source != null)        q.eq(ErrorLog::getSource, source);
        if (userId != null)        q.eq(ErrorLog::getUserId, userId);
        if (clientVersion != null) q.eq(ErrorLog::getClientVersion, clientVersion);
        if (since != null)         q.ge(ErrorLog::getOccurredAt, since);
        if (until != null)         q.le(ErrorLog::getOccurredAt, until);
        if (keyword != null && !keyword.isEmpty()) {
            q.and(w -> w.like(ErrorLog::getMessage, keyword)
                        .or().like(ErrorLog::getStack, keyword));
        }
        q.orderByDesc(ErrorLog::getOccurredAt);
        Page<ErrorLog> p = new Page<>(page, size);
        mapper.selectPage(p, q);
        PageResult<ErrorLog> r = new PageResult<>();
        r.setItems(p.getRecords() == null ? new ArrayList<>() : p.getRecords());
        r.setTotal(p.getTotal());
        r.setPage((int) p.getCurrent());
        r.setSize((int) p.getSize());
        return r;
    }

    private static String nullDefault(String s, String def) {
        return (s == null || s.isEmpty()) ? def : s;
    }

    private static String trimOrNull(String s, int max) {
        if (s == null || s.isEmpty()) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }
}
