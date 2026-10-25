# General — Arquitectura y Stack Tecnológico

**Estado**: Referencia técnica transversal del Módulo 2  
**Fecha**: 25/09/2026

## Propósito

Este documento define la arquitectura, las tecnologías y las reglas técnicas comunes de AviControl Módulo 2. Es la fuente única para estas decisiones: los planes técnicos funcionales deben enlazar este archivo y documentar únicamente su alcance, modelos, contratos, reglas, estructura específica y tareas de implementación.

La configuración base del proyecto, incluido Gradle Wrapper, `build.gradle`, `settings.gradle`, Java y Spring Boot, se considera existente. Los planes funcionales no deben agregar tareas para reconstruir esa base.

## Stack tecnológico

| Área | Tecnología adoptada | Uso en el proyecto |
| --- | --- | --- |
| Lenguaje | Java 21 | Dominio, casos de uso, adaptadores y pruebas |
| Framework principal | Spring Boot 4.1.1 | Configuración, ejecución y composición de la aplicación |
| Construcción | Gradle Wrapper | Dependencias, compilación, pruebas y tareas de calidad |
| API HTTP | Spring Web MVC | Controladores y API REST síncrona |
| Validación | Jakarta Bean Validation | Validación de DTOs en el adaptador de entrada |
| Seguridad | Spring Security | Autenticación, autorización por rol y protección de endpoints |
| Persistencia | Spring Data JPA e Hibernate | Adaptadores de persistencia relacional |
| Base de datos | PostgreSQL | Datos propios del Módulo 2 y proyecciones de lectura |
| Migraciones | Flyway | Versionamiento y aplicación ordenada del esquema |
| Eventos internos | Spring Modulith | Eventos entre capacidades dentro de la aplicación modular |
| Eventos entre módulos | Apache Kafka con Spring Kafka | Integración asíncrona y contratos de eventos versionados |
| Serialización | Jackson | JSON para API REST y contratos de integración |
| Documentación de API | OpenAPI 3 | Descripción verificable de endpoints, entradas y respuestas |
| Pruebas unitarias | JUnit 5 y Mockito | Reglas de dominio y casos de uso aislados |
| Pruebas HTTP | MockMvc y Spring Boot Test | Contratos y autorización de la API REST |
| Pruebas de integración | Testcontainers | PostgreSQL y Kafka reales durante las pruebas |
| Utilidades | Lombok | Reducción de código repetitivo sin ocultar reglas de negocio |
| Calidad | Spotless y ArchUnit | Formato uniforme y verificación de límites arquitectónicos |
| Observabilidad | Spring Boot Actuator y Micrometer | Salud, métricas y diagnóstico operativo |
| Entorno local | Docker Compose | PostgreSQL y Kafka para desarrollo local |

Las versiones de librerías administradas por Spring Boot deben obtenerse de su gestión de dependencias. Solo se fija una versión explícita cuando no esté administrada o exista una razón técnica documentada.

## Arquitectura

El Módulo 2 se implementa como una **aplicación modular con arquitectura hexagonal**. Se despliega como una sola aplicación, pero cada capacidad funcional conserva límites explícitos y se comunica mediante puertos o eventos.

```text
src/main/java/com/avicontrol/
├── domain/
│   ├── model/
│   ├── event/
│   ├── exception/
│   └── port/out/
├── application/
└── infrastructure/
    ├── adapter/in/
    │   ├── rest/
    │   ├── event/
    │   └── scheduling/
    ├── adapter/out/
    │   ├── persistence/
    │   ├── event/
    │   └── integration/
    └── config/
```

### Dominio

`domain/` contiene modelos, objetos de valor, reglas, excepciones, eventos de dominio y puertos de salida. Debe ser Java puro y no puede importar Spring, JPA, Kafka, Jackson ni DTOs de infraestructura.

Las reglas que protegen invariantes pertenecen al dominio. Los modelos no deben convertirse en simples contenedores si poseen comportamiento de negocio.

### Aplicación

`application/` contiene los casos de uso. Cada caso de uso representa una responsabilidad concreta, coordina modelos y puertos, define el límite transaccional y no conoce detalles de HTTP, JPA o Kafka.

Los casos de uso reciben dependencias mediante interfaces. El actor autenticado y el tiempo actual se proporcionan como abstracciones; no se consultan directamente desde variables globales de infraestructura.

### Infraestructura

`infrastructure/` contiene los adaptadores:

