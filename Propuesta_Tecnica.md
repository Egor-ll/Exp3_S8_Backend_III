# Propuesta Técnica — BancoXYZ

## Implementación de microservicios, OAuth2.0, resiliencia, Docker y Kafka — Semana 8

**Asignatura:** Desarrollo Backend III (PBY2203)  
**Actividad:** Desarrollando microservicios y resiliencia en la nube con Spring Cloud  
**Grupo:** 13

---

# 1. Objetivo

La propuesta extiende BancoXYZ hacia una arquitectura distribuida preparada para ejecución reproducible mediante contenedores, incorporando seguridad OAuth2.0, tolerancia a fallos, mensajería y observabilidad.

Se conserva `bank-backend` como responsable de la lógica bancaria y persistencia, mientras los tres Backend for Frontend independientes mantienen contratos específicos para Web, Mobile y ATM.

Para Semana 8 se incorporan o consolidan las siguientes capacidades:

1. Authorization Server OAuth2.0.
2. OAuth2 Login y Resource Server.
3. Dockerización de los microservicios.
4. Docker Compose como mecanismo de orquestación.
5. Resilience4j.
6. Apache Kafka.
7. Kafka UI.
8. Spring Boot Actuator.
9. Evidencia funcional mediante navegador, consola y Postman.

---

# 2. Arquitectura propuesta

```text
                              +----------------------+
                              |  Authorization Server |
                              |       :9000           |
                              |       OAuth2.0        |
                              +----------+-----------+
                                         |
                         OAuth2 / JWT    |
                                         |
        +----------------+---------------+----------------+
        |                |                                |
+-------v-------+ +------v-------+                 +------v-------+
|   BFF Web     | | BFF Mobile   |                 |  BFF ATM     |
| HTTPS :8081   | | HTTPS :8082  |                 | HTTPS :8083  |
| OAuth2 Login  | | Resource     |                 | Resource     |
|               | | Server       |                 | Server       |
+-------+-------+ +------+-------+                 +------+-------+
        |                 |                                |
        +-----------------+----------------+---------------+
                                          |
                              Discovery + LoadBalancer
                                          |
                                +---------v---------+
                                |   Bank Backend    |
                                |     HTTP :8080    |
                                +----+----------+---+
                                     |          |
                               +-----v---+   +--v-------+
                               |Postgres |   |  Kafka   |
                               |  :5432  |   |  :29092  |
                               +---------+   +----+-----+
                                                 |
                                            +----v-----+
                                            | Kafka UI  |
                                            |   :8090   |
                                            +----------+
```

Los servicios de configuración y descubrimiento complementan la arquitectura:

```text
Config Server   :8888
Eureka Server   :8761
```

---

# 3. Componentes

| Componente | Puerto | Responsabilidad |
|---|---:|---|
| `auth-server` | `9000` | Authorization Server OAuth2 |
| `config-server` | `8888` | Configuración centralizada |
| `discovery-server` | `8761` | Service Discovery |
| `bank-backend` | `8080` | API bancaria |
| `bff-web` | `8081` | BFF para Web |
| `bff-mobile` | `8082` | BFF para Mobile |
| `bff-atm` | `8083` | BFF para ATM |
| `postgres` | `5432` | Persistencia |
| `kafka` | `9092/29092` | Mensajería |
| `kafka-ui` | `8090` | Observación de Kafka |

---

# 4. Configuración centralizada

Spring Cloud Config Server continúa proporcionando configuración externa a los BFF.

Las configuraciones incluyen:

```text
bff-web.properties
bff-mobile.properties
bff-atm.properties
```

Se externalizan:

- URL lógica del Bank Backend;
- timeout;
- Eureka;
- LoadBalancer;
- Circuit Breaker;
- Retry;
- Rate Limiter;
- Actuator;
- parámetros de observabilidad.

Esta estrategia permite ajustar políticas operativas sin modificar directamente el código de los BFF.

---

# 5. Service Discovery

Se utiliza Eureka Server en:

