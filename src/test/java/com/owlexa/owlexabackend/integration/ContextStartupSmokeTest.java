package com.owlexa.owlexabackend.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test to verify the Spring Boot context starts up under the
 * "test" profile. If this passes, the foundation works.
 */
class ContextStartupSmokeTest extends BaseIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Context loads under 'test' profile")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
        String[] activeProfiles = applicationContext.getEnvironment().getActiveProfiles();
        assertThat(activeProfiles).contains("test");
    }

    @Test
    @DisplayName("MockMvc bean is autowired and reachable")
    void mockMvcBeanIsAvailable() {
        assertThat(mockMvc).isNotNull();
    }
}