- **Entrada REST**: controladores, DTOs, validación sintáctica y mappers.
- **Entrada por eventos**: consumidores que traducen contratos externos y ejecutan casos de uso.
- **Entrada programada**: tareas que activan casos de uso sin contener reglas de negocio.
- **Salida de persistencia**: entidades JPA, repositorios Spring Data y mappers.
- **Salida por eventos**: publicación y traducción hacia contratos de integración.
- **Integraciones**: clientes que implementan puertos hacia otros módulos o capacidades.

Los controladores, listeners, schedulers y repositorios no contienen reglas centrales del negocio.

## Regla de dependencias

```text
infrastructure  →  application  →  domain
```

- El dominio no depende de las demás capas.
- Aplicación depende del dominio y de sus puertos.
- Infraestructura implementa los puertos y puede depender de aplicación y dominio.
- Los DTOs REST, entidades JPA y mensajes Kafka son modelos diferentes y se convierten mediante mappers.
- No se inyectan repositorios JPA directamente en controladores o casos de uso.
- No se retornan entidades JPA desde la API.
- No se publican entidades JPA en eventos.

ArchUnit verifica automáticamente estas restricciones.

## Modularidad y propiedad de datos

Cada capacidad funcional es propietaria de sus agregados y tablas. Ningún módulo modifica directamente los datos propiedad de otro.

- `Galpón` y `Lote` pertenecen al Módulo 1.
- El Módulo 2 puede conservar proyecciones locales de solo lectura de esos datos.
- Las proyecciones se actualizan mediante eventos y no convierten al Módulo 2 en propietario de las entidades originales.
- Cuando una proyección no sea suficiente o no esté vigente, el caso de uso utiliza un puerto de consulta hacia el módulo propietario.
- No existen asignaciones entre trabajadores y galpones dentro del alcance confirmado.

La comunicación directa entre capacidades internas solo se realiza mediante interfaces públicas de aplicación. Los efectos desacoplados se comunican mediante eventos internos.

## Persistencia y transacciones

- PostgreSQL almacena los datos propios del Módulo 2 y sus proyecciones.
- Flyway es la única fuente para crear o modificar el esquema.
- Hibernate valida el esquema y no lo genera automáticamente en ambientes persistentes.
- Las restricciones de integridad, unicidad y valores permitidos se aplican tanto en el dominio como en PostgreSQL cuando corresponda.
- Cada caso de uso de escritura define una transacción local clara.
- Se utiliza bloqueo optimista como estrategia general y bloqueo pesimista únicamente cuando una regla de concurrencia lo exige.
- Una operación que modifica varias tablas propias debe confirmarse o revertirse por completo.
- Los historiales y movimientos no se eliminan para recalcular saldos; se compensan mediante nuevos movimientos cuando corresponda.

## Eventos

### Eventos internos

Spring Modulith gestiona eventos entre capacidades de la misma aplicación. Un evento interno expresa un hecho en pasado, por ejemplo `RecepcionAlimentoConfirmada`, y no se expone automáticamente fuera del módulo.

### Eventos de integración

Kafka transporta eventos entre módulos desplegables. Los contratos externos son DTOs versionados, separados de los eventos internos y de las entidades del dominio.

Todo evento de integración incluye:

```text
eventId
eventType
eventVersion
occurredAt
publishedAt
producer
correlationId
causationId
aggregateType
aggregateId
payload
```

Reglas generales:

- Los nombres describen hechos ocurridos y las versiones forman parte del contrato.
- `eventId` es la clave de idempotencia.
- Los consumidores toleran reentregas y eventos fuera de orden.
- `sourceVersion` controla el orden cuando se actualizan proyecciones de un agregado externo.
- La clave de partición corresponde al agregado cuyo orden debe preservarse.
- Los contratos contienen solo los datos requeridos por el consumidor.
- Un cambio incompatible crea una nueva versión del evento.

### Publicación confiable

Cuando una operación guarda información y publica un evento, ambos hechos se registran dentro de la misma transacción local mediante el registro de publicaciones de Spring Modulith. La entrega a Kafka puede repetirse; la ausencia de efectos duplicados se consigue mediante consumidores idempotentes.

No se utilizan transacciones distribuidas. Los procesos que cruzan módulos se representan con estados como `PENDIENTE`, `CONFIRMADO` y `RECHAZADO` cuando requieren seguimiento.

## API REST