```text
http://discovery-server:8761/eureka/
```

Durante la ejecución participan en Discovery:

```text
BANK-BACKEND
BFF-WEB
BFF-MOBILE
BFF-ATM
```

Los BFF no dependen de una IP fija para localizar el Bank Backend. Spring Cloud LoadBalancer utiliza la información registrada en Eureka.

---

# 6. Backend for Frontend

## 6.1 BFF Web

Puerto:

```text
8081 HTTPS
```

Utiliza OAuth2 Login y mantiene la sesión autenticada del usuario.

Endpoints principales:

```text
GET /api/web/cuentas
GET /api/web/cuentas/{id}
GET /api/web/cuentas/{id}/detalle
```

## 6.2 BFF Mobile

Puerto:

```text
8082 HTTPS
```

Utiliza OAuth2 Resource Server para validar Bearer Tokens.

Endpoints principales:

```text
GET /api/mobile/cuentas
GET /api/mobile/cuentas/{id}/resumen
```

## 6.3 BFF ATM

Puerto:

```text
8083 HTTPS
```

Utiliza OAuth2 Resource Server.

Endpoints principales:

```text
GET  /api/atm/cuentas/{id}/saldo
POST /api/atm/cuentas/{id}/retiro
```

Ningún BFF accede directamente a PostgreSQL.

---

# 7. OAuth2.0

## 7.1 Authorization Server

Se incorpora `auth-server` en el puerto:

```text
9000
```

El Authorization Server proporciona:

```text
/oauth2/authorize
/oauth2/token
/oauth2/jwks
```

El JWK Set permite que los Resource Server validen la firma de los JWT emitidos.

## 7.2 Usuario académico

```text
usuario
123456
```

## 7.3 Cliente Postman

```text
Client ID:
bancoxyz-postman

Client Secret:
bancoxyz-postman-secret

Redirect URI:
https://oauth.pstmn.io/v1/browser-callback
```

## 7.4 Cliente BFF Web

```text
Client ID:
bancoxyz-client

Redirect URI:
https://127.0.0.1:8081/login/oauth2/code/bancoxyz-client
```

## 7.5 Flujo validado

```text
Usuario
   |
   v
Authorization Server :9000
   |
   | Authorization Code
   v
Access Token
   |
   v
BFF / API protegida
   |
   v
Bank Backend
```

La prueba realizada con Postman utilizó un Bearer Token válido contra:

```text
GET http://127.0.0.1:8080/api/cuentas
```

Resultado:

```text
200 OK
```

y se recibieron datos reales de las cuentas.

---

# 8. Resilience4j

Los BFF utilizan la instancia:

```text
bankbackend
```

## 8.1 Circuit Breaker

Configuración:

```text
Tipo de ventana: COUNT_BASED
Tamaño: 5
Mínimo de llamadas: 3
Umbral de fallos: 50 %
OPEN: 10 segundos
HALF_OPEN: 2 llamadas permitidas
Transición automática: habilitada
```

Flujo:

```text
CLOSED
   |
   | fallos
   v
OPEN
   |
   | espera
   v
HALF_OPEN
   |
   | respuestas correctas
   v
CLOSED
```

## 8.2 Retry

```text
max-attempts = 3
wait-duration = 200 ms
```

Se utiliza para fallos técnicos de operaciones de lectura.

El retiro ATM no utiliza Retry automático debido a que no es idempotente.

## 8.3 Rate Limiter

```text
10 permisos
1 segundo
timeout de adquisición: 0 ms
```

El Rate Limiter se mantiene separado del Circuit Breaker para que un exceso de tráfico no se interprete como una falla del backend.

## 8.4 Timeout

```text
backend.timeout = 3s
```

## 8.5 Fallback

Ante indisponibilidad del Bank Backend, el BFF genera una respuesta controlada:

```text
503 Service Unavailable
```

La prueba real produjo:

```json
{
  "detail": "El Bank Backend no está disponible durante: obtener el listado de cuentas",
  "instance": "/api/web/cuentas",
  "status": 503,
  "title": "Bank Backend no disponible"
}
```

