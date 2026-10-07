package com.velrix.platform.web.controller.menu;

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
class MenuControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthService authService;

	@Test
	@Transactional
	void admin_createsPageAndButton_thenUpdatesListsAndDeletes() throws Exception {
		MvcResult created = create("admin", """
				{"parentId":2003,"name":"  验收菜单甲  ","path":"  /demo/menu  ","type":"MENU","sort":9}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(ApiCodes.OK))
				.andExpect(jsonPath("$.data.name").value("验收菜单甲"))
				.andExpect(jsonPath("$.data.path").value("/demo/menu"))
				.andExpect(jsonPath("$.data.type").value("MENU"))
				.andExpect(jsonPath("$.data.permCode").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.data.hidden").value(false))
				.andExpect(jsonPath("$.data.sort").value(9))
				.andExpect(jsonPath("$.data.parentId").value(2003))
				.andExpect(jsonPath("$.data.id").isNumber())
				.andReturn();
		long pageId = JsonPath.read(
				created.getResponse().getContentAsString(StandardCharsets.UTF_8),
				"$.data.id");
		assertTrue(pageId > 0);

		String listed = list("admin")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(ApiCodes.OK))
				.andReturn()
				.getResponse()
				.getContentAsString(StandardCharsets.UTF_8);
		Map<String, Object> menuAdmin = findByName(JsonPath.read(listed, "$.data"), "菜单管理");
		assertNotNull(menuAdmin, "管理树应包含菜单管理");
		assertNotNull(findByName(children(menuAdmin), "菜单列表"));
		assertNotNull(findByName(children(menuAdmin), "新建菜单"));
		assertNotNull(findByName(children(menuAdmin), "编辑菜单"));
		assertNotNull(findByName(children(menuAdmin), "删除菜单"));
		assertNotNull(findByName(children(menuAdmin), "验收菜单甲"));

		MvcResult button = create("admin", """
				{"parentId":%d,"name":"验收按钮","type":"BUTTON","permCode":"  demo:menu-action  "}
				""".formatted(pageId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.type").value("BUTTON"))
				.andExpect(jsonPath("$.data.permCode").value("demo:menu-action"))
				.andExpect(jsonPath("$.data.path").value(org.hamcrest.Matchers.nullValue()))
				.andReturn();
		long buttonId = JsonPath.read(
				button.getResponse().getContentAsString(StandardCharsets.UTF_8),
				"$.data.id");

		String afterButton = list("admin")
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString(StandardCharsets.UTF_8);
		Map<String, Object> pageNode = findByName(JsonPath.read(afterButton, "$.data"), "验收菜单甲");
		assertNotNull(pageNode);
		assertNotNull(findByName(children(pageNode), "验收按钮"));

		update(pageId, "admin", """
				{"parentId":2003,"name":"验收菜单甲改","path":"   ","type":"MENU","hidden":true}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("验收菜单甲改"))
				.andExpect(jsonPath("$.data.path").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.data.hidden").value(true))
				.andExpect(jsonPath("$.data.updatedAt").isNotEmpty());

		remove(pageId, "admin")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.code").value(ApiCodes.BIZ_ERROR))
				.andExpect(jsonPath("$.message").value("下面还有子菜单，不能删除"));

		remove(buttonId, "admin")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(ApiCodes.OK));
		remove(pageId, "admin")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(ApiCodes.OK));

		String afterDelete = list("admin")
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString(StandardCharsets.UTF_8);
		assertNull(findByName(JsonPath.read(afterDelete, "$.data"), "验收菜单甲改"));
	}

	@Test
	@Transactional
	void admin_rejectsInvalidMenu() throws Exception {
		create("admin", "{\"name\":\"   \",\"type\":\"MENU\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("菜单名字不能为空"));

		create("admin", "{\"name\":\"坏类型\",\"type\":\"PAGE\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("菜单类型只能是页面或按钮"));

		create("admin", "{\"parentId\":2003,\"name\":\"无码按钮\",\"type\":\"BUTTON\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("按钮必须填写权限码"));

		create("admin", "{\"parentId\":2003,\"name\":\"带码页面\",\"type\":\"MENU\",\"permCode\":\"menu:extra\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("页面不能填写权限码"));

		create("admin", "{\"parentId\":2003,\"name\":\"重复码\",\"type\":\"BUTTON\",\"permCode\":\"menu:list\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("该权限码已经存在"));

		create("admin", "{\"parentId\":999999999,\"name\":\"无父级\",\"type\":\"MENU\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("父级菜单不存在"));

		create("admin", "{\"parentId\":2012,\"name\":\"挂在按钮下\",\"type\":\"MENU\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("父级必须是页面"));

		update(999999999L, "admin", "{\"name\":\"不存在\",\"type\":\"MENU\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("该菜单不存在"));

		update(2003L, "admin", "{\"parentId\":2003,\"name\":\"菜单管理\",\"path\":\"/system/menus\",\"type\":\"MENU\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("不能把自己设为父级"));

		update(2000L, "admin", "{\"parentId\":2001,\"name\":\"系统管理\",\"path\":\"/system\",\"type\":\"MENU\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("不能把子孙设为父级"));

		update(2000L, "admin", "{\"name\":\"系统管理\",\"path\":\"/system\",\"type\":\"BUTTON\",\"permCode\":\"system:root\"}")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("下面还有子菜单，不能改成按钮"));

		remove(2003L, "admin")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("下面还有子菜单，不能删除"));

		remove(999999999L, "admin")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.message").value("该菜单不存在"));
	}

	@Test
	void lisi_menuCrud_forbidden() throws Exception {
		create("lisi", "{\"parentId\":2003,\"name\":\"越权菜单\",\"type\":\"MENU\"}")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(ApiCodes.FORBIDDEN));

		update(2003L, "lisi", "{\"name\":\"菜单管理\",\"type\":\"MENU\"}")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(ApiCodes.FORBIDDEN));

		list("lisi")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(ApiCodes.FORBIDDEN));

		remove(2011L, "lisi")
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
		return mockMvc.perform(post("/api/menus")
				.header("Authorization", "Bearer " + token(username))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private ResultActions update(long id, String username, String json) throws Exception {
		return mockMvc.perform(put("/api/menus/" + id)
				.header("Authorization", "Bearer " + token(username))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private ResultActions list(String username) throws Exception {
		return mockMvc.perform(get("/api/menus")
				.header("Authorization", "Bearer " + token(username)));
	}

	private ResultActions remove(long id, String username) throws Exception {
		return mockMvc.perform(delete("/api/menus/" + id)
				.header("Authorization", "Bearer " + token(username)));
	}

	private String token(String username) {
		return authService.login(username, "admin123");
	}
}
