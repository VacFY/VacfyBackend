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

## API para el front

- **Base:** `http://localhost:8080` en local; en producción, la URL del backend desplegado. Todas las rutas REST empiezan con `/api/v1`.
- **Formato:** JSON. Fechas en ISO-8601 UTC (`2026-10-07T14:03:00Z`), temperatura en °C, humedad en %.
- **Swagger:** `/swagger-ui.html` permite probar todo desde el navegador.

### Sesión (cookie)

El login **no devuelve un token en el cuerpo**. El backend responde con `Set-Cookie: access-token=…` (HttpOnly, dura 12 h) y el navegador la envía sola en cada petición. Para eso, todas las llamadas deben ir con credenciales:

```js
fetch(`${API}/api/v1/profile`, { credentials: 'include' });
// axios
const api = axios.create({ baseURL: API, withCredentials: true });
```

- El origen del front debe estar en `ALLOWED_ORIGINS`.
- Si el front y el backend están en dominios distintos, la cookie solo viaja con HTTPS y `COOKIE_SAMESITE=None`. El perfil `prod` ya usa `None`.
- **Rutas públicas:** `sign-up`, `sign-in`, `/actuator/health`, Swagger y los WebSockets. Todo lo demás responde **401** si no hay sesión válida (sin cookie, vencida o cerrada). Ante un 401, manda al usuario al login.
- JavaScript no puede leer la cookie. Para saber si hay sesión, llama a `GET /api/v1/profile`: 200 = hay sesión, 401 = no hay.

### Errores

Todavía no hay un manejador global de errores, así que los códigos son estos:

| Código | Cuándo |
|---|---|
| 400 | JSON mal formado, falta un parámetro obligatorio, o fallan las validaciones de alertas, lecturas o perfiles de vacuna (detalladas abajo) |
| 401 | No hay sesión |
| 404 | `GET /profile`, `GET /device` o `PATCH /alerts/{id}/acknowledge` cuando el recurso no existe |
| 500 | **Cualquier otro error de negocio:** DNI o contraseña incorrectos, DNI ya registrado, campo vacío, dispositivo inexistente… |

El cuerpo del error es el genérico de Spring y **no trae el motivo**:

```json
{ "timestamp": "2026-10-07T14:03:00.000+00:00", "status": 500, "error": "Internal Server Error", "path": "/api/v1/authentication/sign-in" }
```

Por eso conviene validar los campos en el front antes de enviarlos y elegir el mensaje según el endpoint y el código. Por ejemplo, un 500 en `sign-in` casi siempre significa "DNI o contraseña incorrectos".

### Autenticación — `/api/v1/authentication`

| Método | Ruta | Sesión | Body | Respuesta |
|---|---|---|---|---|
| POST | `/sign-up` | No | `{ "userDni", "userPassword" }` | **201** sin cuerpo; deja la cookie (queda logueado) |
| POST | `/sign-in` | No | `{ "userDni", "userPassword" }` | **200** sin cuerpo; deja la cookie |
| POST | `/sign-out` | Sí | — | **200**; cierra la sesión y borra la cookie |
| POST | `/reset-password` | Sí | `{ "currentPassword", "newPassword" }` | **200** sin cuerpo |

```json
{ "userDni": "12345678", "userPassword": "miClave123" }
```

- `sign-up` también crea el perfil del usuario, con nombre, apellido y empresa en `"Undefined"`.
- Responden **500**: DNI ya registrado o vacío (`sign-up`), DNI o contraseña incorrectos (`sign-in`) y `currentPassword` incorrecta (`reset-password`).
- El backend **acepta contraseñas vacías**: valídalas en el front. Una contraseña de más de 72 bytes da 500.
- `reset-password` en realidad es "cambiar contraseña": pide la actual y no cierra la sesión. No hay recuperación por correo.

### Perfil — `/api/v1/profile` (requiere sesión)

| Método | Ruta | Body | Respuesta |
|---|---|---|---|
| GET | `/api/v1/profile` | — | **200** perfil · **404** si no existe |
| PUT | `/api/v1/profile` | `{ "profileName", "profileLastName", "profileCompany" }` | **200** sin cuerpo |
| PATCH | `/api/v1/profile/dni` | `{ "profileDni" }` | **200** sin cuerpo |

