package com.velrix.platform.web.controller.user;

import com.jayway.jsonpath.JsonPath;
import com.velrix.platform.application.auth.AuthService;
import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

	private static final String USERNAME = "验收用户甲";
	private static final String OLD_PASSWORD = "old-pass-1";
	private static final String NEW_PASSWORD = "new-pass-1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthService authService;

	@Test
	@Transactional
	void admin_createsUser_thenUpdatesAndReplacesRoles() throws Exception {
		MvcResult created = create("admin",
				"{\"username\":\"  " + USERNAME + "  \",\"displayName\":\" 验收甲 \",\"password\":\"" + OLD_PASSWORD + "\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(ApiCodes.OK))
				.andExpect(jsonPath("$.data.username").value(USERNAME))
				.andExpect(jsonPath("$.data.displayName").value("验收甲"))
				.andExpect(jsonPath("$.data.enabled").value(true))
				.andExpect(jsonPath("$.data.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.data.id").isNumber())
				.andReturn();
		Number id = JsonPath.read(
				created.getResponse().getContentAsString(StandardCharsets.UTF_8),
				"$.data.id");
		assertFalse(authService.login(USERNAME, OLD_PASSWORD).isBlank());

		update(id.longValue(), "admin", "{\"displayName\":\"验收甲改\",\"enabled\":false}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.displayName").value("验收甲改"))
				.andExpect(jsonPath("$.data.enabled").value(false))
				.andExpect(jsonPath("$.data.username").value(USERNAME));
		assertLoginFailed(USERNAME, OLD_PASSWORD);

		update(id.longValue(), "admin",
				"{\"displayName\":\"验收甲改\",\"password\":\"   \",\"enabled\":true}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.enabled").value(true));
		assertFalse(authService.login(USERNAME, OLD_PASSWORD).isBlank());

		update(id.longValue(), "admin",
				"{\"displayName\":\"验收甲改\",\"password\":\"" + NEW_PASSWORD + "\",\"enabled\":true}")
				.andExpect(status().isOk());
		assertFalse(authService.login(USERNAME, NEW_PASSWORD).isBlank());
		assertLoginFailed(USERNAME, OLD_PASSWORD);

		replaceRoles(id.longValue(), "admin", "{\"roleIds\":[2,3,2]}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(ApiCodes.OK))
				.andExpect(jsonPath("$.data", hasSize(2)))
				.andExpect(jsonPath("$.data", hasItem(2)))
				.andExpect(jsonPath("$.data", hasItem(3)));

		replaceRoles(id.longValue(), "admin", "{\"roleIds\":[]}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data", hasSize(0)));
	}

	@Test
	@Transactional
	void admin_rejectsBlankDuplicateAndMissingUser() throws Exception {
		create("admin", "{\"username\":\"   \",\"displayName\":\"甲\",\"password\":\"old-pass-1\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.code").value(ApiCodes.BIZ_ERROR))
				.andExpect(jsonPath("$.message").value("登录名不能为空"));

		create("admin", "{\"username\":\"验收乙\",\"displayName\":\"   \",\"password\":\"old-pass-1\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("昵称不能为空"));

		create("admin", "{\"username\":\"验收乙\",\"displayName\":\"乙\",\"password\":\"   \"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("密码不能为空"));

		create("admin", "{\"username\":\"Admin\",\"displayName\":\"乙\",\"password\":\"old-pass-1\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("该用户已经存在"));

		update(999999999L, "admin", "{\"displayName\":\"不存在\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("该用户不存在"));

		replaceRoles(999999999L, "admin", "{\"roleIds\":[2]}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("该用户不存在"));

		replaceRoles(1002L, "admin", "{\"roleIds\":[999999999]}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("此角色未创建，无法分配给当前用户"));
	}

	@Test
	void lisi_userCrud_forbidden() throws Exception {
		create("lisi", "{\"username\":\"验收用户乙\",\"displayName\":\"乙\",\"password\":\"old-pass-1\"}")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(ApiCodes.FORBIDDEN))
				.andExpect(jsonPath("$.message").value("没有权限"));

		update(1002L, "lisi", "{\"displayName\":\"李四\"}")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(ApiCodes.FORBIDDEN));

		replaceRoles(1002L, "lisi", "{\"roleIds\":[2]}")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(ApiCodes.FORBIDDEN));
	}

	private void assertLoginFailed(String username, String password) {
		BizException ex = assertThrows(BizException.class, () -> authService.login(username, password));
		assertEquals(ApiCodes.BIZ_LOGIN_FAILED, ex.getCode());
	}

	private ResultActions create(String username, String json) throws Exception {
		return mockMvc.perform(post("/api/users")
				.header("Authorization", "Bearer " + token(username))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private ResultActions update(long id, String username, String json) throws Exception {
		return mockMvc.perform(put("/api/users/" + id)
				.header("Authorization", "Bearer " + token(username))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private ResultActions replaceRoles(long id, String username, String json) throws Exception {
		return mockMvc.perform(put("/api/users/" + id + "/roles")
				.header("Authorization", "Bearer " + token(username))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private String token(String username) {
		return authService.login(username, "admin123");
	}
}
