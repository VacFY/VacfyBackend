# VacTy Backend

Spring Boot 4 (OpenJDK 25), monolito modular hexagonal.

| Módulo | Qué hace |
|---|---|
| `iam` | Registro e inicio de sesión. Token opaco en cookie, sesiones en la tabla `sessions` (Postgres) con expiración |
| `user` | Perfiles y dispositivos |
| `iot` | Recibe la telemetría por MQTT y la reenvía en vivo por `/ws/device` |
| `monitoring` | **Termos, vacunas, lotes y alertas**: termos vinculados a cada enfermera (código + clave), catálogo de vacunas, lotes de cada termo (lectura de códigos GS1), lecturas, motor de alertas de temperatura y vencimiento, dashboard y `/ws/alerts` |

Base de datos: PostgreSQL en **Supabase**. Broker: Mosquitto u otro broker MQTT.

## Motor de alertas

El backend genera **todas** las alertas (temperatura y vencimiento). El panel web y la app móvil solo las muestran.

### Rango de cada termo

1. **Con lotes activos:** el mínimo es el mayor de los mínimos de sus vacunas y el máximo, el menor de los máximos. El termo es sensible a la congelación (o al calor) si alguno de sus lotes lo es.
2. **Sin lotes:** el perfil asignado con `PUT /api/v1/containers/{contenedor}/profile`.
3. **Sin perfil:** `PAI estándar 2–8 °C`.

No se puede registrar un lote cuya vacuna no tenga un rango en común con los demás lotes del termo (409). Registrar o cerrar un lote recalcula el rango al instante.

### Reglas

| Regla | Se abre cuando… | Severidad | Se cierra cuando… |
|---|---|---|---|
| `OUT_OF_RANGE` | 3 lecturas seguidas fuera del rango del termo | `CRITICAL` si el frío afecta a una vacuna sensible a la congelación o el calor a una sensible al calor; si no, `WARNING`. El mensaje nombra los lotes en riesgo | Vuelve al rango con 0,5 °C de margen |
| `RAPID_CHANGE` | La temperatura cambia ≥ 2 °C en ≤ 5 min. Usa la mediana de 3 lecturas para ignorar picos del sensor | `WARNING` | Se estabiliza |
| `SENSOR_OFFLINE` | No llegan lecturas por 2 min | `WARNING` | Llega una lectura |
| `INVALID_READING` | 3 lecturas seguidas nulas o imposibles | `WARNING` | Llega una lectura válida |
| `LOT_EXPIRING` | Un lote vence en 30 días o menos | `WARNING`; la misma alerta sube a `CRITICAL` a los 7 días | Se cierra el lote, o vence (y se abre `LOT_EXPIRED`) |
| `LOT_EXPIRED` | Un lote venció: pasa a `EXPIRED` y deja de contar para el rango | `CRITICAL` | Se cierra el lote como `DISCARDED` |

- Cada episodio genera **una sola alerta** (por lote, en las de vencimiento), con estados `ACTIVE → ACKNOWLEDGED → RESOLVED`. Si una alerta sube a `CRITICAL`, vuelve a `ACTIVE` para que alguien la vea de nuevo.
- Los vencimientos se revisan al arrancar, todos los días a las 06:00 (hora de `VACTY_TIMEZONE`) y al registrar un lote. Un lote vence al terminar el día de su fecha.
- Las lecturas se guardan **1 cada 30 s por contenedor**, y **todas** mientras hay una alerta abierta.
- Los umbrales se configuran en `application.yaml`, bajo `vacty.alerts.*` (`expiring-days`, `expiring-critical-days`, `expiry-check-cron`, …).

## API para el front

- **Base:** `http://localhost:8080` en local; en producción, la URL del backend desplegado. Todas las rutas REST empiezan con `/api/v1`.
- **Formato:** JSON. Fechas en ISO-8601 UTC (`2026-10-07T14:03:00Z`), temperatura en °C, humedad en %.
- **Swagger:** `/swagger-ui.html` explica cada endpoint y permite probarlo desde el navegador: inicia sesión con *sign-in* y el navegador guarda la cookie.

### Sesión (cookie)

El login **no devuelve un token en el cuerpo**. El backend responde con `Set-Cookie: access-token=…` (HttpOnly, dura 12 h) y el navegador la envía sola en cada petición. Para eso, todas las llamadas deben ir con credenciales:

```js
fetch(`${API}/api/v1/profile`, { credentials: 'include' });
// axios
const api = axios.create({ baseURL: API, withCredentials: true });
```

