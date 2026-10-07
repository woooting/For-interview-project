package com.velrix.platform.web.controller.role;

import com.jayway.jsonpath.JsonPath;
import com.velrix.platform.application.auth.AuthService;
import com.velrix.shared.api.ApiCodes;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoleControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthService authService;

	/**
	 * 真打 POST /api/roles：断言响应里带 MyBatis-Plus 生成的 id，以及 insert 后 selectById 回填的 seq、时间。
	 * {@link Transactional} 会在用例结束时回滚，不落库。
	 */
	@Test
	@Transactional
	void createRole_responseIncludesNewIdAndFieldsReloadedFromDb() throws Exception {
		MvcResult created = create("admin",
				"{\"name\":\"雪花id回填验证角色\",\"description\":\"测 create 返回最新行\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(ApiCodes.OK))
				.andReturn();

		String json = created.getResponse().getContentAsString(StandardCharsets.UTF_8);
		System.out.println("POST /api/roles 响应 JSON: " + json);

		Number id = JsonPath.read(json, "$.data.id");
		assertNotNull(id, "create 响应应包含新生成的 id");
		assertTrue(id.longValue() > 0, "id 应为正数（雪花等策略生成）");

		Object seq = JsonPath.read(json, "$.data.seq");
		Object createdAt = JsonPath.read(json, "$.data.createdAt");
		Object updatedAt = JsonPath.read(json, "$.data.updatedAt");
		assertNotNull(seq, "selectById 后应带回数据库 seq");
		assertNotNull(createdAt, "selectById 后应带回 createdAt");
		assertNotNull(updatedAt, "selectById 后应带回 updatedAt");
	}

	@Test
	@Transactional
	void admin_createsRole_thenUpdatesAndListsIt() throws Exception {
		MvcResult created = create("admin", "{\"name\":\"  验收角色甲  \",\"description\":\"说明\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(ApiCodes.OK))
				.andExpect(jsonPath("$.data.name").value("验收角色甲"))
				.andExpect(jsonPath("$.data.description").value("说明"))
				.andExpect(jsonPath("$.data.administrator").value(false))
				.andExpect(jsonPath("$.data.id").isNumber())
				.andReturn();
		Number id = JsonPath.read(
				created.getResponse().getContentAsString(StandardCharsets.UTF_8),
				"$.data.id");

		String listed = list("admin")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(ApiCodes.OK))
				.andReturn()
				.getResponse()
				.getContentAsString(StandardCharsets.UTF_8);
		List<String> names = JsonPath.read(listed, "$.data[*].name");
		assertTrue(names.contains("系统管理员"));
		assertTrue(names.contains("验收角色甲"));
		List<Number> seqs = JsonPath.read(listed, "$.data[*].seq");
		for (int i = 1; i < seqs.size(); i++) {
			assertTrue(seqs.get(i).longValue() >= seqs.get(i - 1).longValue());
		}

		update(id.longValue(), "admin",
				"{\"name\":\"验收角色甲\",\"description\":null,\"administrator\":true}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(ApiCodes.OK))
				.andExpect(jsonPath("$.data.name").value("验收角色甲"))
				.andExpect(jsonPath("$.data.description").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.data.administrator").value(true));
	}

	@Test
	@Transactional
	void admin_rejectsBlankDuplicateAndMissingRole() throws Exception {
		create("admin", "{\"name\":\"   \"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.code").value(ApiCodes.BIZ_ERROR))
				.andExpect(jsonPath("$.message").value("角色名不能为空"));

		create("admin", "{\"name\":\"采购员\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.code").value(ApiCodes.BIZ_ERROR))
				.andExpect(jsonPath("$.message").value("该角色已经存在"));

		update(2L, "admin", "{\"name\":\"系统管理员\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("该角色已经存在"));

		update(999999999L, "admin", "{\"name\":\"不存在的角色\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("该角色不存在"));
	}

	@Test
	void lisi_roleCrud_forbidden() throws Exception {
		create("lisi", "{\"name\":\"验收角色乙\"}")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(ApiCodes.FORBIDDEN))
				.andExpect(jsonPath("$.message").value("没有权限"));

		update(2L, "lisi", "{\"name\":\"采购员\"}")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(ApiCodes.FORBIDDEN));

		list("lisi")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(ApiCodes.FORBIDDEN));
	}

	private ResultActions create(String username, String json) throws Exception {
		return mockMvc.perform(post("/api/roles")
				.header("Authorization", "Bearer " + token(username))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private ResultActions update(long id, String username, String json) throws Exception {
		return mockMvc.perform(put("/api/roles/" + id)
				.header("Authorization", "Bearer " + token(username))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private ResultActions list(String username) throws Exception {
		return mockMvc.perform(get("/api/roles")
				.header("Authorization", "Bearer " + token(username)));
	}

	private String token(String username) {
		return authService.login(username, "admin123");
	}
}
