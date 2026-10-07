# VacTy Backend

Spring Boot 4 (OpenJDK 25), monolito modular hexagonal.

| Módulo | Qué hace |
|---|---|
| `iam` | Registro e inicio de sesión. Token opaco en cookie, sesiones en la tabla `sessions` (Postgres) con expiración |
| `user` | Perfiles y dispositivos |
| `iot` | Recibe la telemetría por MQTT y la reenvía en vivo por `/ws/device` |
| `monitoring` | **Motor de alertas**: guarda lecturas, evalúa reglas y avisa por `/ws/alerts` |

Base de datos: PostgreSQL en **Supabase**. Broker: Mosquitto u otro broker MQTT.

## Motor de alertas

| Regla | Se abre cuando… | Se cierra cuando… |
|---|---|---|
| `OUT_OF_RANGE` | 3 lecturas seguidas fuera del rango del perfil (por defecto 2–8 °C). `CRITICAL` si la vacuna es sensible a congelación y baja del mínimo | Vuelve al rango con 0.5 °C de margen |
| `RAPID_CHANGE` | La temperatura cambia ≥ 2 °C en ≤ 5 min. Usa la mediana de 3 lecturas para ignorar picos del sensor | Se estabiliza |
| `SENSOR_OFFLINE` | No llegan lecturas por 2 min | Llega una lectura |
| `INVALID_READING` | 3 lecturas seguidas nulas o imposibles | Llega una lectura válida |

- Cada episodio genera **una sola alerta**, con estados `ACTIVE → ACKNOWLEDGED → RESOLVED`.
- Las lecturas se guardan **1 cada 30 s por contenedor**, y **todas** mientras hay una alerta abierta.
- Los umbrales se configuran en `application.yaml`, bajo `vacty.alerts.*`.

### API nueva (requiere sesión)

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api/v1/alerts?status=OPEN&contenedor=001` | Alertas. `status`: OPEN (por defecto), ACTIVE, ACKNOWLEDGED, RESOLVED, ALL |
| PATCH | `/api/v1/alerts/{id}/acknowledge` | La enfermera marca la alerta como vista |
| GET | `/api/v1/readings?contenedor=001&from=…&to=…` | Historial (ISO-8601 UTC; por defecto, últimas 24 h) |
| GET / POST | `/api/v1/vaccine-profiles` | Perfiles: `{"name","minTemp","maxTemp","freezeSensitive"}` |
| PUT | `/api/v1/containers/{contenedor}/profile` | Asigna un perfil a un contenedor: `{"profileId":1}` |

### WebSockets

- `/ws/device`: telemetría en vivo. Es el mismo JSON de antes: `{"contenedor","temperatura","humedad","distancia"}`.
- `/ws/alerts`: cada mensaje es una alerta (`id, contenedor, type, severity, status, message, triggerValue, minValue, maxValue, startedAt, …`). Al conectarse se reciben primero las alertas abiertas.

## Correr en local

Requisitos: JDK 25 y Docker.

```bash
cp .env.example .env        # completa la contraseña de Supabase y AUTH_OPAQUE_SECRET
docker compose up -d        # Mosquitto
./mvnw spring-boot:run
```

- Swagger: http://localhost:8080/swagger-ui.html
- Salud: http://localhost:8080/actuator/health

### Prueba del motor sin el ESP32

```bash
T=iot/telemetry
# Normal: no pasa nada
for i in 1 2 3; do mosquitto_pub -t $T -m '{"contenedor":"001","temperatura":5.0,"humedad":50}'; sleep 2; done
# Fuera de rango: a la 3.ª lectura se abre OUT_OF_RANGE (y RAPID_CHANGE por el salto)
for i in 1 2 3; do mosquitto_pub -t $T -m '{"contenedor":"001","temperatura":10.0,"humedad":50}'; sleep 2; done
# Vuelve al rango: se cierra
mosquitto_pub -t $T -m '{"contenedor":"001","temperatura":7.0,"humedad":50}'
# JSON con nan (firmware antiguo): no rompe nada; tres seguidas abren INVALID_READING
mosquitto_pub -t $T -m '{"contenedor":"001","temperatura":nan,"humedad":nan}'
# Deja de publicar 2–3 min: se abre SENSOR_OFFLINE
```

Para ver las alertas: `GET /api/v1/alerts?status=ALL`, o conecta un cliente WebSocket a `ws://localhost:8080/ws/alerts`.

## Variables de entorno

| Variable | Ejemplo / nota |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` en el servidor (el Dockerfile ya lo pone) |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://aws-0-us-east-2.pooler.supabase.com:5432/postgres?sslmode=require` (**Session pooler**, IPv4) |
| `SPRING_DATASOURCE_USERNAME` | `postgres.<project-ref>` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base de datos de Supabase |
| `AUTH_OPAQUE_SECRET` | Obligatorio, ≥ 32 caracteres: `openssl rand -base64 48` |
| `AUTH_SESSION_TTL_HOURS` | Duración de la sesión en horas; por defecto 12 |
| `ALLOWED_ORIGINS` | URL del panel/app, separadas por coma, p. ej. `https://vacty-panel.vercel.app` |
| `COOKIE_SAMESITE` | `Strict` si panel y backend comparten dominio; `None` si no (prod usa `None` por defecto) |
| `MQTT_BROKER_URL` | `tcp://host:1883`, o `ssl://host:8883` con TLS |
| `MQTT_USERNAME` / `MQTT_PASSWORD` | Credenciales del broker |
| `MQTT_TOPIC` | `iot/telemetry` |
| `PORT` | Lo pone la plataforma; por defecto 8080 |

**Notas sobre Supabase:**
- Usa el *Session pooler* (botón **Connect** del dashboard). La conexión directa `db.<ref>.supabase.co` solo funciona por IPv6, y la mayoría de servicios de despliegue no lo soportan.
- Los proyectos gratuitos se pausan por inactividad. Ábrelo antes de una demo.
- Las tablas se crean con `ddl-auto=update`. Las nuevas son `readings`, `alerts`, `vaccine_profiles`, `container_profiles` y `sessions` (sesiones de login; las vencidas se borran cada hora).

## Desplegar

La imagen se construye con el `Dockerfile` (Temurin 25, perfil `prod`). Se necesita:

1. **Backend**: cualquier servicio que construya un Dockerfile (Render, Railway, Fly.io o una VM con Docker), con las variables de arriba.
2. **Broker MQTT público**: el ESP32 tiene que llegar a él desde cualquier red. Hay dos opciones:
   - *VM propia*: Mosquitto con `password_file` y `allow_anonymous false`, puertos 1883 u 8883.
   - *Administrado* (HiveMQ Cloud, EMQX Cloud): TLS en 8883 con usuario y clave; en el ESP32, `MQTT_USE_TLS 1`.

La opción más simple es **una sola VM con Docker** que corra el backend y Mosquitto (descomenta el servicio `backend` en `docker-compose.yml`), conectada a Supabase.

## Tests

```bash
./mvnw test                               # tests del motor de reglas (no necesitan base de datos)
RUN_CONTEXT_TEST=true ./mvnw test         # además, el test de contexto (necesita BD y Mosquitto)
```