```json
{ "profileDni": "12345678", "profileName": "Ana", "profileLastName": "Quispe", "profileCompany": "Posta Santa Rosa" }
```

- Un usuario recién registrado tiene `"Undefined"` en nombre, apellido y empresa. Trátalo como vacío y pídele completar el perfil.
- `PUT` exige los tres campos, sin vacíos (si falta uno, 500). Para cambiar solo uno, envía los otros con su valor actual.
- `PATCH /dni` también cambia el DNI con el que se inicia sesión. Si el DNI ya está en uso, responde 500.

### Dispositivo — `/api/v1/device` (requiere sesión)

| Método | Ruta | Body | Respuesta |
|---|---|---|---|
| GET | `/api/v1/device` | — | **200** dispositivo · **404** si no tiene |
| POST | `/api/v1/device` | `{ "deviceName", "deviceConnectionAddress" }` | **200** sin cuerpo (no devuelve el id) |
| PUT | `/api/v1/device` | `{ "deviceName", "deviceConnectionAddress" }` | **200** sin cuerpo |
| DELETE | `/api/v1/device/{deviceId}` | — | **200** sin cuerpo |

```json
{ "deviceId": "8d0c6a1e-2b7f-4c1a-9f3e-5a6b7c8d9e0f", "deviceName": "Termo 1", "deviceConnectionAddress": "192.168.1.50" }
```

- **Un solo dispositivo por usuario.** Cada `POST` crea uno nuevo y, si el usuario llega a tener dos, `GET` y `PUT` responden 500. Usa `POST` solo cuando `GET` devuelva 404; para editar, usa `PUT`.
- Después del `POST`, llama a `GET` para obtener el `deviceId`.
- Los dos campos son obligatorios y no pueden estar vacíos (si no, 500). `deviceConnectionAddress` es texto libre: el backend no lo usa.
- `DELETE` con un id que no es del usuario, o que no es un UUID, responde 500.
- El dispositivo no está vinculado al código `contenedor` de la telemetría.

### Alertas — `/api/v1/alerts` (requiere sesión)

**`GET /api/v1/alerts?status=OPEN&contenedor=001`** devuelve hasta 200 alertas, de la más reciente a la más antigua.

| Parámetro | Obligatorio | Valores |
|---|---|---|
| `status` | No | `OPEN` (por defecto: `ACTIVE` + `ACKNOWLEDGED`), `ACTIVE`, `ACKNOWLEDGED`, `RESOLVED` o `ALL`. Otro valor da 400 |
| `contenedor` | No | Código del contenedor (p. ej. `001`). Sin él, trae todos |

**`PATCH /api/v1/alerts/{id}/acknowledge`** (sin body) marca la alerta como vista y devuelve **200** con la alerta actualizada, o **404** si no existe. Solo cambia las alertas `ACTIVE`; si ya estaba vista o resuelta, la devuelve igual. El cambio también se envía por `/ws/alerts`.

Cada alerta tiene esta forma (los campos que no aplican vienen en `null`):

```json
{
  "id": 42,
  "contenedor": "001",
  "type": "OUT_OF_RANGE",
  "severity": "WARNING",
  "status": "ACKNOWLEDGED",
  "message": "Temperatura por encima del máximo: 9.1 °C (rango PAI estándar 2–8 °C: 2.0 – 8.0 °C).",
  "triggerValue": 9.1,
  "minValue": 8.7,
  "maxValue": 10.2,
  "startedAt": "2026-10-07T14:03:00Z",
  "acknowledgedAt": "2026-10-07T14:05:12Z",
  "acknowledgedBy": "3f1c2b4a-7d8e-4f60-9a1b-2c3d4e5f6a7b",
  "resolvedAt": null,
  "resolutionMessage": null
}
```