- El origen del front debe estar en `ALLOWED_ORIGINS`.
- Si el front y el backend están en dominios distintos, la cookie solo viaja con HTTPS y `COOKIE_SAMESITE=None`. El perfil `prod` ya usa `None`.
- **Rutas públicas:** `sign-up`, `sign-in`, `/actuator/health` y Swagger. Todo lo demás, incluidos los WebSockets, responde **401** si no hay sesión válida (sin cookie, vencida o cerrada). Ante un 401, manda al usuario al login.
- **Cada usuario ve solo lo suyo:** la enfermera, los termos que tiene vinculados; el supervisor, todos (ver [Termos y vinculación](#termos-y-vinculación-requiere-sesión)).
- JavaScript no puede leer la cookie. Para saber si hay sesión, llama a `GET /api/v1/profile`: 200 = hay sesión, 401 = no hay.

### Errores

**Vacunas, lotes, termos y todos los 403 y 429** responden con el motivo listo para mostrar en `message`:

```json
{ "timestamp": "2026-10-07T17:03:15.278997Z", "status": 409, "error": "Conflict", "message": "No se puede guardar Varicela (congelada) en el termo 001: necesita -50,0 a -15,0 °C y no tiene un rango común con Pentavalente (lote AB1234, 2,0 a 8,0 °C). Guárdela en otro termo.", "path": "/api/v1/lots" }
```

**El resto de endpoints** todavía no tiene un manejador de errores, así que los códigos son estos:

| Código | Cuándo |
|---|---|
| 400 | JSON mal formado, falta un parámetro obligatorio, o fallan las validaciones de alertas, lecturas o perfiles de vacuna (detalladas abajo) |
| 401 | No hay sesión |
| 404 | `GET /profile`, `GET /device` o `PATCH /alerts/{id}/acknowledge` cuando el recurso no existe |
| 500 | **Cualquier otro error de negocio** en autenticación, perfil y dispositivo: DNI o contraseña incorrectos, DNI ya registrado, campo vacío, dispositivo inexistente… |

En esos endpoints, el cuerpo del error es el genérico de Spring y **no trae el motivo**:

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
{ "profileDni": "12345678", "profileName": "Ana", "profileLastName": "Quispe", "profileCompany": "Posta Santa Rosa", "role": "ENFERMERA" }
```

- Un usuario recién registrado tiene `"Undefined"` en nombre, apellido y empresa. Trátalo como vacío y pídele completar el perfil.
- `role` es `ENFERMERA` o `SUPERVISOR`: decide qué pantallas mostrar (ver [Termos y vinculación](#termos-y-vinculación-requiere-sesión)).
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
- El dispositivo no está vinculado al código `contenedor` de la telemetría. Para ligar termos a enfermeras se usa [Termos y vinculación](#termos-y-vinculación-requiere-sesión).

### Termos y vinculación (requiere sesión)

Cada termo queda vinculado a la enfermera que lo lleva, con **código + clave**. La enfermera ve solo sus termos; el supervisor (microred) ve todos.

**Roles.** `GET /api/v1/profile` devuelve `role`:

| Rol | Qué ve y qué puede hacer |
|---|---|
| `ENFERMERA` | Por defecto al registrarse. Ve solo sus termos vinculados: lecturas (desde que lo tiene), dashboard, alertas, lotes y WebSockets. Un termo ajeno responde **403** |
| `SUPERVISOR` | Ve todos los termos, los registra, genera sus claves y puede desvincular a cualquiera |

Para el MVP, el rol de supervisor se asigna directo en la base de datos (en Supabase: **SQL Editor**). La columna `role` se crea sola la primera vez que arranca esta versión; después basta con:

```sql
UPDATE credentials SET role = 'SUPERVISOR' WHERE user_dni = '12345678';
```

El cambio vale desde la siguiente petición, sin volver a iniciar sesión. Las cuentas anteriores, con `role` vacío, cuentan como `ENFERMERA`.

| Método | Ruta | Quién | Body | Respuesta |
|---|---|---|---|---|
| POST | `/api/v1/containers` | Supervisor | `{ "codigo", "nombre" }` | **201** con la clave en claro (única vez) · **400** · **403** · **409** ya existe |
| POST | `/api/v1/containers/{codigo}/regenerate-key` | Supervisor | — | **200** con la clave nueva (la anterior deja de servir) · **404** |
| GET | `/api/v1/containers` | Supervisor | — | **200** todos los termos con quién lo tiene y su estado |
| POST | `/api/v1/containers/link` | Cualquiera | `{ "codigo", "clave" }` | **200** · **400** código o clave incorrectos · **429** |
| POST | `/api/v1/containers/{codigo}/unlink` | Quien lo tiene, o el supervisor | — | **200** · **403** no es suyo · **409** no está asignado |
| GET | `/api/v1/my/containers` | Cualquiera | — | **200** mis termos |
| GET | `/api/v1/containers/{codigo}/assignments` | Supervisor, o quien lo tiene ahora | — | **200** historial · **403** |

**Registrar (supervisor).** El código es el que envía el ESP32 (1 a 32 letras, números, `-` o `_`). La respuesta trae la **clave de vinculación** en formato `XXX-XXX`, sin caracteres que se confunden (0/O, 1/I/L). **Solo se muestra esta vez**: imprímala en la etiqueta del termo. En la base de datos queda solo su hash (BCrypt).

```json
{ "codigo": "001", "nombre": "Termo Posta Huambos", "clave": "K7P-29Q", "activo": true, "creadoEn": "2026-10-07T21:28:49.230738Z" }
```

**Vincular (enfermera).** `{ "codigo": "001", "clave": "K7P-29Q" }`. La clave se acepta en mayúsculas o minúsculas, con o sin guion y espacios.

```json
{ "contenedor": "001", "nombre": "Termo Posta Huambos", "asignadoDesde": "2026-10-07T21:29:05.731544Z", "nuevaAsignacion": true }
```

- **Cambio de turno:** si otra enfermera tenía el termo, su asignación se cierra con `TOMADO_POR_OTRA` y se abre la nueva. Un termo tiene como máximo una asignación activa; una enfermera puede tener varios termos.
- Si ya era suyo, responde 200 con `nuevaAsignacion: false` y no duplica nada.
- Si el código o la clave no coinciden, el mensaje es siempre "Código o clave incorrectos.", sin decir cuál de los dos falló.
- Tras **5 intentos fallidos en 10 minutos**, por usuario o por termo, responde **429** hasta que pase la ventana.
- También acepta un código temporal del QR (ver [Emparejamiento por QR](#emparejamiento-por-qr-fase-oled-apagado-por-defecto)).
- Al vincular o desvincular, los afectados y los supervisores reciben `{"tipo":"ASIGNACION_CAMBIADA","contenedor":"001"}` por `/ws/alerts`.

**Mis termos.** `GET /api/v1/my/containers` devuelve, por termo, lo mismo que el dashboard más `nombre` y `asignadoDesde`:

```json
[
  {
    "contenedor": "001", "nombre": "Termo Posta Huambos", "asignadoDesde": "2026-10-07T21:29:05.731544Z",
    "status": "OK", "temperatura": 5.0, "humedad": 50.0, "lastReadingAt": "2026-10-07T21:29:03.104Z",
    "range": { "minTemp": 2.0, "maxTemp": 8.0, "basedOn": "PROFILE", "profileName": "PAI estándar 2–8 °C", "freezeSensitive": true, "heatSensitive": false },
    "activeLots": 0, "expiredLots": 0, "nextExpiry": null, "openAlerts": 0, "highestSeverity": null
  }
]
```

**Entregar.** `POST /api/v1/containers/{codigo}/unlink` cierra la asignación con `ENTREGADO`. Si lo hace el supervisor sobre el termo de otra persona, `DESVINCULADO_POR_SUPERVISOR`. Otra enfermera recibe 403.

**Todos los termos (supervisor).** Igual que "Mis termos", con `registrado`, `activo` y `asignadoA` (`{ userId, dni, nombre, desde }` o `null`). Un termo que envía telemetría sin estar registrado aparece con `registrado: false`: sus lecturas y alertas se guardan igual, pero nadie puede vincularlo hasta registrarlo.

**Historial.** `GET /api/v1/containers/{codigo}/assignments`, del más reciente al más antiguo:

```json
[
  { "id": 2, "contenedor": "001", "userId": "336d355b-…", "persona": { "userId": "336d355b-…", "dni": "33333333", "nombre": "Beto Rojas" }, "desde": "2026-10-07T21:29:35.288Z", "hasta": null, "motivoCierre": null },
  { "id": 1, "contenedor": "001", "userId": "31b96677-…", "persona": { "userId": "31b96677-…", "dni": "22222222", "nombre": "Ana Quispe" }, "desde": "2026-10-07T21:29:05.731Z", "hasta": "2026-10-07T21:29:35.288Z", "motivoCierre": "TOMADO_POR_OTRA" }
]
```

**Qué filtra el acceso.** Con un termo que no es suyo, la enfermera recibe **403** ("No tiene acceso al termo 002: vincúlelo primero con su clave.") en lecturas, lotes del termo, registrar o cerrar lotes, alertas con `contenedor`, marcar una alerta como vista y asignar perfil al termo. El dashboard, la lista de alertas y los lotes por vencer le muestran solo sus termos. Sus lecturas empiezan en `asignadoDesde`; el supervisor ve todo el historial.

### Dashboard — `GET /api/v1/dashboard/summary` (requiere sesión)

Pantalla de inicio de web y móvil, ordenada por código. El supervisor ve un elemento por termo conocido (registrado, o con telemetría reciente, perfil, lotes o alertas); la enfermera, solo los termos que tiene vinculados.

```json
{
  "generatedAt": "2026-10-07T17:03:41Z",
  "containers": [
    {
      "contenedor": "001",
      "status": "ALERTA",
      "temperatura": 1.2,
      "humedad": 52.0,
      "lastReadingAt": "2026-10-07T17:03:40.880953Z",
      "range": { "minTemp": 2.0, "maxTemp": 8.0, "basedOn": "LOTS", "profileName": null, "freezeSensitive": true, "heatSensitive": true },
      "activeLots": 3,
      "expiredLots": 0,
      "nextExpiry": { "lotId": 2, "vaccine": "Hepatitis B", "lotNumber": "HB77", "expiryDate": "2026-10-12", "daysToExpiry": 5 },
      "openAlerts": 3,
      "highestSeverity": "CRITICAL"
    }
  ]
}
```

| Campo | Significado |
|---|---|
| `status` | `SIN_DATOS` si nunca envió o lleva más de 2 min sin enviar (tiene prioridad); `ALERTA` si tiene alguna alerta abierta; `OK` en otro caso |
| `temperatura` / `humedad` | Última lectura válida (en memoria; tras un reinicio, la última guardada). `null` si nunca envió |
| `range` | Rango vigilado. `basedOn`: `LOTS` (calculado con los lotes) o `PROFILE` (perfil asignado o el de por defecto, con su `profileName`) |
| `activeLots` / `expiredLots` | Lotes activos y vencidos que siguen en el termo |
| `nextExpiry` | Lote activo que vence primero, o `null` |
| `openAlerts` / `highestSeverity` | Alertas abiertas y la severidad más alta (`null` si no hay) |

Para refrescarlo, vuelve a pedirlo cuando llegue un mensaje por `/ws/alerts`, o cada 30–60 s. La temperatura segundo a segundo llega por `/ws/device`.

### Alertas — `/api/v1/alerts` (requiere sesión)

**`GET /api/v1/alerts?status=OPEN&contenedor=001`** devuelve hasta 200 alertas, de la más reciente a la más antigua: las de temperatura y las de vencimiento. La enfermera solo ve las de sus termos; si pide un `contenedor` ajeno, recibe 403.

| Parámetro | Obligatorio | Valores |
|---|---|---|
| `status` | No | `OPEN` (por defecto: `ACTIVE` + `ACKNOWLEDGED`), `ACTIVE`, `ACKNOWLEDGED`, `RESOLVED` o `ALL`. Otro valor da 400 |
| `contenedor` | No | Código del contenedor (p. ej. `001`). Sin él, trae todos |

**`PATCH /api/v1/alerts/{id}/acknowledge`** (sin body) marca la alerta como vista y devuelve **200** con la alerta actualizada, o **404** si no existe. Solo cambia las alertas `ACTIVE`; si ya estaba vista o resuelta, la devuelve igual. El cambio también se envía por `/ws/alerts`.

#### Contrato de cada alerta (REST y `/ws/alerts`)

Web y móvil reciben el mismo JSON por los dos canales. Los campos que no aplican vienen en `null`; `affectedLots` es una lista vacía si la alerta no es de lotes concretos.

```json
{
  "id": 4,
  "contenedor": "001",
  "type": "OUT_OF_RANGE",
  "severity": "CRITICAL",
  "status": "ACTIVE",
  "title": "Riesgo de congelación en termo 001",
  "message": "1,2 °C: riesgo de congelación para Hepatitis B (lote HB77) y Pentavalente (lote AB1234). También fuera de rango: SPR (lote S1). Rango del termo: 2,0 – 8,0 °C. No use las vacunas hasta evaluarlas.",
  "affectedLots": [
    { "lotId": 2, "vaccine": "Hepatitis B", "lotNumber": "HB77", "expiryDate": "2026-10-12" },
    { "lotId": 4, "vaccine": "SPR", "lotNumber": "S1", "expiryDate": "2027-03-31" },
    { "lotId": 1, "vaccine": "Pentavalente", "lotNumber": "AB1234", "expiryDate": "2027-12-31" }
  ],
  "lotId": null,
  "triggerValue": 1.2,
  "minValue": 1.2,
  "maxValue": 1.2,
  "startedAt": "2026-10-07T17:03:40.880953Z",
  "acknowledgedAt": null,
  "acknowledgedBy": null,
  "resolvedAt": null,
  "resolutionMessage": null
}
```

| Campo | Para qué usarlo |
|---|---|
| `title` | Texto corto: notificación, encabezado de la tarjeta o fila de la lista |
| `message` | Texto completo para la enfermera, con qué hacer |
| `severity` | `WARNING` o `CRITICAL`: color, sonido y orden |
| `status` | `ACTIVE` → `ACKNOWLEDGED` → `RESOLVED`. Puede pasar de `ACTIVE` a `RESOLVED` sin que nadie la vea, y volver de `ACKNOWLEDGED` a `ACTIVE` si sube a `CRITICAL` |
| `type` | `OUT_OF_RANGE`, `RAPID_CHANGE`, `SENSOR_OFFLINE`, `INVALID_READING`, `LOT_EXPIRING` o `LOT_EXPIRED` (ver [Motor de alertas](#motor-de-alertas)). Si tu código hace un `switch`, deja un caso por defecto |
| `affectedLots` | Lotes en riesgo: vacuna, lote y vencimiento |
| `lotId` | Lote de una alerta de vencimiento: para el botón "Descartar" (`PATCH /api/v1/lots/{lotId}/close`). `null` en las de temperatura |
| `triggerValue` | Temperatura que abrió la alerta. `null` en `SENSOR_OFFLINE` y en las de vencimiento; a veces en `INVALID_READING` |
| `minValue` / `maxValue` | Temperatura mínima y máxima registradas mientras la alerta estuvo abierta |
| `acknowledgedBy` | Id (UUID) del usuario que la marcó como vista. El backend no guarda su nombre |
| `resolutionMessage` | Texto de cierre, p. ej. `"Temperatura de vuelta en rango: 5,0 °C."` o `"Lote descartado: Vencido."` |

Títulos por tipo:

| `type` | `title` de ejemplo |
|---|---|
| `OUT_OF_RANGE` | `Riesgo de congelación en termo 001` · `Riesgo por calor en termo 001` · `Temperatura alta en termo 001: 9,5 °C` |
| `RAPID_CHANGE` | `Cambio brusco de temperatura en termo 001` |
| `SENSOR_OFFLINE` | `Termo 001 sin datos` |
| `INVALID_READING` | `Sensor con fallas en termo 001` |
| `LOT_EXPIRING` | `Lote HB77 (Hepatitis B) vence el 12/10/2026` |
| `LOT_EXPIRED` | `Lote OLD1 (Neumococo) vencido` |

Las alertas guardadas antes de este cambio no tenían `title`: el backend les pone uno genérico según su tipo (p. ej. `Temperatura fuera de rango en termo 001`).

### Lotes (requiere sesión)

El registro tiene dos pasos: **leer** el código (no guarda nada) y **registrar** el lote.

| Método | Ruta | Body | Respuesta |
|---|---|---|---|
| POST | `/api/v1/lots/read` | `{ "code" }` | **200** datos leídos · **400** código ilegible |
| POST | `/api/v1/lots` | ver abajo | **201** lote · **400** · **409** |
| GET | `/api/v1/containers/{contenedor}/lots?includeExpired=false` | — | **200** lotes del termo, del que vence primero al último |
| GET | `/api/v1/lots/expiring?days=30` | — | **200** lotes activos de todos los termos que vencen en N días o menos |
| PATCH | `/api/v1/lots/{lotId}/close` | `{ "status": "USED" \| "DISCARDED", "reason" }` | **200** lote · **400** · **404** · **409** si ya estaba cerrado |

**Leer el código.** `code` acepta:
- el texto crudo del escáner, con o sin prefijo `]d2`, y con el separador GS (ASCII 29) después del lote o la serie cuando no van al final. `JSON.stringify` ya lo envía como `\u001d`. Si el escáner no puede enviar GS, también se acepta `<GS>`;
- el formato legible escrito a mano: `(01)08901234567890(17)271231(10)AB1234`;
- solo el GTIN (14 dígitos; también 8, 12 o 13, que se completan con ceros).

Se usan los AIs 01 (GTIN, con dígito verificador), 17 (vencimiento AAMMDD; día 00 = último día del mes), 10 (lote) y 21 (serie). Los demás se ignoran.

```json
{
  "gtin": "08901234567890",
  "lotNumber": "AB1234",
  "expiryDate": "2027-12-31",
  "serial": null,
  "daysToExpiry": 450,
  "vaccine": null,
  "knownProduct": false,
  "warnings": ["GTIN no registrado: elija la vacuna."]
}
```

- `knownProduct: true` → `vaccine` ya trae la vacuna. `false` → muestra el selector con `GET /api/v1/vaccines`; al registrar, el GTIN queda asociado a la vacuna elegida y la próxima lectura ya la reconoce.
- Muestra `warnings` tal cual: `Vencido el 31/01/2026: no se puede registrar.`, `Vence en 12 días.`, `Falta el número de lote: escríbalo.`, …

**Registrar.**

```json
{ "contenedor": "001", "vaccineId": 2, "lotNumber": "AB1234", "expiryDate": "2027-12-31", "vials": 20, "doses": 200, "gtin": "08901234567890", "source": "SCAN" }
```

| Campo | Obligatorio | Nota |
|---|---|---|
| `contenedor` | Sí | Código del termo |
| `vaccineId` | Sí | Id de `GET /api/v1/vaccines` |
| `lotNumber` | Sí | Hasta 20 caracteres; se guarda en mayúsculas |
| `expiryDate` | Sí | `AAAA-MM-DD`; no puede ser anterior a hoy (400) |
| `vials` | Sí | Frascos, 1 o más |
| `doses` | No | Dosis |
| `gtin` | No | Si es nuevo, queda asociado a la vacuna; si pertenece a otra vacuna, 409 |
| `source` | No | `SCAN`, `TYPED_CODE` o `MANUAL` (por defecto) |

Responde **409** si la vacuna no tiene un rango en común con los lotes del termo, si ese lote de esa vacuna ya está activo en el termo, o si el GTIN es de otra vacuna. Si el lote vence en 30 días o menos, la alerta `LOT_EXPIRING` sale al momento.

Cada lote (respuesta de registrar, cerrar y las listas):

```json
{
  "id": 2,
  "contenedor": "001",
  "vaccine": { "id": 3, "name": "Hepatitis B", "protectsAgainst": "Hepatitis B", "minTemp": 2.0, "maxTemp": 8.0, "freezeSensitive": true, "heatSensitive": false, "dosesPerVial": null, "notes": null, "verified": false, "careProfile": "ANTI_FREEZE", "careProfileLabel": "Anticongelamiento", "careInstructions": ["Se daña al congelarse.", "Usar paquetes fríos acondicionados.", "Evitar el contacto directo con el hielo."] },
  "gtin": null,
  "lotNumber": "HB77",
  "expiryDate": "2026-10-12",
  "daysToExpiry": 5,
  "vials": 10,
  "doses": null,
  "source": "MANUAL",
  "status": "ACTIVE",
  "registeredBy": "d5477e77-eab2-499c-bf91-6817865ac156",
  "registeredAt": "2026-10-07T17:03:15.141444Z",
  "closedAt": null,
  "closeReason": null
}
```

- `status`: `ACTIVE`, `USED`, `DISCARDED` o `EXPIRED` (venció y sigue en el termo: hay que descartarlo).
- `daysToExpiry` se calcula con la fecha de la posta (`VACTY_TIMEZONE`); es negativo si ya venció.
- `GET /containers/{contenedor}/lots` devuelve solo los `ACTIVE`; con `includeExpired=true` también los `EXPIRED`.
- Cerrar un lote (`USED` o `DISCARDED`) resuelve sus alertas de vencimiento y recalcula el rango del termo. Funciona con lotes `ACTIVE` y `EXPIRED`.

### Vacunas — `/api/v1/vaccines` (requiere sesión)

| Método | Ruta | Body | Respuesta |
|---|---|---|---|
| GET | `/api/v1/vaccines` | — | **200** catálogo ordenado por nombre |
| POST | `/api/v1/vaccines` | `{ "name", "protectsAgainst", "minTemp", "maxTemp", "freezeSensitive", "heatSensitive", "dosesPerVial", "notes", "careProfile", "careInstructions" }` | **201** · **400** · **409** nombre repetido |
| PUT | `/api/v1/vaccines/{id}` | los mismos campos y `verified` | **200** · **400** · **404** · **409** |

```json
{
  "id": 2,
  "name": "Pentavalente",
  "protectsAgainst": "Difteria, tos ferina, tétanos, hepatitis B y Haemophilus influenzae tipo b (Hib)",
  "minTemp": 2.0,
  "maxTemp": 8.0,
  "freezeSensitive": true,
  "heatSensitive": false,
  "dosesPerVial": null,
  "notes": null,
  "verified": false,
  "careProfile": "ANTI_FREEZE",
  "careProfileLabel": "Anticongelamiento",
  "careInstructions": ["Se daña al congelarse.", "Usar paquetes fríos acondicionados.", "Evitar el contacto directo con el hielo."]
}
```

| Campo | Significado |
|---|---|
| `freezeSensitive` / `heatSensitive` | Los usa el motor: definen cuándo una alerta de temperatura es `CRITICAL` |
| `careProfile` | Criterio de cuidado: `ANTI_FREEZE`, `PROTECT_FROM_LIGHT_AND_HEAT` o `CHECK_MANUFACTURER`. `null` si no se definió |
| `careProfileLabel` | Texto del criterio, listo para mostrar: "Anticongelamiento", "Proteger de luz y calor" o "Verificar fabricante" |
| `careInstructions` | Cuidados en el termo, uno por elemento; lista vacía si no hay. Para mostrarlos como viñetas |
| `verified` | `true` cuando alguien revisó los datos con la ficha técnica del fabricante |

**Semilla.** Al arrancar se crean, si no existen, estas 10 vacunas, todas con `verified: false`:

| Vacuna | Rango | Se daña al congelarse | Sensible al calor | `careProfile` |
|---|---|---|---|---|
| Pentavalente, Hepatitis B, Neumococo, Polio inactivada (IPV), Influenza inactivada, VPH | 2–8 °C | Sí | No | `ANTI_FREEZE` |
| SPR, BCG | 2–8 °C | No | Sí | `PROTECT_FROM_LIGHT_AND_HEAT` |
| Varicela, Rotavirus | 2–8 °C, según el producto | Depende del producto (se guarda como No) | Sí | `CHECK_MANUFACTURER` |

Cuidados de cada grupo (`careInstructions`):

- **Anticongelamiento:** Se daña al congelarse. Usar paquetes fríos acondicionados. Evitar el contacto directo con el hielo.
- **Proteger de luz y calor:** No se daña al congelarse (revisar la nota del fabricante). Usar paquetes fríos acondicionados. Evitar el contacto directo con el hielo no es imprescindible para el vial. Proteger de la luz.
- **Verificar fabricante:** Congelación: depende del producto. Rango de 2 a 8 °C según el producto: verificar con el fabricante. Usar paquetes fríos acondicionados. Contacto directo con el hielo: según el producto (en Rotavirus, según la formulación). Protección de la luz: según el producto.

**Hay que revisarlas con la ficha técnica del fabricante** y marcarlas con `PUT /api/v1/vaccines/{id}` `{"verified": true}`.

Si la base ya tiene las vacunas de una semilla anterior y nadie las verificó ni les puso cuidados, al arrancar se completan una sola vez: nombre ("Influenza" pasa a "Influenza inactivada"), para qué sirve, sensibilidad y cuidados. Se conservan su rango, dosis por frasco y notas; las que ya tienen cuidados o están verificadas no se tocan.

- En `POST` solo `name` es obligatorio; el rango por defecto es 2–8 °C.
- `PUT` cambia solo los campos que envíes; `careInstructions`, si se envía, reemplaza la lista completa. Si el nuevo rango deja a algún termo sin un rango común entre sus lotes, responde 409.
- Es la misma tabla que `/api/v1/vaccine-profiles` (que sigue funcionando igual); `/vaccines` no muestra el perfil genérico `PAI estándar 2–8 °C`.

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
- La enfermera solo ve las lecturas de sus termos, y **desde que se le asignó** (`asignadoDesde`); un termo ajeno da 403. El supervisor ve todo el historial.
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

- El perfil `PAI estándar 2–8 °C` siempre existe (se crea al arrancar). Los contenedores sin lotes ni perfil asignado usan ese.
- Es la misma tabla que el catálogo de `/api/v1/vaccines`, con menos campos: la lista también incluye esas vacunas.
- El perfil asignado a un termo solo se usa mientras **no tiene lotes activos**; con lotes, el rango sale de ellos.
- `POST` responde **400** si falta el nombre o ya existe, si falta `minTemp` o `maxTemp`, o si `minTemp >= maxTemp`. `freezeSensitive` es opcional (por defecto, `false`).
- `PUT /containers/{contenedor}/profile` responde **400** si el perfil no existe. Si el contenedor ya tenía un perfil, lo reemplaza.
- No hay endpoints para borrar perfiles ni para consultar qué perfil tiene asignado un contenedor. Para editar, usa `PUT /api/v1/vaccines/{id}`.

### Tiempo real (WebSockets)

| URL | Qué envía |
|---|---|
| `/ws/device` | Cada lectura que llega del ESP32, sin el muestreo de 30 s: `{ "contenedor": "001", "temperatura": 5.4, "humedad": 61.0 }`. `humedad` puede ser `null` |
| `/ws/alerts` | Una alerta por mensaje, con la misma forma que en REST. Al conectarse llegan primero las alertas abiertas; después, cada alerta que se abre, se marca como vista o se resuelve. También el aviso `{"tipo":"ASIGNACION_CAMBIADA","contenedor":"001"}` |

- **Piden sesión.** El handshake lleva la misma cookie `access-token` que la API REST. En el navegador se envía sola (con el mismo origen o `COOKIE_SAMESITE=None`). En la app móvil, guarda la cookie que llega en `Set-Cookie` al iniciar sesión y envíala en el header `Cookie` del handshake. Sin sesión válida, el handshake se rechaza (401).
- **Cada usuario recibe solo lo de sus termos:** la enfermera, los que tiene vinculados; el supervisor, todos. Si alguien toma tu termo, sus lecturas dejan de llegarte al momento.
- Usa `ws://localhost:8080/ws/...` en local y `wss://<backend>/ws/...` en producción. El origen del front debe estar en `ALLOWED_ORIGINS`.
- Solo el servidor envía mensajes; lo que mande el cliente se ignora.
- En `/ws/alerts`, un mensaje con `tipo` es un aviso; uno con `id` es una alerta. Actualiza tu lista por `id`: una alerta con `status: "RESOLVED"` ya se cerró.
- Si la conexión se cae, reconéctate. Al reconectar, `/ws/alerts` vuelve a enviar las alertas abiertas.

```js
const ws = new WebSocket(`${WS}/ws/alerts`);   // el navegador envía la cookie solo
ws.onmessage = (e) => {
  const msg = JSON.parse(e.data);
  if (msg.tipo === 'ASIGNACION_CAMBIADA') return refreshMyContainers();   // GET /api/v1/my/containers
  alertsById[msg.id] = msg;
};
```

```js
// App móvil (React Native): la cookie va en el header
const ws = new WebSocket(`${WS}/ws/device`, null, { headers: { Cookie: `access-token=${token}` } });
```

Secuencia real de `/ws/alerts` mientras se registraba un lote que vence en 5 días y el termo 001 bajaba a 1,2 °C (resumida a `id`, `type`, `severity`, `status` y `title`; cada mensaje trae el JSON completo de arriba):

```json
{"id":2,"type":"LOT_EXPIRING","severity":"CRITICAL","status":"ACTIVE","title":"Lote HB77 (Hepatitis B) vence el 12/10/2026"}
{"id":3,"type":"RAPID_CHANGE","severity":"WARNING","status":"ACTIVE","title":"Cambio brusco de temperatura en termo 001"}
{"id":4,"type":"OUT_OF_RANGE","severity":"CRITICAL","status":"ACTIVE","title":"Riesgo de congelación en termo 001"}
{"id":4,"type":"OUT_OF_RANGE","severity":"CRITICAL","status":"ACKNOWLEDGED","title":"Riesgo de congelación en termo 001"}
{"id":2,"type":"LOT_EXPIRING","severity":"CRITICAL","status":"RESOLVED","title":"Lote HB77 (Hepatitis B) vence el 12/10/2026"}
```

Una alerta de lote vencido completa:

```json
{
  "id": 6,
  "contenedor": "002",
  "type": "LOT_EXPIRED",
  "severity": "CRITICAL",
  "status": "ACTIVE",
  "title": "Lote OLD1 (Neumococo) vencido",
  "message": "No usar: lote OLD1 de Neumococo venció el 06/10/2026. Retírelo del termo 002 y regístrelo como descartado.",
  "affectedLots": [{ "lotId": 6, "vaccine": "Neumococo", "lotNumber": "OLD1", "expiryDate": "2026-10-06" }],
  "lotId": 6,
  "triggerValue": null,
  "minValue": null,
  "maxValue": null,
  "startedAt": "2026-10-07T17:04:15.132385Z",
  "acknowledgedAt": null,
  "acknowledgedBy": null,
  "resolvedAt": null,
  "resolutionMessage": null
}
```

### Salud

`GET /actuator/health` es público y responde con `"status": "UP"` cuando el backend está listo.

## Emparejamiento por QR (fase OLED, apagado por defecto)

Pensado para cuando el ESP32 tenga pantalla: el termo muestra un QR con un **código temporal** y la app lo escanea, sin escribir la clave. El backend ya está listo; el firmware no cambia hasta entonces.

Se activa con `PAIRING_MQTT_ENABLED=true` (`vacty.pairing.mqtt-enabled`). Así funciona:

1. El ESP32 publica en **`vacty/{contenedor}/pairing/request`** un mensaje vacío o `{}`.
2. Si el termo está registrado y activo, el backend genera un código de 6 caracteres, válido **5 minutos** (`vacty.pairing.code-ttl-seconds`). Guarda solo su hash, y un código nuevo reemplaza al anterior. Lo publica en **`vacty/{contenedor}/pairing/code`** (QoS 1, sin `retained`):

   ```json
   {"contenedor":"001","codigo":"K75NGJ","venceEn":"2026-10-07T21:35:05.234186Z","qr":"vacty:001:K75NGJ"}
   ```

3. El ESP32 muestra `qr` como código QR. Formato: **`vacty:<codigo del termo>:<clave>`**.
4. La app lee el QR, separa código y clave, y llama a `POST /api/v1/containers/link` con `{ "codigo": "001", "clave": "K75NGJ" }`. Ese endpoint acepta tanto la clave fija de la etiqueta como el código temporal. El temporal sirve **una sola vez**.

> **Antes de activarlo en producción:** el broker debe tener usuario y ACL por dispositivo, de modo que solo el termo 001 pueda leer `vacty/001/pairing/code` y publicar en `vacty/001/pairing/request`. Si no, cualquiera conectado al broker podría leer los códigos y vincularse.

## Probar la vinculación en Swagger, paso a paso

En local, con `./mvnw spring-boot:run`, abre http://localhost:8080/swagger-ui.html. Si el navegador no guarda la cookie por usar http, arranca con `COOKIE_SECURE=false`.

1. **Crea dos cuentas.** En *Authentication → POST /sign-up*, crea al supervisor (`{"userDni":"11111111","userPassword":"clave123"}`) y a la enfermera (`{"userDni":"22222222","userPassword":"clave123"}`).
2. **Haz supervisor a la primera.** En Supabase (*SQL Editor*): `UPDATE credentials SET role = 'SUPERVISOR' WHERE user_dni = '11111111';`
3. **Registra el termo 001.** Inicia sesión como supervisor (*POST /sign-in* con `11111111`). En *Containers → POST /api/v1/containers*, envía `{"codigo":"001","nombre":"Termo Posta Huambos"}` y **anota la clave** de la respuesta (p. ej. `K7P-29Q`).
4. **Vincula como enfermera.** Cierra sesión (*POST /sign-out*) e inicia sesión con `22222222`. En *POST /api/v1/containers/link*, envía `{"codigo":"001","clave":"K7P-29Q"}`.
5. **Mira "Mis termos".** *GET /api/v1/my/containers* muestra el 001. *GET /api/v1/dashboard/summary* muestra solo ese termo, y *GET /api/v1/readings?contenedor=002* responde 403.
6. **Mira la temperatura en vivo por WebSocket.** Con la sesión de la enfermera abierta en Swagger, abre la consola del navegador (F12) en esa misma pestaña y escribe:

   ```js
   const ws = new WebSocket(location.origin.replace('http', 'ws') + '/ws/device');
   ws.onmessage = (e) => console.log(JSON.parse(e.data));
   ```

   Cuando el ESP32 del termo 001 publique, verás `{contenedor: '001', temperatura: …, humedad: …}`. Sin el ESP32, simúlalo: `mosquitto_pub -t iot/telemetry -m '{"contenedor":"001","temperatura":5.2,"humedad":60}'`. Una lectura del 002 no aparece, porque no es suyo.
7. **Cambio de turno (opcional).** Con otra enfermera, vincula el 001 con la misma clave: la primera recibe `{"tipo":"ASIGNACION_CAMBIADA","contenedor":"001"}` por `/ws/alerts` y deja de recibir sus lecturas. Como supervisor, *GET /api/v1/containers/001/assignments* muestra el historial con `TOMADO_POR_OTRA`.

## Ejemplos con curl

Los ids de vacunas son de ejemplo: tómalos de `GET /api/v1/vaccines`. La cookie de sesión se guarda en `cookies.txt`.

```bash
API=http://localhost:8080
J='Content-Type: application/json'

# Iniciar sesión (guarda la cookie)
curl -c cookies.txt -H "$J" -d '{"userDni":"12345678","userPassword":"miClave123"}' $API/api/v1/authentication/sign-in

# Leer un código escaneado: 01 + 17 + GS + 10, con prefijo ]d2 (en JSON, GS se escribe \u001d)
curl -b cookies.txt -H "$J" -d '{"code":"]d2010890123456789017271231\u001d10AB1234"}' $API/api/v1/lots/read
# → {"gtin":"08901234567890","lotNumber":"AB1234","expiryDate":"2027-12-31","serial":null,"daysToExpiry":450,
#    "vaccine":null,"knownProduct":false,"warnings":["GTIN no registrado: elija la vacuna."]}

# Leer un código escrito a mano
curl -b cookies.txt -H "$J" -d '{"code":"(01)08901234567890(17)271231(10)AB1234"}' $API/api/v1/lots/read

# Un GTIN con el dígito verificador mal (…892 en vez de …890) → 400
curl -b cookies.txt -H "$J" -d '{"code":"(01)08901234567892(17)271231(10)AB1234"}' $API/api/v1/lots/read
# → {"status":400,"error":"Bad Request","message":"GTIN inválido: 08901234567892 no pasa el dígito verificador. Revise el número.", …}

# Registrar el lote escaneado (el GTIN queda asociado a Pentavalente para la próxima lectura)
curl -b cookies.txt -H "$J" -d '{"contenedor":"001","vaccineId":2,"lotNumber":"AB1234","expiryDate":"2027-12-31","vials":20,"doses":200,"gtin":"08901234567890","source":"SCAN"}' $API/api/v1/lots

# Registrar un lote a mano, sin GTIN
curl -b cookies.txt -H "$J" -d '{"contenedor":"001","vaccineId":3,"lotNumber":"HB77","expiryDate":"2027-06-30","vials":10,"source":"MANUAL"}' $API/api/v1/lots

# Lote vencido → 400
curl -b cookies.txt -H "$J" -d '{"contenedor":"001","vaccineId":4,"lotNumber":"NM1","expiryDate":"2026-01-31","vials":5}' $API/api/v1/lots
# → {"status":400,"message":"El lote NM1 venció el 31/01/2026: una vacuna vencida no se registra. Sepárela para su descarte.", …}

# Lote incompatible con el termo → 409 (una vacuna congelada en un termo de 2–8 °C)
curl -b cookies.txt -H "$J" -d '{"name":"Varicela (congelada)","protectsAgainst":"Varicela","minTemp":-50,"maxTemp":-15,"heatSensitive":true}' $API/api/v1/vaccines
curl -b cookies.txt -H "$J" -d '{"contenedor":"001","vaccineId":12,"lotNumber":"VZ9","expiryDate":"2027-06-30","vials":5}' $API/api/v1/lots
# → {"status":409,"message":"No se puede guardar Varicela (congelada) en el termo 001: necesita -50,0 a -15,0 °C y no tiene
#    un rango común con Hepatitis B (lote HB77, 2,0 a 8,0 °C) y Pentavalente (lote AB1234, 2,0 a 8,0 °C). Guárdela en otro termo.", …}

# Lotes de un termo, por vencer y dashboard
curl -b cookies.txt $API/api/v1/containers/001/lots
curl -b cookies.txt "$API/api/v1/lots/expiring?days=30"
curl -b cookies.txt $API/api/v1/dashboard/summary

# Descartar un lote (resuelve sus alertas de vencimiento)
curl -b cookies.txt -X PATCH -H "$J" -d '{"status":"DISCARDED","reason":"Vencido"}' $API/api/v1/lots/6/close

# Marcar una vacuna como revisada con la ficha técnica
curl -b cookies.txt -X PUT -H "$J" -d '{"verified":true}' $API/api/v1/vaccines/2

# Termos: el supervisor registra (guarda la clave de la respuesta) y la enfermera se vincula
curl -b sup.txt -H "$J" -d '{"codigo":"001","nombre":"Termo Posta Huambos"}' $API/api/v1/containers
# → {"codigo":"001","nombre":"Termo Posta Huambos","clave":"K7P-29Q","activo":true,…}
curl -b cookies.txt -H "$J" -d '{"codigo":"001","clave":"K7P-29Q"}' $API/api/v1/containers/link
curl -b cookies.txt $API/api/v1/my/containers
curl -b cookies.txt -X POST $API/api/v1/containers/001/unlink
```

## Notificaciones push (pendiente)

El backend ya decide **cuándo** avisar al celular: cada alerta que se abre o sube a `CRITICAL` pasa por el puerto de salida `NotificationSender` (`monitoring/application/internal/outboundservices`) con `title`, `body` (el `message`), `severity`, `type`, `contenedor` y `alertId`. Las alertas que se marcan como vistas o se cierran solo van por `/ws/alerts`.

Hoy la única implementación es `NoOpNotificationSender`, que solo deja una línea en el log. Para conectar **Firebase Cloud Messaging** falta:

1. **Credenciales.** Crear el proyecto en Firebase y una cuenta de servicio. Pasar su JSON al backend por una variable de entorno (p. ej. `FIREBASE_CREDENTIALS` en base64), nunca en el repo.
2. **Implementación.** Agregar la dependencia `com.google.firebase:firebase-admin` y una clase `FcmNotificationSender implements NotificationSender` en `monitoring/infrastructure/notifications`. Activarla solo cuando exista la variable (`@ConditionalOnProperty`) y marcar `NoOpNotificationSender` con `@ConditionalOnMissingBean(NotificationSender.class)` para que no haya dos.
3. **Tokens de los celulares.** Un endpoint, p. ej. `POST /api/v1/push-tokens`, que guarde el token FCM de cada usuario (tabla nueva), y borrarlo en `sign-out` o cuando FCM responda que el token ya no es válido.
4. **Destinatarios.** Avisar a la enfermera que tiene el termo (asignación activa en `container_assignments`) y a los supervisores, con el mismo criterio que `ContainerAccessService` usa para `/ws/alerts`.
5. **App móvil.** Pedir permiso de notificaciones, obtener el token con el SDK de Firebase y enviarlo al backend después del login. Al tocar la notificación, abrir la alerta con `alertId`. En Android, un canal de alta prioridad para `CRITICAL`; en iOS, cargar la clave APNs en Firebase.

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
| `VACTY_TIMEZONE` | Zona horaria de la posta, define qué día es "hoy" para los vencimientos; por defecto `America/Lima` |
| `PAIRING_MQTT_ENABLED` | `true` activa el emparejamiento por QR vía MQTT; por defecto `false` (ver [Emparejamiento por QR](#emparejamiento-por-qr-fase-oled-apagado-por-defecto)) |
| `PORT` | Lo pone la plataforma; por defecto 8080 |

**Notas sobre Supabase:**
- Usa el *Session pooler* (botón **Connect** del dashboard). La conexión directa `db.<ref>.supabase.co` solo funciona por IPv6, y la mayoría de servicios de despliegue no lo soportan.
- Los proyectos gratuitos se pausan por inactividad. Ábrelo antes de una demo.
- Las tablas se crean con `ddl-auto=update`: `readings`, `alerts`, `vaccine_profiles` (catálogo de vacunas), `container_profiles`, `vaccine_lots`, `vaccine_products` (GTIN → vacuna), `containers` (termos registrados), `container_assignments` (quién tiene cada termo) y `sessions` (sesiones de login; las vencidas se borran cada hora). En `credentials` se agrega la columna `role`, vacía en las cuentas existentes (cuentan como `ENFERMERA`).
- Al arrancar, `SchemaPatches` aplica tres ajustes que JPA no puede declarar: el índice único de lotes activos por termo, el índice único de una sola asignación activa por termo, y quitar el `CHECK` antiguo de `alerts.type`, que impediría guardar las alertas de vencimiento. Todos se pueden repetir sin problema.

## Desplegar

La imagen se construye con el `Dockerfile` (Temurin 25, perfil `prod`). Se necesita:

1. **Backend**: cualquier servicio que construya un Dockerfile (Render, Railway, Fly.io o una VM con Docker), con las variables de arriba.
2. **Broker MQTT público**: el ESP32 tiene que llegar a él desde cualquier red. Hay dos opciones:
   - *VM propia*: Mosquitto con `password_file` y `allow_anonymous false`, puertos 1883 u 8883.
   - *Administrado* (HiveMQ Cloud, EMQX Cloud): TLS en 8883 con usuario y clave; en el ESP32, `MQTT_USE_TLS 1`.

La opción más simple es **una sola VM con Docker** que corra el backend y Mosquitto (descomenta el servicio `backend` en `docker-compose.yml`), conectada a Supabase.

## Tests

```bash
./mvnw test                               # tests de dominio (no necesitan base de datos)
RUN_CONTEXT_TEST=true ./mvnw test         # además, el test de contexto (necesita BD y Mosquitto)
```

| Test | Qué cubre |
|---|---|
| `Gs1ParserTest` | Código crudo con GS y con `]d2`, con paréntesis, solo GTIN, día 00, dígito verificador inválido, lote al final sin GS, AIs ignorados |
| `ContainerLimitsCalculatorTest` | Rango combinado de los lotes, sensibilidad, lotes incompatibles |
| `LotExpiryRulesTest` | Avisos a 30 y 7 días, escalado a `CRITICAL`, lote vencido, sin duplicados |
| `AlertRuleEngineTest` | Reglas de temperatura, y mensajes y severidad con los lotes del termo |
| `ContainerLinkTest` | Registrar (solo supervisor), vincular con clave correcta o incorrecta, límite de intentos, cambio de turno, no duplicar, desvincular ajeno (403), supervisor desvincula, código temporal de un solo uso que vence, clave regenerada |
| `ContainerAccessServiceImplTest` | Supervisor ve todo, enfermera solo lo suyo, caché que se invalida al vincular |
| `AccessFilteringTest` | Lecturas (403 y desde la asignación), dashboard y alertas filtrados por usuario |
| `WebSocketFilteringTest` / `SessionHandshakeInterceptorTest` | `/ws/device` y `/ws/alerts` solo envían a quien puede ver el termo; handshake sin sesión rechazado |
| `PairingServiceImplTest` / `PairingTopicTest` / `ContainerKeysTest` | Códigos temporales, tópicos MQTT y formato de las claves |
