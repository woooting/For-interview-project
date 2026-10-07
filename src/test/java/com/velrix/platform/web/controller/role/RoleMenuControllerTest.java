package com.velrix.platform.web.controller.role;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.velrix.platform.application.auth.AuthService;
import com.velrix.platform.application.access.MenuAccessService;
import com.velrix.platform.application.access.dto.MenuNodeResponse;
import com.velrix.platform.application.access.dto.UserAccessResponse;
import com.velrix.platform.domain.audit.SysAuditLog;
import com.velrix.platform.infrastructure.persistence.audit.SysAuditLogMapper;
import com.velrix.shared.api.ApiCodes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoleMenuControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthService authService;

	@Autowired
	private MenuAccessService menuAccessService;

	@Autowired
	private SysAuditLogMapper sysAuditLogMapper;

	@Test
	@Transactional
	void admin_replacesPurchaserMenus_lisiAccessChanges_andAuditIsWritten() throws Exception {
		replace(2L, "admin", "[2101]")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(ApiCodes.OK))
				.andExpect(jsonPath("$.data.added", hasSize(0)))
				.andExpect(jsonPath("$.data.removed", hasSize(2)))
				.andExpect(jsonPath("$.data.removed", hasItem(2100)))
				.andExpect(jsonPath("$.data.removed", hasItem(2102)));

		UserAccessResponse access = menuAccessService.loadAccess(1002L);
		assertTrue(access.permCodes().isEmpty());
		assertEquals(1, access.menus().size());
		MenuNodeResponse purchase = access.menus().get(0);
		assertEquals(2100L, purchase.id());
		assertEquals(List.of(2101L), purchase.children().stream().map(MenuNodeResponse::id).toList());

		List<SysAuditLog> logs = sysAuditLogMapper.selectList(new LambdaQueryWrapper<SysAuditLog>()
				.eq(SysAuditLog::getAction, "ROLE_MENU_UPDATE")
				.eq(SysAuditLog::getTargetType, "ROLE")
				.eq(SysAuditLog::getTargetId, 2L)
				.orderByDesc(SysAuditLog::getId));
		assertFalse(logs.isEmpty());
		SysAuditLog latest = logs.get(0);
		assertEquals("admin", latest.getActor());
		assertNotNull(latest.getOccurredAt());
		assertTrue(latest.getSummary().contains("采购员"));
		assertTrue(latest.getSummary().contains("2100"));
		assertTrue(latest.getSummary().contains("2102"));
	}

	@Test
	void lisi_replaceMenus_forbidden() throws Exception {
		replace(2L, "lisi", "[2101]")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(ApiCodes.FORBIDDEN))
				.andExpect(jsonPath("$.message").value("没有权限"));
	}

	@Test
	void admin_replaceAdministratorRole_rejected() throws Exception {
		replace(1L, "admin", "[2101]")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.code").value(ApiCodes.BIZ_ERROR))
				.andExpect(jsonPath("$.message").value("管理员已经拥有全部菜单"));
	}

	private ResultActions replace(Long roleId, String username, String menuIdsJson) throws Exception {
		String token = authService.login(username, "admin123");
		return mockMvc.perform(put("/api/roles/" + roleId + "/menus")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"menuIds\":" + menuIdsJson + "}"));
	}
}
