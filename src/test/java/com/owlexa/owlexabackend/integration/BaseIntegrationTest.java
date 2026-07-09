package com.owlexa.owlexabackend.integration;

import com.owlexa.owlexabackend.common.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Base class for all integration tests in Owlexa.
 *
 * <p>Provides:
 * <ul>
 *   <li>Full Spring Boot context startup (H2 in-memory DB, real security filter chain)</li>
 *   <li>MockMvc for HTTP layer testing</li>
 *   <li>Active "test" profile → loads application-test.yml</li>
 *   <li>TenantContext cleanup in {@code @AfterEach} to prevent ThreadLocal leak</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @AfterEach
    void resetTenantContext() {
        TenantContext.clear();
    }
}