- Los endpoints utilizan JSON y rutas versionadas cuando se publique un contrato externo estable.
- Los DTOs de entrada se validan mediante Bean Validation.
- Los valores derivados se calculan en dominio o aplicación y no se aceptan como campos editables.
- Los listados con crecimiento potencial incluyen paginación, filtros y orden explícito.
- OpenAPI documenta rutas, parámetros, cuerpos, respuestas y errores.

Los errores siguen `application/problem+json` y una estructura compatible con RFC 9457:

```json
{
  "type": "https://avicontrol/errors/recurso-no-encontrado",
  "title": "Recurso no encontrado",
  "status": 404,
  "detail": "No existe el recurso solicitado",
  "instance": "/api/recurso/uuid",
  "code": "RECURSO_NO_ENCONTRADO",
  "correlationId": "uuid",
  "fieldErrors": []
}
```

Mapeo general:

- `400`: estructura o campos inválidos.
- `401`: autenticación ausente o inválida.
- `403`: rol o pertenencia no autorizados.
- `404`: recurso inexistente.
- `409`: conflicto de estado, concurrencia o recurso ya utilizado.
- `422`: datos sintácticamente válidos que incumplen una regla calculable.
- `503`: integración imprescindible no disponible.

## Seguridad

- Spring Security resuelve autenticación y autorización en infraestructura.
- Los roles se expresan como autoridades explícitas, por ejemplo `ROLE_ADMINISTRADOR`, `ROLE_NUTRICIONISTA` y `ROLE_TRABAJADOR` cuando el caso de uso los necesite.
- Los casos de uso reciben una representación del actor y no dependen directamente de `SecurityContextHolder`.
- La autorización por propiedad o alcance se vuelve a validar en aplicación; no depende únicamente del controlador.
- Credenciales, secretos y direcciones de servicios se suministran mediante configuración externa.

## Cálculos, dinero y tiempo

- Dinero, pesos, volúmenes, cantidades decimales y porcentajes utilizan `BigDecimal`.
- Cada capacidad define escala, precisión y redondeo de forma centralizada y coherente.
- Los cálculos detectan divisiones inválidas y desbordamientos antes de persistir resultados.
- Las fechas sin hora utilizan `LocalDate`; los instantes auditables utilizan UTC.
- La zona de negocio es `America/Bogota`.
- Los casos de uso reciben un `Clock` y no llaman directamente a `LocalDate.now()` o `Instant.now()`.

## Estrategia de pruebas

- **Dominio**: JUnit 5 sin contexto Spring.
- **Casos de uso**: JUnit 5 y Mockito con puertos simulados.
- **REST**: MockMvc para contratos, validaciones, estados HTTP y seguridad.
- **Persistencia**: Testcontainers con PostgreSQL y migraciones Flyway reales.
- **Eventos**: Spring Modulith Test y Testcontainers con Kafka para publicación, consumo, duplicados y orden.
- **Arquitectura**: ArchUnit para verificar dependencias entre capas.
- **End-to-end**: recorridos completos solo para los flujos críticos.

Las pruebas se ejecutan mediante Gradle Wrapper. Una tarea no se considera terminada si rompe las pruebas existentes o las reglas de arquitectura.

## Observabilidad y operación

- Cada solicitud y evento propaga un `correlationId`.
- Los logs son estructurados y no contienen secretos ni payloads sensibles completos.
- Actuator expone verificaciones de salud para la aplicación, PostgreSQL y Kafka.
- Micrometer registra latencia, errores, publicaciones pendientes, reintentos y fallos de consumidores.
- Los eventos no procesables utilizan reintentos controlados y un mecanismo de recuperación después de agotar los intentos.
- Docker Compose proporciona PostgreSQL y Kafka en el entorno local.

## Convenciones para los planes técnicos

Todo plan funcional debe incluir al inicio:

```markdown
**Arquitectura y tecnologías**: [General.md](General.md)
```

Los planes funcionales no deben repetir:

- La lista completa de tecnologías.
- La explicación general de arquitectura hexagonal.
- La estructura común de capas.
- Las reglas generales de persistencia, eventos, seguridad, errores o pruebas.
- Tareas para configurar nuevamente la base técnica ya implementada.

Cada plan debe limitarse a:

- Specs incluidos y resumen funcional.
- Contexto, restricciones e integraciones específicas del feature.
- Clases y adaptadores propios del feature.
- Historias de usuario, pruebas y tareas concretas.
- Dependencias con otros features.
- Decisiones o excepciones que no estén cubiertas por este documento.
