package com.dianjinshou.modules.storage.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.comparison.service.ComparisonService;
import com.dianjinshou.modules.storage.entity.CloudFile;
import com.dianjinshou.modules.storage.vo.CloudFileVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudSpaceServiceTest {

    @Mock
    private CloudFileService cloudFileService;
    @Mock
    private ComparisonService comparisonService;

    private CloudSpaceService service;

    @BeforeAll
    static void initTableInfo() {
        try {
            MapperBuilderAssistant a = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a.setCurrentNamespace("com.dianjinshou.modules.storage.mapper.CloudFileMapper2");
            TableInfoHelper.initTableInfo(a, CloudFile.class);
        } catch (Exception ignored) { }
    }

    @BeforeEach
    void setUp() {
        service = new CloudSpaceService(cloudFileService, comparisonService);
        setCurrentUser(1L, "operator", 100L);
    }

    private void setCurrentUser(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void listByType_recordings_success() {
        CloudFile cf = new CloudFile();
        cf.setId(1L);
        cf.setFileName("recording.mp4");
        cf.setFileType("recording");
        cf.setFileSize(1000L);
        cf.setStatus("active");
        cf.setOrgId(100L);

        Page<CloudFileVO> page = new Page<>(1, 20, 1);
        page.setRecords(Arrays.asList(CloudFileVO.fromEntity(cf)));
        when(cloudFileService.listBusinessFiles(eq("full_recap"), eq(1), eq(20), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(page);

        Page<CloudFileVO> result = service.listByType("recording", 1, 20, null, null);

        assertEquals(1, result.getRecords().size());
        assertEquals("recording.mp4", result.getRecords().get(0).getFileName());
    }

    @Test
    void listByType_withKeyword_success() {
        Page<CloudFileVO> emptyPage = new Page<>(1, 20, 0);
        emptyPage.setRecords(Collections.emptyList());
        when(cloudFileService.listBusinessFiles(eq("clip_recap"), eq(1), eq(20), eq("test"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(emptyPage);

        Page<CloudFileVO> result = service.listByType("clip", 1, 20, "test", "size");

        assertEquals(0, result.getRecords().size());
    }

    @Test
    void batchDelete_success() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        doNothing().when(cloudFileService).deleteFile(any(Long.class));

        service.batchDelete(ids);

        verify(cloudFileService, times(3)).deleteFile(any(Long.class));
    }

    @Test
    void batchDelete_emptyList_throws() {
        assertThrows(BusinessException.class, () -> service.batchDelete(Collections.emptyList()));
    }

    @Test
    void batchDelete_tooMany_throws() {
        List<Long> ids = new java.util.ArrayList<Long>();
        for (int i = 0; i < 51; i++) ids.add((long) i);

        assertThrows(BusinessException.class, () -> service.batchDelete(ids));
    }

    @Test
    void batchDownloadUrls_success() {
        when(cloudFileService.getDownloadUrl(1L)).thenReturn("url1");
        when(cloudFileService.getDownloadUrl(2L)).thenReturn("url2");

        List<String> urls = service.batchDownloadUrls(Arrays.asList(1L, 2L));

        assertEquals(2, urls.size());
        assertEquals("url1", urls.get(0));
    }
}
