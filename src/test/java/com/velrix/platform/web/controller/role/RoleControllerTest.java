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