| Campo | Significado |
|---|---|
| `type` | `OUT_OF_RANGE`, `RAPID_CHANGE`, `SENSOR_OFFLINE` o `INVALID_READING` (ver [Motor de alertas](#motor-de-alertas)) |
| `severity` | `WARNING` o `CRITICAL` (riesgo de congelación) |
| `status` | `ACTIVE` → `ACKNOWLEDGED` → `RESOLVED`. Puede pasar de `ACTIVE` a `RESOLVED` sin que nadie la vea |
| `message` | Texto en español, listo para mostrar |
| `triggerValue` | Temperatura que abrió la alerta. `null` en `SENSOR_OFFLINE` y a veces en `INVALID_READING` |
| `minValue` / `maxValue` | Temperatura mínima y máxima registradas mientras la alerta estuvo abierta |
| `acknowledgedBy` | Id (UUID) del usuario que la marcó como vista. El backend no guarda su nombre |
| `resolutionMessage` | Texto de cierre, p. ej. `"Temperatura de vuelta en rango: 6.8 °C."` |

### Lecturas — `GET /api/v1/readings` (requiere sesión)

`GET /api/v1/readings?contenedor=001&from=2026-10-07T00:00:00Z&to=2026-10-07T12:00:00Z`

| Parámetro | Obligatorio | Nota |
|---|---|---|
| `contenedor` | Sí | Si falta, 400 |
| `from` / `to` | No | ISO-8601 UTC. Por defecto, las últimas 24 h hasta ahora. Una fecha inválida da 400 |

```json
[
  { "id": 1520, "contenedor": "001", "temperatura": 5.4, "humedad": 61.0, "receivedAt": "2026-10-07T11:59:30Z" }
]
```

- Vienen **de la más reciente a la más antigua**. Para graficar, invierte el arreglo.
- `humedad` puede venir en `null`.
- **Máximo 1000 lecturas por consulta.** Como se guarda una cada 30 s, 1000 lecturas cubren unas 8 h: con el rango por defecto de 24 h solo llegan las ~8 h más recientes. Para rangos largos, pide tramos de 8 h o menos.

### Perfiles de vacuna (requiere sesión)

| Método | Ruta | Body | Respuesta |
|---|---|---|---|
| GET | `/api/v1/vaccine-profiles` | — | **200** lista ordenada por nombre |
| POST | `/api/v1/vaccine-profiles` | `{ "name", "minTemp", "maxTemp", "freezeSensitive" }` | **201** con el perfil creado |
| PUT | `/api/v1/containers/{contenedor}/profile` | `{ "profileId" }` | **200** `{ "contenedor", "profileId" }` |

```json
{ "id": 1, "name": "PAI estándar 2–8 °C", "minTemp": 2.0, "maxTemp": 8.0, "freezeSensitive": true }
```

- El perfil `PAI estándar 2–8 °C` siempre existe (se crea al arrancar). Los contenedores sin perfil asignado usan ese.
- `POST` responde **400** si falta el nombre o ya existe, si falta `minTemp` o `maxTemp`, o si `minTemp >= maxTemp`. `freezeSensitive` es opcional (por defecto, `false`).
- `PUT /containers/{contenedor}/profile` responde **400** si el perfil no existe. Si el contenedor ya tenía un perfil, lo reemplaza.
- No hay endpoints para editar o borrar perfiles, ni para consultar qué perfil tiene asignado un contenedor.

### Tiempo real (WebSockets)

| URL | Qué envía |
|---|---|
| `/ws/device` | Cada lectura que llega del ESP32, sin el muestreo de 30 s: `{ "contenedor": "001", "temperatura": 5.4, "humedad": 61.0 }`. `humedad` puede ser `null` |
| `/ws/alerts` | Una alerta por mensaje, con la misma forma que en REST. Al conectarse llegan primero las alertas abiertas; después, cada alerta que se abre, se marca como vista o se resuelve |

- Usa `ws://localhost:8080/ws/...` en local y `wss://<backend>/ws/...` en producción.
- No piden sesión, pero el origen del front debe estar en `ALLOWED_ORIGINS`.
- Solo el servidor envía mensajes; lo que mande el cliente se ignora.
- En `/ws/alerts`, actualiza tu lista por `id`: una alerta con `status: "RESOLVED"` ya se cerró.
- Si la conexión se cae, reconéctate. Al reconectar, `/ws/alerts` vuelve a enviar las alertas abiertas.

```js
const ws = new WebSocket(`${WS}/ws/alerts`);
ws.onmessage = (e) => {
  const alert = JSON.parse(e.data);
  alertsById[alert.id] = alert;
};
```

### Salud

`GET /actuator/health` es público y responde con `"status": "UP"` cuando el backend está listo.

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
