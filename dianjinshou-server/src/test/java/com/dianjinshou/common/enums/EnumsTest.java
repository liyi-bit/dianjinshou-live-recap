package com.dianjinshou.common.enums;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class EnumsTest {

    @Test
    void platform_has3Values() {
        assertEquals(3, Platform.values().length);
        assertEquals("douyin", Platform.DOUYIN.getCode());
        assertEquals("抖音", Platform.DOUYIN.getDesc());
    }

    @Test
    void role_has4Values() {
        assertEquals(4, Role.values().length);
        assertEquals("super_admin", Role.SUPER_ADMIN.getCode());
        assertEquals("anchor", Role.ANCHOR.getCode());
    }

    @Test
    void accountType_has3Values() {
        assertEquals(3, AccountType.values().length);
        assertEquals("own", AccountType.OWN.getCode());
    }

    @Test
    void recapType_has2Values() {
        assertEquals(2, RecapType.values().length);
    }

    @Test
    void analysisStatus_has5Values() {
        assertEquals(5, AnalysisStatus.values().length);
        assertEquals("pending", AnalysisStatus.PENDING.getCode());
    }

    @Test
    void clipCategory_has14Values() {
        assertEquals(14, ClipCategory.values().length);
        assertEquals("RETENTION", ClipCategory.RETENTION.getCode());
        assertEquals("留人切片", ClipCategory.RETENTION.getDesc());
        assertEquals("OTHER", ClipCategory.OTHER.getCode());
    }

    @Test
    void recordingStatus_has5Values() {
        assertEquals(5, RecordingStatus.values().length);
        assertEquals("recording", RecordingStatus.RECORDING.getCode());
    }

    @Test
    void assistantType_has3Values() {
        assertEquals(3, AssistantType.values().length);
        assertEquals("operation", AssistantType.OPERATION.getCode());
    }

    @Test
    void iEnum_getValue_returnsCode() {
        assertEquals("douyin", Platform.DOUYIN.getValue());
        assertEquals("super_admin", Role.SUPER_ADMIN.getValue());
        assertEquals("RETENTION", ClipCategory.RETENTION.getValue());
    }

    // ========== QA 增量: 边界测试 ==========

    @Test
    void platform_allDescsNonEmpty() {
        for (Platform p : Platform.values()) {
            assertNotNull(p.getCode());
            assertFalse(p.getCode().isEmpty());
            assertNotNull(p.getDesc());
            assertFalse(p.getDesc().isEmpty());
        }
    }

    @Test
    void platform_specificValues() {
        assertEquals("抖音", Platform.DOUYIN.getDesc());
        assertEquals("kuaishou", Platform.KUAISHOU.getCode());
        assertEquals("快手", Platform.KUAISHOU.getDesc());
        assertEquals("shipinhao", Platform.SHIPINHAO.getCode());
        assertEquals("视频号", Platform.SHIPINHAO.getDesc());
    }

    @Test
    void platform_getValue() {
        for (Platform p : Platform.values()) {
            assertEquals(p.getCode(), p.getValue());
        }
    }

    @Test
    void role_specificValues() {
        assertEquals("超管", Role.SUPER_ADMIN.getDesc());
        assertEquals("admin", Role.ADMIN.getCode());
        assertEquals("组织管理员", Role.ADMIN.getDesc());
        assertEquals("operator", Role.OPERATOR.getCode());
        assertEquals("运营", Role.OPERATOR.getDesc());
        assertEquals("anchor", Role.ANCHOR.getCode());
        assertEquals("主播", Role.ANCHOR.getDesc());
    }

    @Test
    void role_getValue() {
        for (Role r : Role.values()) {
            assertEquals(r.getCode(), r.getValue());
        }
    }

    @Test
    void accountType_specificValues() {
        assertEquals("own", AccountType.OWN.getCode());
        assertEquals("自有", AccountType.OWN.getDesc());
        assertEquals("competitor", AccountType.COMPETITOR.getCode());
        assertEquals("竞品", AccountType.COMPETITOR.getDesc());
        assertEquals("industry", AccountType.INDUSTRY.getCode());
        assertEquals("同行业", AccountType.INDUSTRY.getDesc());
    }

    @Test
    void accountType_getValue() {
        for (AccountType at : AccountType.values()) {
            assertEquals(at.getCode(), at.getValue());
        }
    }

    @Test
    void recapType_specificValues() {
        assertEquals("full", RecapType.FULL.getCode());
        assertEquals("整场复盘", RecapType.FULL.getDesc());
        assertEquals("clip", RecapType.CLIP.getCode());
        assertEquals("切片复盘", RecapType.CLIP.getDesc());
    }

    @Test
    void recapType_getValue() {
        for (RecapType rt : RecapType.values()) {
            assertEquals(rt.getCode(), rt.getValue());
        }
    }

    @Test
    void analysisStatus_specificValues() {
        assertEquals("排队分析中", AnalysisStatus.PENDING.getDesc());
        assertEquals("asr_processing", AnalysisStatus.ASR_PROCESSING.getCode());
        assertEquals("语音转写中", AnalysisStatus.ASR_PROCESSING.getDesc());
        assertEquals("ai_processing", AnalysisStatus.AI_PROCESSING.getCode());
        assertEquals("AI分析中", AnalysisStatus.AI_PROCESSING.getDesc());
        assertEquals("completed", AnalysisStatus.COMPLETED.getCode());
        assertEquals("分析完成", AnalysisStatus.COMPLETED.getDesc());
        assertEquals("failed", AnalysisStatus.FAILED.getCode());
        assertEquals("分析失败", AnalysisStatus.FAILED.getDesc());
    }

    @Test
    void analysisStatus_getValue() {
        for (AnalysisStatus as : AnalysisStatus.values()) {
            assertEquals(as.getCode(), as.getValue());
        }
    }

    @Test
    void clipCategory_allDescsNonEmpty() {
        for (ClipCategory cc : ClipCategory.values()) {
            assertNotNull(cc.getCode());
            assertFalse(cc.getCode().isEmpty());
            assertNotNull(cc.getDesc());
            assertFalse(cc.getDesc().isEmpty());
        }
    }

    @Test
    void clipCategory_codeMatchesName() {
        for (ClipCategory cc : ClipCategory.values()) {
            assertEquals(cc.name(), cc.getCode());
        }
    }

    @Test
    void clipCategory_specificDescs() {
        assertEquals("留人切片", ClipCategory.RETENTION.getDesc());
        assertEquals("优质话术", ClipCategory.QUALITY_SPEECH.getDesc());
        assertEquals("营销塑品", ClipCategory.MARKETING.getDesc());
        assertEquals("互动切片", ClipCategory.INTERACTION.getDesc());
        assertEquals("粉团切片", ClipCategory.FAN_CLUB.getDesc());
        assertEquals("表现力切片", ClipCategory.EXPRESSION.getDesc());
        assertEquals("规避违规", ClipCategory.COMPLIANCE.getDesc());
        assertEquals("观点切片", ClipCategory.VIEWPOINT.getDesc());
        assertEquals("举例切片", ClipCategory.EXAMPLE.getDesc());
        assertEquals("引导私域", ClipCategory.PRIVATE_DOMAIN.getDesc());
        assertEquals("人设切片", ClipCategory.PERSONA.getDesc());
        assertEquals("循环话术", ClipCategory.LOOP_SPEECH.getDesc());
        assertEquals("憋单切片", ClipCategory.BIE_DAN.getDesc());
        assertEquals("其他切片", ClipCategory.OTHER.getDesc());
    }

    @Test
    void clipCategory_getValue() {
        for (ClipCategory cc : ClipCategory.values()) {
            assertEquals(cc.getCode(), cc.getValue());
        }
    }

    @Test
    void recordingStatus_specificValues() {
        assertEquals("monitoring", RecordingStatus.MONITORING.getCode());
        assertEquals("监控中", RecordingStatus.MONITORING.getDesc());
        assertEquals("录制中", RecordingStatus.RECORDING.getDesc());
        assertEquals("completed", RecordingStatus.COMPLETED.getCode());
        assertEquals("已完成", RecordingStatus.COMPLETED.getDesc());
        assertEquals("failed", RecordingStatus.FAILED.getCode());
        assertEquals("失败", RecordingStatus.FAILED.getDesc());
        assertEquals("deleted", RecordingStatus.DELETED.getCode());
        assertEquals("已删除", RecordingStatus.DELETED.getDesc());
    }

    @Test
    void recordingStatus_getValue() {
        for (RecordingStatus rs : RecordingStatus.values()) {
            assertEquals(rs.getCode(), rs.getValue());
        }
    }

    @Test
    void valueOf_allEnums_works() {
        assertDoesNotThrow(() -> Platform.valueOf("DOUYIN"));
        assertDoesNotThrow(() -> Role.valueOf("SUPER_ADMIN"));
        assertDoesNotThrow(() -> AccountType.valueOf("OWN"));
        assertDoesNotThrow(() -> RecapType.valueOf("FULL"));
        assertDoesNotThrow(() -> AnalysisStatus.valueOf("PENDING"));
        assertDoesNotThrow(() -> ClipCategory.valueOf("RETENTION"));
        assertDoesNotThrow(() -> RecordingStatus.valueOf("MONITORING"));
    }

    @Test
    void uniqueCodesWithinEnum() {
        assertEquals(
            Platform.values().length,
            Arrays.stream(Platform.values()).map(Platform::getCode).distinct().count()
        );
        assertEquals(
            Role.values().length,
            Arrays.stream(Role.values()).map(Role::getCode).distinct().count()
        );
        assertEquals(
            AccountType.values().length,
            Arrays.stream(AccountType.values()).map(AccountType::getCode).distinct().count()
        );
        assertEquals(
            ClipCategory.values().length,
            Arrays.stream(ClipCategory.values()).map(ClipCategory::getCode).distinct().count()
        );
        assertEquals(
            RecordingStatus.values().length,
            Arrays.stream(RecordingStatus.values()).map(RecordingStatus::getCode).distinct().count()
        );
    }
}