Después de iniciar nuevamente `bank-backend`, la misma operación volvió a entregar los datos normalmente.

---

# 9. Docker

La solución incorpora Dockerfiles para los microservicios Java:

```text
auth-server
config-server
discovery-server
bank-backend
bff-web
bff-mobile
bff-atm
```

La infraestructura adicional utiliza:

```text
postgres:17
apache/kafka:4.0.0
provectuslabs/kafka-ui:latest
```

Los contenedores se comunican mediante:

```text
bancoxyz-net
```

Los healthchecks permiten que Docker Compose espere la disponibilidad de las dependencias antes de iniciar los servicios dependientes.

---

# 10. Docker Compose

El `docker-compose.yml` orquesta el entorno completo.

Servicios definidos:

```text
postgres
kafka
kafka-ui
auth-server
config-server
discovery-server
bank-backend
bff-web
bff-mobile
bff-atm
```

La configuración incluye:

- red interna;
- healthchecks;
- dependencias condicionadas por salud;
- volumen persistente PostgreSQL;
- variables de entorno;
- repositorio de configuración montado en Config Server;
- conexión interna Kafka;
- Kafka UI conectado al broker.

Validación:

```powershell
docker compose config
```

Ejecución:

```powershell
docker compose up -d --build
```

---

# 11. Kafka y Kafka UI

Kafka se ejecuta como broker único en configuración KRaft.

Comunicación interna:

```text
kafka:29092
```

Puerto externo:

```text
localhost:9092
```

Kafka UI:

```text
http://localhost:8090
```

Kafka UI se conecta al broker mediante:

```text
kafka:29092
```

Durante la validación se observó:

```text
Cluster: bancoxyz
Estado: Online
Brokers: 1
Topics: 2
Partitions: 51
Offline clusters: 0
```

Esto proporciona una evidencia visual del funcionamiento del componente Kafka y de su integración con Docker Compose.

---

# 12. Observabilidad

Se utiliza Spring Boot Actuator.

Endpoints relevantes:

```text
/actuator/health
/actuator/info
/actuator/metrics
/actuator/circuitbreakers
/actuator/circuitbreakerevents
/actuator/retries
/actuator/retryevents
/actuator/ratelimiters
/actuator/ratelimiterevents
```

La exposición de los endpoints administrativos permanece protegida por la configuración de seguridad correspondiente.

---

# 13. Verificaciones funcionales

## 13.1 OAuth2

Se obtuvo un Access Token mediante el Authorization Server y se utilizó desde Postman como Bearer Token.

Petición:

```text
GET http://127.0.0.1:8080/api/cuentas
```

Resultado:

```text
200 OK
```

## 13.2 Resilience4j

Secuencia validada:

```text
Bank Backend activo
        |
        v
GET /api/web/cuentas
        |
        v
Respuesta normal
        |
        v
Bank Backend detenido
        |
        v
GET /api/web/cuentas
        |
        v
503 Bank Backend no disponible
        |
        v
Bank Backend recuperado
        |
        v
GET /api/web/cuentas
        |
        v
Respuesta normal
```

## 13.3 Kafka

Kafka UI:

```text
Online
1 broker
2 topics
51 partitions
```

---

# 14. Correspondencia con la pauta de Semana 8

| Criterio | Puntaje | Implementación |
|---|---:|---|
| OAuth2.0 | 20 | Authorization Server, Authorization Code, JWT, OAuth2 Login, Resource Server y prueba con Postman |
| Docker | 20 | Dockerfiles para los microservicios Java |
| Docker Compose | 20 | Orquestación de infraestructura y microservicios con healthchecks y dependencias |
| Resilience4j | 20 | Circuit Breaker, Retry, Rate Limiter, timeout y fallback |
| Kafka/JMS | 15 | Apache Kafka operativo y Kafka UI para observabilidad |
| Código, documentación y evidencias | 5 | Código fuente, README, propuesta técnica y evidencias de ejecución |

