package com.dev.vacfy.monitoring.infrastructure.persistence.jpa;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ajustes de esquema que JPA no puede declarar. Se aplican después de que Hibernate
 * actualiza las tablas (ddl-auto=update) y antes de que la app reciba peticiones.
 * Son idempotentes: se pueden ejecutar en cada arranque.
 */
@Component
public class SchemaPatches {
    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaPatches.class);

    private static final String[] PATCHES = {
            // Un mismo lote de la misma vacuna solo puede estar ACTIVE una vez por termo
            "CREATE UNIQUE INDEX IF NOT EXISTS ux_vaccine_lots_active "
                    + "ON vaccine_lots (vaccine_id, lot_number, contenedor) WHERE status = 'ACTIVE'",
            // Un termo tiene como máximo una asignación activa
            "CREATE UNIQUE INDEX IF NOT EXISTS ux_container_assignments_active "
                    + "ON container_assignments (contenedor) WHERE hasta IS NULL",
            // Hibernate creó un CHECK con los tipos de alerta de entonces; ddl-auto=update no lo amplía
            // y las alertas LOT_EXPIRING / LOT_EXPIRED fallarían al guardarse
            "ALTER TABLE alerts DROP CONSTRAINT IF EXISTS alerts_type_check"
    };

    private final JdbcTemplate jdbcTemplate;

    /** Depende del EntityManagerFactory para ejecutarse después de que Hibernate cree las tablas. */
    public SchemaPatches(JdbcTemplate jdbcTemplate, EntityManagerFactory entityManagerFactory) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void apply() {
        for (String sql : PATCHES) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                LOGGER.warn("No se pudo aplicar el ajuste de esquema: {}", sql, e);
            }
        }
    }
}
