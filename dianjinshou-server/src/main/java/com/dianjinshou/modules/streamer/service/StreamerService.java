package com.dianjinshou.modules.streamer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.enums.AccountType;
import com.dianjinshou.common.enums.Platform;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.common.security.DataPermission;
import com.dianjinshou.common.security.OrgScopeHelper;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.streamer.dto.CreateStreamerRequest;
import com.dianjinshou.modules.streamer.dto.StreamerQueryRequest;
import com.dianjinshou.modules.streamer.dto.UpdateStreamerRequest;
import com.dianjinshou.modules.streamer.entity.Streamer;
import com.dianjinshou.modules.streamer.mapper.StreamerMapper;
import com.dianjinshou.modules.streamer.vo.StreamerListVO;
import com.dianjinshou.modules.streamer.vo.StreamerStatsVO;
import com.dianjinshou.modules.streamer.vo.StreamerVO;
import com.dianjinshou.modules.recording.entity.Recording;
import com.dianjinshou.modules.recording.mapper.RecordingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class StreamerService {

    private final StreamerMapper streamerMapper;
    private final RecordingMapper recordingMapper;

    public StreamerService(StreamerMapper streamerMapper, RecordingMapper recordingMapper) {
        this.streamerMapper = streamerMapper;
        this.recordingMapper = recordingMapper;
    }

    /** 按真实录制数动态填充 todaySessions（今日场次不再依赖 streamers 表的脏数据）*/
    private int countTodaySessions(Long streamerId) {
        if (streamerId == null) return 0;
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);
        Long count = recordingMapper.selectCount(
                new LambdaQueryWrapper<Recording>()
                        .eq(Recording::getStreamerId, streamerId)
                        .ge(Recording::getStartTime, todayStart)
                        .lt(Recording::getStartTime, tomorrowStart));
        return count == null ? 0 : count.intValue();
    }

    /** 按真实录制数动态填充 totalSessions */
    private int countTotalSessions(Long streamerId) {
        if (streamerId == null) return 0;
        Long count = recordingMapper.selectCount(
                new LambdaQueryWrapper<Recording>().eq(Recording::getStreamerId, streamerId));
        return count == null ? 0 : count.intValue();
    }

    /** v1.1.0：每个用户最多添加 20 个主播（防止无限刷） */
    private static final int MAX_STREAMERS_PER_USER = 20;

    @Transactional
    @DataPermission
    public StreamerVO create(CreateStreamerRequest req) {
        Long userId = SecurityContextHelper.currentUserId();
        Long orgId = SecurityContextHelper.currentOrgId();

        // v1.1.0：数量上限校验（软硬兜底，桌面端 UI 会先拦截一次）
        if (userId != null) {
            LambdaQueryWrapper<Streamer> cntWrapper = new LambdaQueryWrapper<>();
            cntWrapper.eq(Streamer::getUserId, userId);
            Long myCount = streamerMapper.selectCount(cntWrapper);
            if (myCount != null && myCount >= MAX_STREAMERS_PER_USER) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "已达到主播数量上限（" + MAX_STREAMERS_PER_USER + " 个），请先删除不需要的再添加");
            }
        }

        // Duplicate check: same orgId + platform + (accountId OR secUid)
        Platform platform = parsePlatform(req.getPlatform());
        if (req.getAccountId() != null && platform != null) {
            LambdaQueryWrapper<Streamer> dupWrapper = new LambdaQueryWrapper<>();
            dupWrapper.eq(Streamer::getOrgId, orgId);
            dupWrapper.eq(Streamer::getPlatform, platform);
            dupWrapper.eq(Streamer::getAccountId, req.getAccountId().trim());
            Long count = streamerMapper.selectCount(dupWrapper);
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "该平台下已存在相同账号ID的直播间，请勿重复添加");
            }
        }
        // Secondary duplicate check by sec_uid (same streamer may have different web_rid aliases)
        if (req.getSecUid() != null && !req.getSecUid().trim().isEmpty() && platform != null) {
            LambdaQueryWrapper<Streamer> secUidWrapper = new LambdaQueryWrapper<>();
            secUidWrapper.eq(Streamer::getOrgId, orgId);
            secUidWrapper.eq(Streamer::getPlatform, platform);
            secUidWrapper.eq(Streamer::getSecUid, req.getSecUid().trim());
            Long count = streamerMapper.selectCount(secUidWrapper);
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "该主播已添加过（通过其他ID形式），请勿重复添加");
            }
        }

        Streamer streamer = new Streamer();
        streamer.setUserId(userId);
        streamer.setOrgId(orgId);
        streamer.setPlatform(parsePlatform(req.getPlatform()));
        streamer.setAccountId(req.getAccountId());
        streamer.setAnchorName(req.getAnchorName() != null ? req.getAnchorName()
                : (req.getAccountId() != null ? req.getAccountId() : "未命名主播"));
        streamer.setAnchorAvatar(req.getAnchorAvatar());
        streamer.setSecUid(req.getSecUid());
        streamer.setIndustryId(req.getIndustryId());
        streamer.setAccountType(parseAccountType(req.getAccountType()));
        streamer.setLiveRoomMode(req.getLiveRoomMode());
        streamer.setAccountStage(req.getAccountStage());
        streamer.setAccountLevel(req.getAccountLevel());
        streamer.setTrafficStructure(req.getTrafficStructure());
        streamer.setBroadcastTimeStart(req.getBroadcastTimeStart());
        streamer.setBroadcastTimeEnd(req.getBroadcastTimeEnd());
        streamer.setAccountIssue(req.getAccountIssue());
        streamer.setDefaultLanguage(req.getDefaultLanguage() != null ? req.getDefaultLanguage() : "中文通用");
        streamer.setIsMonitoring(true);
        streamer.setAutoAiAnalysis(true);
        streamer.setCloudSyncEnabled(Boolean.TRUE.equals(req.getCloudSyncEnabled()));
        streamer.setTotalSessions(0);
        streamer.setTodaySessions(0);
        streamer.setStatus(1);

        streamerMapper.insert(streamer);

        return StreamerVO.fromEntity(streamer);
    }

    @DataPermission
    public PageResult<StreamerListVO> list(StreamerQueryRequest req) {
        int page = req.getPageOrDefault();
        int size = req.getSizeOrDefault();

        LambdaQueryWrapper<Streamer> wrapper = new LambdaQueryWrapper<>();
        OrgScopeHelper.applyOrgScope(wrapper, Streamer::getOrgId);

        if (req.getKeyword() != null && !req.getKeyword().trim().isEmpty()) {
            wrapper.like(Streamer::getAnchorName, req.getKeyword().trim());
        }
        if (req.getAccountType() != null && !req.getAccountType().trim().isEmpty()) {
            wrapper.eq(Streamer::getAccountType, parseAccountType(req.getAccountType()));
        }
        if (req.getPlatform() != null && !req.getPlatform().trim().isEmpty()) {
            wrapper.eq(Streamer::getPlatform, parsePlatform(req.getPlatform()));
        }
        if (req.getIsMonitoring() != null) {
            wrapper.eq(Streamer::getIsMonitoring, req.getIsMonitoring());
        }

        wrapper.orderByDesc(Streamer::getCreatedAt);

        Page<Streamer> pageParam = new Page<>(page, size);
        Page<Streamer> result = streamerMapper.selectPage(pageParam, wrapper);

        List<StreamerListVO> items = new ArrayList<>();
        for (Streamer s : result.getRecords()) {
            StreamerListVO vo = StreamerListVO.fromEntity(s);
            vo.setTodaySessions(countTodaySessions(s.getId()));
            vo.setTotalSessions(countTotalSessions(s.getId()));
            items.add(vo);
        }

        return PageResult.of(items, result.getTotal(), page, size);
    }

    @DataPermission
    public StreamerVO detail(Long id) {
        Streamer streamer = streamerMapper.selectById(id);
        if (streamer == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "直播间不存在");
        }
        checkOwnership(streamer);
        StreamerVO vo = StreamerVO.fromEntity(streamer);
        vo.setTodaySessions(countTodaySessions(id));
        vo.setTotalSessions(countTotalSessions(id));
        return vo;
    }

    @Transactional
    @DataPermission
    public StreamerVO update(Long id, UpdateStreamerRequest req) {
        Streamer streamer = streamerMapper.selectById(id);
        if (streamer == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "直播间不存在");
        }
        checkOwnership(streamer);

        if (req.getPlatform() != null) {
            streamer.setPlatform(parsePlatform(req.getPlatform()));
        }
        if (req.getAccountId() != null) {
            streamer.setAccountId(req.getAccountId());
        }
        if (req.getAnchorName() != null) {
            streamer.setAnchorName(req.getAnchorName());
        }
        if (req.getAnchorAvatar() != null) {
            streamer.setAnchorAvatar(req.getAnchorAvatar());
        }
        if (req.getSecUid() != null) {
            streamer.setSecUid(req.getSecUid());
        }
        if (req.getIndustryId() != null) {
            streamer.setIndustryId(req.getIndustryId());
        }
        if (req.getAccountType() != null) {
            streamer.setAccountType(parseAccountType(req.getAccountType()));
        }
        if (req.getLiveRoomMode() != null) {
            streamer.setLiveRoomMode(req.getLiveRoomMode());
        }
        if (req.getAccountStage() != null) {
            streamer.setAccountStage(req.getAccountStage());
        }
        if (req.getAccountLevel() != null) {
            streamer.setAccountLevel(req.getAccountLevel());
        }
        if (req.getTrafficStructure() != null) {
            streamer.setTrafficStructure(req.getTrafficStructure());
        }
        if (req.getBroadcastTimeStart() != null) {
            streamer.setBroadcastTimeStart(req.getBroadcastTimeStart());
        }
        if (req.getBroadcastTimeEnd() != null) {
            streamer.setBroadcastTimeEnd(req.getBroadcastTimeEnd());
        }
        if (req.getAccountIssue() != null) {
            streamer.setAccountIssue(req.getAccountIssue());
        }
        if (req.getDefaultLanguage() != null) {
            streamer.setDefaultLanguage(req.getDefaultLanguage());
        }
        if (req.getIsMonitoring() != null) {
            streamer.setIsMonitoring(req.getIsMonitoring());
        }
        if (req.getAutoAiAnalysis() != null) {
            streamer.setAutoAiAnalysis(req.getAutoAiAnalysis());
        }
        if (req.getCloudSyncEnabled() != null) {
            streamer.setCloudSyncEnabled(req.getCloudSyncEnabled());
        }

        streamerMapper.updateById(streamer);

        return StreamerVO.fromEntity(streamer);
    }

    @Transactional
    @DataPermission
    public int delete(Long id) {
        Streamer streamer = streamerMapper.selectById(id);
        if (streamer == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "直播间不存在");
        }
        checkOwnership(streamer);

        streamerMapper.deleteById(id);

        // TODO: cascade cleanup of recordings in later tasks
        return 0;
    }

    @Transactional
    @DataPermission
    public void startMonitor(Long id) {
        Streamer streamer = streamerMapper.selectById(id);
        if (streamer == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "直播间不存在");
        }
        checkOwnership(streamer);

        streamer.setIsMonitoring(true);
        streamerMapper.updateById(streamer);
    }

    @Transactional
    @DataPermission
    public void stopMonitor(Long id) {
        Streamer streamer = streamerMapper.selectById(id);
        if (streamer == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "直播间不存在");
        }
        checkOwnership(streamer);

        streamer.setIsMonitoring(false);
        streamerMapper.updateById(streamer);
    }

    @DataPermission
    public StreamerStatsVO stats() {
        LambdaQueryWrapper<Streamer> baseWrapper = new LambdaQueryWrapper<>();
        OrgScopeHelper.applyOrgScope(baseWrapper, Streamer::getOrgId);

        List<Streamer> all = streamerMapper.selectList(baseWrapper);

        StreamerStatsVO vo = new StreamerStatsVO();
        vo.setTotal(all.size());

        long monitoring = 0;
        long ownCount = 0;
        long competitorCount = 0;
        long industryCount = 0;

        for (Streamer s : all) {
            if (Boolean.TRUE.equals(s.getIsMonitoring())) {
                monitoring++;
            }
            if (s.getAccountType() != null) {
                switch (s.getAccountType()) {
                    case OWN:
                        ownCount++;
                        break;
                    case COMPETITOR:
                        competitorCount++;
                        break;
                    case INDUSTRY:
                        industryCount++;
                        break;
                }
            }
        }

        vo.setMonitoring(monitoring);
        vo.setRecording(0); // TODO: count active recordings in later tasks
        vo.setOwnCount(ownCount);
        vo.setCompetitorCount(competitorCount);
        vo.setIndustryCount(industryCount);

        return vo;
    }

    private void checkOwnership(Streamer streamer) {
        String role = SecurityContextHelper.currentRole();
        if ("super_admin".equals(role)) {
            return;
        }
        Long currentOrgId = SecurityContextHelper.currentOrgId();
        if (currentOrgId == null || !currentOrgId.equals(streamer.getOrgId())) {
            throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS, "无权访问该直播间");
        }
    }

    private Platform parsePlatform(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (Platform p : Platform.values()) {
            if (p.getCode().equals(value)) {
                return p;
            }
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的平台: " + value);
    }

    private AccountType parseAccountType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return AccountType.OWN;
        }
        for (AccountType t : AccountType.values()) {
            if (t.getCode().equals(value)) {
                return t;
            }
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的账号类型: " + value);
    }
}