**Total: 100 puntos**

---

# 15. Evidencias de entrega

Las evidencias recomendadas son:

1. Docker Compose con los servicios levantados.
2. Auth Server operativo.
3. Login OAuth2 del BFF Web.
4. Postman con Bearer Token y `200 OK`.
5. Bank Backend detenido y respuesta `503`.
6. Bank Backend recuperado y respuesta normal.
7. Kafka UI mostrando cluster `bancoxyz`.
8. Kafka UI mostrando los topics.
9. Eureka con los servicios registrados.
10. Actuator y métricas de resiliencia.

Estas evidencias complementan el código fuente y la documentación técnica.

---

# 16. Reproducibilidad

Desde la raíz:

```powershell
docker compose config
docker compose up -d --build
docker compose ps
```

Para Kafka UI:

```text
http://localhost:8090
```

Para Auth Server:

```text
http://localhost:9000
```

Para Bank Backend:

```text
http://127.0.0.1:8080
```

Para BFF Web:

```text
https://127.0.0.1:8081
```

Para BFF Mobile:

```text
https://127.0.0.1:8082
```

Para BFF ATM:

```text
https://127.0.0.1:8083
```

---

# 17. Despliegue Cloud

La solución está preparada para ser trasladada a una instancia AWS EC2 utilizando Docker y Docker Compose.

La validación funcional documentada en esta propuesta corresponde al entorno local.

La etapa siguiente consiste en:

1. publicar la versión final en GitHub;
2. preparar la nueva instancia EC2;
3. clonar el repositorio;
4. ejecutar Docker Compose;
5. validar los healthchecks;
6. comprobar OAuth2, BFF, Kafka y resiliencia en el entorno Cloud.

No se presenta la ejecución en EC2 como validada hasta completar esas pruebas.

---

# 18. Decisiones técnicas relevantes

### OAuth2 centralizado

El Authorization Server concentra el flujo de autenticación y emisión de tokens.

### BFF por canal

Web, Mobile y ATM mantienen contratos independientes.

### Discovery

Eureka evita acoplar los BFF a una dirección fija del Bank Backend.

### Retry seguro

Se limita a operaciones de lectura para evitar efectos secundarios duplicados.

### Rate Limiter independiente

El exceso de tráfico genera `429` sin convertirlo en una falla del backend.

### Fallback

Las fallas de infraestructura conocidas se traducen en respuestas HTTP controladas.

### Docker Compose

Permite reproducir la arquitectura completa en un entorno limpio.

### Kafka UI

Permite demostrar visualmente el estado del broker y sus topics durante la evaluación.

---

# 19. Evolución productiva

La implementación actual está orientada a la actividad académica. Para producción se recomienda:

- proveedor OAuth2/OIDC administrado;
- JWT con políticas de expiración y rotación adecuadas;
- Secret Manager;
- certificados emitidos por una CA;
- TLS interno;
- observabilidad centralizada;
- trazabilidad distribuida;
- alertamiento;
- Testcontainers;
- CI/CD;
- configuración separada por ambientes.

---

# 20. Conclusión

BancoXYZ evoluciona en Semana 8 hacia una solución distribuida que combina:

```text
Spring Cloud
+
OAuth2.0
+
Docker
+
Docker Compose
+
Resilience4j
+
Kafka
+
Kafka UI
+
Actuator
```

La implementación fue validada funcionalmente en los puntos principales:

- OAuth2 con token válido y acceso `200 OK`;
- BFF Web protegido;
- respuesta controlada `503` ante caída de `bank-backend`;
- recuperación posterior del backend;
- Kafka operativo;
- Kafka UI conectado y mostrando el cluster;
- servicios ejecutándose mediante Docker Compose.

La arquitectura mantiene la separación de responsabilidades construida en las semanas anteriores y agrega las capacidades exigidas para la actividad de Semana 8.

---

**BancoXYZ · Semana 8 — Microservicios, OAuth2.0, resiliencia, Docker y Kafka.**
