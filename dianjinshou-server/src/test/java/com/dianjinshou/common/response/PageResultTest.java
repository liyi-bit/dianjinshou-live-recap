package com.dianjinshou.common.response;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResultTest {

    @Test
    void of_createsCorrectPage() {
        List<String> items = Arrays.asList("a", "b", "c");
        PageResult<String> page = PageResult.of(items, 100, 1, 10);

        assertEquals(items, page.getItems());
        assertEquals(100, page.getTotal());
        assertEquals(1, page.getPage());
        assertEquals(10, page.getSize());
    }

    // ========== QA 增量: 边界测试 ==========

    @Test
    void of_emptyList() {
        PageResult<String> pr = PageResult.of(java.util.Collections.emptyList(), 0, 1, 10);
        assertTrue(pr.getItems().isEmpty());
        assertEquals(0, pr.getTotal());
    }

    @Test
    void getterSetter_allFields() {
        PageResult<Integer> pr = new PageResult<>();
        List<Integer> items = Arrays.asList(1, 2);
        pr.setItems(items);
        pr.setTotal(50);
        pr.setPage(3);
        pr.setSize(10);

        assertEquals(items, pr.getItems());
        assertEquals(50, pr.getTotal());
        assertEquals(3, pr.getPage());
        assertEquals(10, pr.getSize());
    }

    @Test
    void noArgConstructor_defaultValues() {
        PageResult<Object> pr = new PageResult<>();
        assertNull(pr.getItems());
        assertEquals(0, pr.getTotal());
        assertEquals(0, pr.getPage());
        assertEquals(0, pr.getSize());
    }

    @Test
    void constructorEquivalentToOf() {
        List<String> items = Arrays.asList("x", "y");
        PageResult<String> fromCtor = new PageResult<>(items, 200, 5, 50);
        PageResult<String> fromOf = PageResult.of(items, 200, 5, 50);

        assertEquals(fromCtor.getItems(), fromOf.getItems());
        assertEquals(fromCtor.getTotal(), fromOf.getTotal());
        assertEquals(fromCtor.getPage(), fromOf.getPage());
        assertEquals(fromCtor.getSize(), fromOf.getSize());
    }

    @Test
    void longTotal_supported() {
        PageResult<Object> pr = PageResult.of(java.util.Collections.emptyList(), Long.MAX_VALUE, 1, 10);
        assertEquals(Long.MAX_VALUE, pr.getTotal());
    }
}
