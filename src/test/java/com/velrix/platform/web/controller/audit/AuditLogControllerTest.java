package com.velrix.platform.web.controller.audit;

import com.jayway.jsonpath.JsonPath;
import com.velrix.platform.application.auth.AuthService;
import com.velrix.shared.api.ApiCodes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditLogControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthService authService;

	@Test
	@Transactional
	void admin_listsAuditLogs_afterRoleMenuChange() throws Exception {
		replaceRoleMenus(2L, "admin", "[2101]")
				.andExpect(status().isOk());

		String json = listLogs("admin", "admin", "ROLE", null, null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(ApiCodes.OK))
				.andReturn()
				.getResponse()
				.getContentAsString(StandardCharsets.UTF_8);

		List<String> actions = JsonPath.read(json, "$.data[*].action");
		assertTrue(actions.contains("ROLE_MENU_UPDATE"));
		List<String> actors = JsonPath.read(json, "$.data[*].actor");
		assertTrue(actors.contains("admin"));
	}

	@Test
	void lisi_listAuditLogs_forbidden() throws Exception {
		listLogs("lisi", null, null, null, null)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(ApiCodes.FORBIDDEN));
	}

	private ResultActions replaceRoleMenus(long roleId, String username, String menuIdsJson) throws Exception {
		return mockMvc.perform(put("/api/roles/" + roleId + "/menus")
				.header("Authorization", "Bearer " + token(username))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"menuIds\":" + menuIdsJson + "}"));
	}

	private ResultActions listLogs(
			String username,
			String actor,
			String targetType,
			String from,
			String to) throws Exception {
		var builder = get("/api/audit-logs")
				.header("Authorization", "Bearer " + token(username));
		if (actor != null) {
			builder.param("actor", actor);
		}
		if (targetType != null) {
			builder.param("targetType", targetType);
		}
		if (from != null) {
			builder.param("from", from);
		}
		if (to != null) {
			builder.param("to", to);
		}
		return mockMvc.perform(builder);
	}

	private String token(String username) {
		return authService.login(username, "admin123");
	}
}
