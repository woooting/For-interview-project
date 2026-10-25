package com.velrix.it;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velrix.masterdata.application.product.ProductGuard;
import com.velrix.masterdata.application.warehouse.WarehouseGuard;
import com.velrix.masterdata.domain.warehouse.MdLocationCapacity;
import com.velrix.masterdata.infrastructure.persistence.warehouse.MdLocationCapacityMapper;
import com.velrix.shared.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.NoOpResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T5 集成测试（需求 §8 AC-M1-01~10）：
 * 独立库 aaa_it + Flyway 全量 V1~V23 + 真实 HTTP + 真实 JWT/RBAC 链路。
 * AC-M1-03 用真实库位容量引用触发非零 impact（不用 stub Contributor）。
 * 编码带随机后缀，可重复执行。
 * 跑法：.\mvnw.cmd test -Dtest=M1AcceptanceIT（需本机 MySQL + Redis）
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class M1AcceptanceIT {

    /** 不用 TestRestTemplate（Boot 4 需额外 restclient 模块）；裸 RestTemplate + 相对路径改绝对路径 */
    @Value("${local.server.port}")
    private int port;

    private final RestTemplate rest = noThrowRestTemplate();

    /** 容器未注册 ObjectMapper bean（Boot 4），测试自建即可 */
    private final ObjectMapper om = new ObjectMapper();

    @Autowired
    private ProductGuard productGuard;

    @Autowired
    private WarehouseGuard warehouseGuard;

    @Autowired
    private MdLocationCapacityMapper capacityMapper;

    @DynamicPropertySource
    static void independentDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:mysql://localhost:3306/aaa_it?useUnicode=true&characterEncoding=utf8"
                        + "&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true");
    }

    // ---------- helpers ----------

    private static RestTemplate noThrowRestTemplate() {
        // JDK HttpURLConnection 不支持 PATCH，换 java.net.http.HttpClient 底层
        RestTemplate rt = new RestTemplate(
                new org.springframework.http.client.JdkClientHttpRequestFactory(java.net.http.HttpClient.newHttpClient()));
        // 4xx/5xx（403/409 断言对象）不抛异常，原样返回 ResponseEntity
        rt.setErrorHandler(new NoOpResponseErrorHandler());
        return rt;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String login(String username) throws Exception {
        ResponseEntity<String> resp = rest.postForEntity(url("/api/auth/login"),
                entity(null, Map.of("username", username, "password", "admin123")), String.class);
        assertEquals("OK", om.readTree(resp.getBody()).path("code").asText());
        return om.readTree(resp.getBody()).path("data").path("accessToken").asText();
    }

    private HttpEntity<Object> entity(String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return new HttpEntity<>(body, headers);
    }

    private ResponseEntity<String> exchange(String token, HttpMethod method, String path, Object body) {
        return rest.exchange(url(path), method, entity(token, body), String.class);
    }

    /** 建商品（随机编码），返回 id */
    private long createProduct(String token, String suffix) throws Exception {
        ResponseEntity<String> resp = exchange(token, HttpMethod.POST, "/api/products",
                Map.of("code", "sku-" + suffix, "name", "IT商品" + suffix, "unit", "个"));
        return om.readTree(resp.getBody()).path("data").path("id").asLong();
    }

    /** 建仓库（随机编码），返回仓库 id */
    private long createWarehouse(String token, String suffix) throws Exception {
        ResponseEntity<String> resp = exchange(token, HttpMethod.POST, "/api/warehouses",
                Map.of("code", "wh-" + suffix, "name", "IT仓" + suffix));
        return om.readTree(resp.getBody()).path("data").path("id").asLong();
    }

    private long createLocation(String token, long warehouseId, String suffix) throws Exception {
        ResponseEntity<String> resp = exchange(token, HttpMethod.POST,
                "/api/warehouses/" + warehouseId + "/locations",
                Map.of("locations", List.of(Map.of("code", "loc-" + suffix, "name", "IT库位" + suffix))));
        return om.readTree(resp.getBody()).path("data").get(0).path("id").asLong();
    }

    // ---------- AC-M1-01：编码 trim + 大写规范化 + 重复拒绝 ----------

    @Test
    void ac01_productCodeNormalizedAndDuplicateRejected() throws Exception {
        String token = login("admin");
        String s = suffix();
        String raw = " sku-" + s + " ";

        ResponseEntity<String> created = exchange(token, HttpMethod.POST, "/api/products",
                Map.of("code", raw, "name", "AC01商品", "unit", "个"));
        JsonNode data = om.readTree(created.getBody()).path("data");
        assertEquals("sku-" + s, data.path("code").asText(), "展示 code 应保留 trim 后原大小写");
        assertEquals("SKU-" + s, data.path("codeNorm").asText(), "codeNorm 应为大写规范化");

        ResponseEntity<String> dup = exchange(token, HttpMethod.POST, "/api/products",
                Map.of("code", "SKU-" + s, "name", "重复商品", "unit", "个"));
        JsonNode dupBody = om.readTree(dup.getBody());
        assertEquals("BIZ_ERROR", dupBody.path("code").asText());
        assertTrue(dupBody.path("message").asText().contains("已存在"));
    }

    // ---------- AC-M1-02：更新编码与他人冲突被拒 ----------

    @Test
    void ac02_productCodeUpdateConflictRejected() throws Exception {
        String token = login("admin");
        String s = suffix();
        long a = createProduct(token, "a" + s);
        long b = createProduct(token, "b" + s);

        ResponseEntity<String> resp = exchange(token, HttpMethod.PUT, "/api/products/" + b,
                Map.of("code", "sku-a" + s, "name", "改名", "unit", "个"));
        JsonNode body = om.readTree(resp.getBody());
        assertEquals("BIZ_ERROR", body.path("code").asText());
        assertTrue(body.path("message").asText().contains("已存在"));
    }

    // ---------- AC-M1-03：有引用时 DELETE 409（真实 LOCATION_CAPACITY 引用） ----------

    @Test
    void ac03_deleteProductWithRefsGets409() throws Exception {
        String token = login("admin");
        String s = suffix();
        long productId = createProduct(token, s);
        long warehouseId = createWarehouse(token, s);
        long locationId = createLocation(token, warehouseId, s);

        ResponseEntity<String> cap = exchange(token, HttpMethod.PUT,
                "/api/locations/" + locationId + "/capacities",
                Map.of("items", List.of(Map.of("productId", productId, "maxQty", new BigDecimal("100")))));
        assertEquals("OK", om.readTree(cap.getBody()).path("code").asText());

        ResponseEntity<String> impact = exchange(token, HttpMethod.GET,
                "/api/products/" + productId + "/impact", null);
        JsonNode impactData = om.readTree(impact.getBody()).path("data");
        assertFalse(impactData.path("deletable").asBoolean());
        boolean hasCapacityItem = false;
        for (JsonNode item : impactData.path("items")) {
            if ("LOCATION_CAPACITY".equals(item.path("refType").asText()) && item.path("count").asInt() == 1) {
                hasCapacityItem = true;
            }
        }
        assertTrue(hasCapacityItem, "impact 应含 LOCATION_CAPACITY count=1");

        ResponseEntity<String> deleted = exchange(token, HttpMethod.DELETE,
                "/api/products/" + productId, null);
        assertEquals(409, deleted.getStatusCode().value());
        assertTrue(deleted.getBody().contains("库位容量配置"));
    }

    // ---------- AC-M1-04：停用商品 requireActive 抛业务异常 ----------

    @Test
    void ac04_inactiveProductGuardThrows() throws Exception {
        String token = login("admin");
        long productId = createProduct(token, suffix());

        ResponseEntity<String> patched = exchange(token, HttpMethod.PATCH,
                "/api/products/" + productId + "/status", Map.of("status", "INACTIVE"));
        assertEquals("INACTIVE", om.readTree(patched.getBody()).path("data").path("status").asText());

        BizException e = assertThrows(BizException.class, () -> productGuard.requireActive(productId));
        assertTrue(e.getMessage().contains("停用"));
    }

    // ---------- AC-M1-05：容量 maxQty=0 被拒 ----------

    @Test
    void ac05_capacityMaxQtyZeroRejected() throws Exception {
        String token = login("admin");
        String s = suffix();
        long productId = createProduct(token, s);
        long warehouseId = createWarehouse(token, s);
        long locationId = createLocation(token, warehouseId, s);

        ResponseEntity<String> resp = exchange(token, HttpMethod.PUT,
                "/api/locations/" + locationId + "/capacities",
                Map.of("items", List.of(Map.of("productId", productId, "maxQty", BigDecimal.ZERO))));
        JsonNode body = om.readTree(resp.getBody());
        assertEquals("PARAM_INVALID", body.path("code").asText(), "maxQty=0 应被 @DecimalMin 拦截");
    }

    // ---------- AC-M1-06：容量 PUT 覆盖式保存 ----------

    @Test
    void ac06_capacityPutOverwrites() throws Exception {
        String token = login("admin");
        String s = suffix();
        long productId = createProduct(token, s);
        long warehouseId = createWarehouse(token, s);
        long locationId = createLocation(token, warehouseId, s);

        exchange(token, HttpMethod.PUT, "/api/locations/" + locationId + "/capacities",
                Map.of("items", List.of(Map.of("productId", productId, "maxQty", new BigDecimal("50")))));
        exchange(token, HttpMethod.PUT, "/api/locations/" + locationId + "/capacities",
                Map.of("items", List.of(Map.of("productId", productId, "maxQty", new BigDecimal("80")))));

        List<MdLocationCapacity> rows = capacityMapper.selectList(
                new LambdaQueryWrapper<MdLocationCapacity>().eq(MdLocationCapacity::getLocationId, locationId));
        assertEquals(1, rows.size(), "覆盖保存后应仅一行");
        assertEquals(0, rows.get(0).getMaxQty().compareTo(new BigDecimal("80")));
    }

    // ---------- AC-M1-07：停用仓库 ----------

    @Test
    void ac07_warehouseDisable() throws Exception {
        String token = login("admin");
        long warehouseId = createWarehouse(token, suffix());

        ResponseEntity<String> resp = exchange(token, HttpMethod.PATCH,
                "/api/warehouses/" + warehouseId + "/status", Map.of("status", "INACTIVE"));
        assertEquals("INACTIVE", om.readTree(resp.getBody()).path("data").path("status").asText());
    }

    // ---------- AC-M1-08：停用仓库 requireActiveForInbound 拒绝（AC1-2） ----------

    @Test
    void ac08_inactiveWarehouseInboundGuardRejects() throws Exception {
        String token = login("admin");
        long warehouseId = createWarehouse(token, suffix());

        exchange(token, HttpMethod.PATCH, "/api/warehouses/" + warehouseId + "/status",
                Map.of("status", "INACTIVE"));

        BizException e = assertThrows(BizException.class,
                () -> warehouseGuard.requireActiveForInbound(warehouseId));
        assertTrue(e.getMessage().contains("停用"));
    }

    // ---------- AC-M1-09：无权限调列表 403 ----------

    @Test
    void ac09_noPermissionGets403() throws Exception {
        String lisi = login("lisi");

        ResponseEntity<String> resp = exchange(lisi, HttpMethod.GET, "/api/customers", null);
        assertEquals(403, resp.getStatusCode().value(), "采购员无 customer:list 应 403");
    }

    // ---------- AC-M1-10：管理员创建客户并停用，列表可见 INACTIVE ----------

    @Test
    void ac10_customerCreateAndDisableVisibleInList() throws Exception {
        String token = login("admin");
        String s = suffix();

        ResponseEntity<String> created = exchange(token, HttpMethod.POST, "/api/customers",
                Map.of("code", "cu-" + s, "name", "AC10客户" + s));
        long customerId = om.readTree(created.getBody()).path("data").path("id").asLong();

        exchange(token, HttpMethod.PATCH, "/api/customers/" + customerId + "/status",
                Map.of("status", "INACTIVE"));

        ResponseEntity<String> list = exchange(token, HttpMethod.GET,
                "/api/customers?keyword=AC10客户" + s, null);
        JsonNode rows = om.readTree(list.getBody()).path("data");
        assertEquals(1, rows.size());
        assertEquals("INACTIVE", rows.get(0).path("status").asText());
    }
}
