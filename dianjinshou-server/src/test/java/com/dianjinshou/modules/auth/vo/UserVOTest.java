package com.dianjinshou.modules.auth.vo;

import com.dianjinshou.modules.auth.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserVOTest {

    @Test
    void fromEntity_masksPhone() {
        User user = new User();
        user.setId(1L);
        user.setUsername("test");
        user.setPhone("13800138000");
        user.setRole("admin");
        user.setOrgId(5L);
        user.setVipLevel(3);

        UserVO vo = UserVO.fromEntity(user);

        assertEquals(1L, vo.getId());
        assertEquals("test", vo.getUsername());
        assertEquals("138****8000", vo.getPhone());
        assertEquals("admin", vo.getRole());
        assertEquals(5L, vo.getOrgId());
        assertEquals(3, vo.getVipLevel());
    }

    @Test
    void fromEntity_nullPhone() {
        User user = new User();
        user.setId(1L);
        user.setUsername("test");
        user.setPhone(null);

        UserVO vo = UserVO.fromEntity(user);
        assertNull(vo.getPhone());
    }
}
