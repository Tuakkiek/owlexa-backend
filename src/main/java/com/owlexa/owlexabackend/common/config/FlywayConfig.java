package com.owlexa.owlexabackend.common.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Value("${spring.flyway.enabled:true}")
    private boolean flywayEnabled;

    /**
     * Explicit Flyway bean so that the migration location and baseline version
     * are set correctly. When {@code spring.flyway.enabled=false} (e.g. in the
     * test profile where JPA create-drop manages the schema), the bean still
     * exists so that Spring's {@code entityManagerFactory} dependency chain is
     * satisfied, but no migration is executed.
     */
    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .baselineVersion("6")
                .locations("classpath:data/migration")
                .load();

        if (flywayEnabled) {
            flyway.repair(); // Remove any failed migration records from schema history
            flyway.migrate();
        }

        return flyway;
    }
}
