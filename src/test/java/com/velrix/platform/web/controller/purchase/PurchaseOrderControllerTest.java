package com.velrix.platform.web.controller.purchase;

import com.velrix.platform.application.auth.AuthService;
import com.velrix.shared.api.ApiCodes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PurchaseOrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthService authService;

	@Test
	void admin_submit_ok() throws Exception {
		submit("admin").andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(ApiCodes.OK));
	}

	@Test
	void lisi_submit_ok() throws Exception {
		submit("lisi").andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(ApiCodes.OK));
	}

	@Test
	void wangwu_submit_forbidden() throws Exception {
		submit("wangwu").andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(ApiCodes.FORBIDDEN))
				.andExpect(jsonPath("$.message").value("没有权限"));
	}

	private ResultActions submit(String username) throws Exception {
		String token = authService.login(username, "admin123");
		return mockMvc.perform(post("/api/purchase-orders/submit")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON));
	}
}
