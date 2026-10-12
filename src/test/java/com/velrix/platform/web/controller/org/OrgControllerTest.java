package com.velrix.platform.web.controller.org;

import com.jayway.jsonpath.JsonPath;
import com.velrix.platform.application.auth.AuthService;
import com.velrix.platform.domain.user.SysUser;
import com.velrix.platform.infrastructure.persistence.user.SysUserMapper;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrgControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthService authService;

	@Autowired
	private SysUserMapper sysUserMapper;

	@Test
	@Transactional
	void admin_createsOrgTree_thenUpdatesListsAndDeletes() throws Exception {
		MvcResult rootCreated = create("admin", """
				{"name":"  验收总部  ","type":" DEV ","sort":1}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(ApiCodes.OK))
				.andExpect(jsonPath("$.data.name").value("验收总部"))
				.andExpect(jsonPath("$.data.type").value("DEV"))
				.andExpect(jsonPath("$.data.sort").value(1))
				.andExpect(jsonPath("$.data.parentId").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.data.id").isNumber())
				.andReturn();
		long rootId = JsonPath.read(
				rootCreated.getResponse().getContentAsString(StandardCharsets.UTF_8),
				"$.data.id");
		assertTrue(rootId > 0);

		MvcResult childCreated = create("admin", """
				{"parentId":%d,"name":"研发部","sort":2}
				""".formatted(rootId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("研发部"))
				.andExpect(jsonPath("$.data.parentId").value(rootId))
				.andReturn();
		long childId = JsonPath.read(
				childCreated.getResponse().getContentAsString(StandardCharsets.UTF_8),
				"$.data.id");

		String listed = list("admin")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(ApiCodes.OK))
				.andReturn()
				.getResponse()
				.getContentAsString(StandardCharsets.UTF_8);
		Map<String, Object> rootNode = findByName(JsonPath.read(listed, "$.data"), "验收总部");
		assertNotNull(rootNode);
		assertNotNull(findByName(children(rootNode), "研发部"));

		update(rootId, "admin", """
				{"name":"验收总部改","type":"MARKET","sort":3}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("验收总部改"))
				.andExpect(jsonPath("$.data.type").value("MARKET"))
				.andExpect(jsonPath("$.data.updatedAt").isNotEmpty());

		remove(rootId, "admin")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.code").value(ApiCodes.BIZ_ERROR))
				.andExpect(jsonPath("$.message").value("下面还有子部门，不能删除"));

		remove(childId, "admin")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(ApiCodes.OK));
		remove(rootId, "admin")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(ApiCodes.OK));

		String afterDelete = list("admin")
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString(StandardCharsets.UTF_8);
		assertNull(findByName(JsonPath.read(afterDelete, "$.data"), "验收总部改"));
	}

	@Test
	@Transactional
	void admin_rejectsInvalidOrg() throws Exception {
		create("admin", "{\"name\":\"   \"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("部门名称不能为空"));

		MvcResult root = create("admin", "{\"name\":\"重名根A\",\"sort\":0}")
				.andExpect(status().isOk())
				.andReturn();
		long rootId = JsonPath.read(
				root.getResponse().getContentAsString(StandardCharsets.UTF_8),
				"$.data.id");

		create("admin", "{\"name\":\"重名根A\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("同级下已有同名部门"));

		create("admin", "{\"parentId\":999999999,\"name\":\"无父级\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("父级组织不存在"));

		update(999999999L, "admin", "{\"name\":\"不存在\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("该组织不存在"));

		update(rootId, "admin", "{\"parentId\":%d,\"name\":\"重名根A\"}".formatted(rootId))
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("不能把自己设为父级"));

		MvcResult child = create("admin", """
				{"parentId":%d,"name":"子部门"}
				""".formatted(rootId))
				.andExpect(status().isOk())
				.andReturn();
		long childId = JsonPath.read(
				child.getResponse().getContentAsString(StandardCharsets.UTF_8),
				"$.data.id");

		update(rootId, "admin", """
				{"parentId":%d,"name":"重名根A"}
				""".formatted(childId))
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("不能把子孙设为父级"));

		remove(rootId, "admin")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("下面还有子部门，不能删除"));

		SysUser lisi = sysUserMapper.selectById(1002L);
		lisi.setOrgId(rootId);
		sysUserMapper.updateById(lisi);

		remove(childId, "admin")
				.andExpect(status().isOk());
		remove(rootId, "admin")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("还有用户归属该部门，不能删除"));

		remove(999999999L, "admin")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("该组织不存在"));
	}

	@Test
	void lisi_orgCrud_forbidden() throws Exception {
		create("lisi", "{\"name\":\"越权部门\"}")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(ApiCodes.FORBIDDEN));

		list("lisi")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(ApiCodes.FORBIDDEN));

		update(1L, "lisi", "{\"name\":\"改部门\"}")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(ApiCodes.FORBIDDEN));

		remove(1L, "lisi")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(ApiCodes.FORBIDDEN));
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> children(Map<String, Object> node) {
		Object value = node.get("children");
		if (value == null) {
			return List.of();
		}
		return (List<Map<String, Object>>) value;
	}

	private static Map<String, Object> findByName(List<Map<String, Object>> nodes, String name) {
		for (Map<String, Object> node : nodes) {
			if (name.equals(node.get("name"))) {
				return node;
			}
			Map<String, Object> found = findByName(children(node), name);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private ResultActions create(String username, String json) throws Exception {
		return mockMvc.perform(post("/api/orgs")
				.header("Authorization", "Bearer " + token(username))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private ResultActions update(long id, String username, String json) throws Exception {
		return mockMvc.perform(put("/api/orgs/" + id)
				.header("Authorization", "Bearer " + token(username))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private ResultActions list(String username) throws Exception {
		return mockMvc.perform(get("/api/orgs")
				.header("Authorization", "Bearer " + token(username)));
	}

	private ResultActions remove(long id, String username) throws Exception {
		return mockMvc.perform(delete("/api/orgs/" + id)
				.header("Authorization", "Bearer " + token(username)));
	}

	private String token(String username) {
		return authService.login(username, "admin123").accessToken();
	}
}
