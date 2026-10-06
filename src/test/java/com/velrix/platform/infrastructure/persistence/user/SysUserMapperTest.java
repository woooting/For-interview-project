package com.velrix.platform.infrastructure.persistence.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velrix.platform.domain.user.SysUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class SysUserMapperTest {

	@Autowired
	private SysUserMapper sysUserMapper;

	@Test
	void selectById_returnsAdminFromSeedData() {
		SysUser user = sysUserMapper.selectById(1001L);

		assertNotNull(user);
		assertEquals("admin", user.getUsername());
		assertEquals("系统管理员", user.getDisplayName());
		assertTrue(user.getEnabled());
	}

	@Test
	void selectOne_byUsernameNorm() {
		SysUser user = sysUserMapper.selectOne(
				new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsernameNorm, "admin"));

		assertNotNull(user);
		assertEquals(1001L, user.getId());
	}
}
