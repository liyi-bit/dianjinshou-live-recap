package com.dianjinshou.modules.shortvideo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.OrgScopeHelper;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.recap.task.AnalysisTaskMessage;
import com.dianjinshou.modules.recap.task.AnalysisTaskProducer;
import com.dianjinshou.modules.shortvideo.dto.ExtractCopywritingRequest;
import com.dianjinshou.modules.shortvideo.entity.VideoCopywriting;
import com.dianjinshou.modules.shortvideo.mapper.VideoCopywritingMapper;
import com.dianjinshou.modules.shortvideo.vo.VideoCopywritingVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class CopywritingExtractService {

    private static final Logger log = LoggerFactory.getLogger(CopywritingExtractService.class);
    private static final Set<String> VALID_SOURCE_TYPES = new HashSet<String>(
            Arrays.asList("url", "local", "recording"));

    private final VideoCopywritingMapper videoCopywritingMapper;
    private final AnalysisTaskProducer analysisTaskProducer;

    public CopywritingExtractService(VideoCopywritingMapper videoCopywritingMapper,
                                     AnalysisTaskProducer analysisTaskProducer) {
        this.videoCopywritingMapper = videoCopywritingMapper;
        this.analysisTaskProducer = analysisTaskProducer;
    }

    public VideoCopywritingVO extractCopywriting(ExtractCopywritingRequest request) {
        Long userId = SecurityContextHelper.currentUserId();
        Long orgId = SecurityContextHelper.currentOrgId();
        if (userId == null || orgId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (!VALID_SOURCE_TYPES.contains(request.getSourceType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的来源类型");
        }

        if ("url".equals(request.getSourceType())) {
            if (request.getSourceUrl() == null || request.getSourceUrl().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "URL模式需提供视频链接");
            }
        } else {
            if (request.getStorageKey() == null || request.getStorageKey().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "本地/录制模式需提供存储key");
            }
        }

        VideoCopywriting entity = new VideoCopywriting();
        entity.setUserId(userId);
        entity.setOrgId(orgId);
        entity.setSourceType(request.getSourceType());
        entity.setSourceUrl(request.getSourceUrl());
        entity.setStorageKey(request.getStorageKey());
        entity.setTitle(request.getTitle());
        entity.setWordCount(0);
        entity.setCopyCount(0);
        entity.setStatus("pending");
        videoCopywritingMapper.insert(entity);

        // Send async message for ASR + AI polishing
        AnalysisTaskMessage msg = new AnalysisTaskMessage();
        msg.setTaskId(entity.getId());
        msg.setType("COPYWRITING_EXTRACT");
        msg.setPriority(5);
        analysisTaskProducer.send(msg);

        log.info("Copywriting extract initiated: id={}, sourceType={}", entity.getId(), request.getSourceType());
        return VideoCopywritingVO.fromEntity(entity);
    }

    public Page<VideoCopywritingVO> listCopywriting(int page, int size, String status) {
        LambdaQueryWrapper<VideoCopywriting> query = new LambdaQueryWrapper<>();
        OrgScopeHelper.applyOrgScope(query, VideoCopywriting::getOrgId);

        if (status != null && !status.isEmpty()) {
            query.eq(VideoCopywriting::getStatus, status);
        }
        query.orderByDesc(VideoCopywriting::getCreatedAt);

        Page<VideoCopywriting> entityPage = videoCopywritingMapper.selectPage(new Page<>(page, size), query);
        Page<VideoCopywritingVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(new ArrayList<VideoCopywritingVO>());
        for (VideoCopywriting vc : entityPage.getRecords()) {
            voPage.getRecords().add(VideoCopywritingVO.fromEntity(vc));
        }
        return voPage;
    }

    public VideoCopywritingVO getCopywriting(Long id) {
        VideoCopywriting entity = videoCopywritingMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文案不存在");
        }
        validateOrgAccess(entity);
        return VideoCopywritingVO.fromEntity(entity);
    }

    public void recordCopy(Long id) {
        VideoCopywriting entity = videoCopywritingMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文案不存在");
        }
        validateOrgAccess(entity);

        LambdaUpdateWrapper<VideoCopywriting> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(VideoCopywriting::getId, id)
                .set(VideoCopywriting::getCopyCount, entity.getCopyCount() + 1);
        videoCopywritingMapper.update(null, wrapper);
    }

    public void deleteCopywriting(Long id) {
        VideoCopywriting entity = videoCopywritingMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文案不存在");
        }
        validateOrgAccess(entity);
        videoCopywritingMapper.deleteById(id);
        log.info("Copywriting deleted: id={}", id);
    }

    private void validateOrgAccess(VideoCopywriting entity) {
        Long orgId = SecurityContextHelper.currentOrgId();
        String role = SecurityContextHelper.currentRole();
        if (!"super_admin".equals(role) && orgId != null && !orgId.equals(entity.getOrgId())) {
            throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS);
        }
    }
}
