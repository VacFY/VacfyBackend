package com.dev.vacfy.shared.infrastructure.documentation.openapi.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfiguration {
    public static final String COOKIE_AUTH = "cookieAuth";

    private static final String DESCRIPTION = """
            Backend de **VacTy**: vigila la temperatura de los termos de vacunas, registra los lotes que entran a cada \
            termo y genera **todas** las alertas (temperatura y vencimiento). El panel web y la app móvil solo las muestran.

            ## Cómo probar desde aquí
            1. Abre **Authentication → POST /api/v1/authentication/sign-in**, pulsa *Try it out* y envía tu DNI y \
            contraseña (o crea una cuenta con *sign-up*).
            2. La respuesta no trae un token: el backend deja la cookie `access-token` (HttpOnly, 12 h) y el navegador \
            la envía sola. **No hace falta el botón Authorize.** Ya puedes probar el resto.
            3. Para salir, usa *sign-out*. En local, si el navegador no guarda la cookie, arranca el backend con \
            `COOKIE_SECURE=false`.

            El candado marca los endpoints que piden sesión; sin sesión responden **401**.

            ## Registrar un lote
            1. **POST /api/v1/lots/read** con el código escaneado o escrito: devuelve GTIN, lote, vencimiento, \
            la vacuna (si el GTIN ya es conocido) y avisos. No guarda nada.
            2. Si `knownProduct` es `false`, elige la vacuna en **GET /api/v1/vaccines**.
            3. **POST /api/v1/lots**: guarda el lote. El termo recalcula su rango y, si el lote vence pronto, sale una alerta.
            4. Cuando se usa o se descarta: **PATCH /api/v1/lots/{lotId}/close**.

            ## Alertas
            - Tipos: `OUT_OF_RANGE`, `RAPID_CHANGE`, `SENSOR_OFFLINE`, `INVALID_READING`, `LOT_EXPIRING`, `LOT_EXPIRED`.
            - Ciclo: `ACTIVE` → `ACKNOWLEDGED` (la enfermera la vio) → `RESOLVED` (la condición terminó).
            - Cada alerta trae `title` (corto, para notificación), `message` (para la enfermera) y `affectedLots`.
            - Se consultan con **GET /api/v1/alerts** y llegan en vivo por el WebSocket `/ws/alerts`. \
            Swagger no muestra WebSockets: ver el README.

            ## Errores
            | Código | Significado |
            |---|---|
            | 400 | Datos inválidos |
            | 401 | No hay sesión |
            | 404 | No existe |
            | 409 | Choca con lo que ya existe (p. ej. rango incompatible en el termo, lote repetido) |
            | 500 | Error de negocio todavía sin manejar en autenticación, perfil y dispositivo (p. ej. contraseña incorrecta) |

            Vacunas, lotes y dashboard devuelven `message` con el motivo listo para mostrar. El resto devuelve el error \
            genérico de Spring, sin motivo.

            **Formatos:** fechas y horas en ISO-8601 UTC (`2026-10-07T14:03:00Z`), fechas de vencimiento `AAAA-MM-DD`, \
            temperatura en °C, humedad en %.""";

    @Value("${documentation.application.version}")
    String applicationVersion;

    @Bean
    public OpenAPI serviceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("VacTy API")
                        .description(DESCRIPTION)
                        .version(applicationVersion))
                .externalDocs(new ExternalDocumentation()
                        .description("README: contrato de alertas, WebSockets y ejemplos curl")
                        .url("https://github.com/VacFY/VacfyBackend#readme"))
                .components(new Components().addSecuritySchemes(COOKIE_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("access-token")
                        .description("Cookie de sesión. La pone sign-in o sign-up; el navegador la envía sola.")))
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_AUTH))
                // Orden de las secciones en Swagger UI
                .tags(List.of(
                        new Tag().name("Authentication").description("Registro, inicio y cierre de sesión (cookie HttpOnly)"),
                        new Tag().name("Dashboard").description("Pantalla de inicio de web y móvil"),
                        new Tag().name("Alerts").description("Alertas de temperatura y vencimiento generadas por el backend"),
                        new Tag().name("Lots").description("Lotes de vacunas en cada termo: leer el código de la caja, registrar, consultar y cerrar"),
                        new Tag().name("Vaccines").description("Catálogo de vacunas: para qué sirve cada una y en qué rango se conserva"),
                        new Tag().name("Readings").description("Historial de temperatura y humedad"),
                        new Tag().name("Vaccine profiles").description("Perfiles de rango (versión anterior del catálogo) y perfil asignado a cada termo"),
                        new Tag().name("Profile").description("Datos del usuario con sesión"),
                        new Tag().name("Device").description("Dispositivo del usuario")));
    }
}
