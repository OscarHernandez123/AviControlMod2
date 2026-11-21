# Integración REST directa entre M2 y M3

Estado: código a454b66 desplegado y verificado en la EC2 de M2 el 20 de noviembre de 2026. El acceso HTTPS para M3 está operativo en https://3.21.155.197 y limitado a su IP de salida. La interfaz administrativa conserva el túnel SSH. Queda pendiente la prueba desde el entorno de M3; ver [HTTPS sin dominio](https-modulo3-sin-dominio.md). No cambia el módulo 1 ni el módulo 3. No necesita migraciones nuevas de base de datos.

## Revisión del repositorio de M3

El equipo informó la rama `feature/AV3-48-sincronizacion-m1-rest`, commit `280e2d7`, PR #50 y documentos del PR #49. Al actualizar las referencias remotas, ambos PR ya aparecen fusionados en `origin/develop`: `f1cce51` (#50) y `75306e3` (#49). No se cambió el árbol de trabajo de M3.

La revisión remota confirma que M3 ya consulta M1; la consulta a M2 y la publicación del aviso siguen pendientes en su implementación. Las respuestas siguientes son el contrato preparado en M2 que debe recibir el equipo de M3 antes de construir su cliente. No se afirma compatibilidad entre despliegues hasta la prueba coordinada.

## Autenticación y alcance

Para la entrada pública HTTPS, M3 no envía credenciales: se autoriza su IP de salida `52.15.95.91`. El proxy utiliza internamente la siguiente identidad de servicio para conservar los permisos del backend privado; no compartirla con M3:

```text
AVICONTROL_MODULO3_ENABLED=true
AVICONTROL_MODULO3_USERNAME=modulo3_consulta
AVICONTROL_MODULO3_PASSWORD=<secreto definido en el entorno>
```

La cuenta está desactivada por defecto y no tiene contraseña predeterminada. Al activarla sin contraseña, el arranque falla. La identidad interna utiliza Basic Auth y rol MODULO3; las solicitudes públicas de M3 se identifican por IP en el proxy. Puede consultar las tres rutas siguientes, la proyección de alimento por lote y la consulta existente de resultado por orden. Además, puede confirmar el uso de un resultado mediante el único POST documentado abajo. No puede registrar ni corregir resultados, alterar consumos, ni acceder a otras rutas privadas. El administrador conserva acceso a las nuevas consultas.

## Rutas GET

Base propuesta: `/api/integracion/modulo3/lotes/{loteId}`.

| Sufijo | Respuesta |
|---|---|
| `/resultado-sacrificio` | Objeto con `id`, `ordenId`, `galponId`, `loteId`, `cantidadFinalPollos`, `pesoTotalKg`, `estado`, `fechaRegistro`, `fechaCorreccion` |
| `/alimentos` | `{loteId, partidas: [...]}` con suministros reales, separados por recepción |
| `/medicamentos` | `{loteId, partidas: [...]}` con consumos reales, separados por recepción |

La proyección permanece separada en `GET /api/lotes/{loteId}/alimento-requerido`, con acceso de lectura MODULO3. Conserva etapas, kilogramos proyectados y costo unitario de referencia; no se sustituye por suministros reales. No entrega impuesto de recepción porque una proyección no identifica una compra física.

Resultado inexistente: 404. Partidas inexistentes: lista vacía; no se inventan consumos ni se sustituyen por proyecciones. Un lote desconocido también devuelve listas vacías; la lista por sí sola no acredita que el lote exista.

Campos de cada partida de alimento:

- `suministroId`, `recepcionId`, `galponId`, `alimentoId`.
- `nombreAlimento`, `tipoAlimento`, `fechaAplicacion`.
- `cantidadKg`, `precioNetoKg`, `impuestoPorcentaje`, `valorImpuestoCop`, `valorImpuestoRecepcionCop`, `moneda`, `datosCompletos`.

Campos de cada partida de medicamento:

- `consumoId`, `recepcionId`, `galponId`, `medicamentoId`.
- `nombreMedicamento`, `fechaAplicacion`.
- `cantidadUnidadBase`, `unidadBase`, `precioNetoHistoricoUnidadBase`, `impuestoPorcentaje`, `valorImpuestoCop`, `valorImpuestoRecepcionCop`, `moneda`, `datosCompletos`.

Los precios son los guardados en el detalle del suministro/consumo. Los nombres y porcentajes de impuesto se obtienen de la recepción, que los flujos de edición de M2 protegen una vez consumida. `datosCompletos=false` indica que falta el nombre en una recepción histórica. No se reemplaza por un nombre ficticio.

Una partida se identifica por el par `(suministroId, recepcionId)` o `(consumoId, recepcionId)`. No consolidar solo por suministro/consumo: pueden abarcar recepciones con precios diferentes.

### Diferencias a acordar con M3

El esquema de M3 usa `id_partida_origen` UUID para alimentos. Este contrato conserva la identidad compuesta de cada tramo; el mapeo de esa identidad a su UUID debe acordarse antes de sincronizar, para no mezclar recepciones ni duplicar partidas.

M3 confirmó que necesita el impuesto como importe. Se exponen dos campos distintos:

- `valorImpuestoCop`: impuesto de la cantidad aplicada en ESTE tramo. Cálculo decimal exacto: cantidad × precio neto histórico unitario × porcentaje / 100, redondeado una sola vez a dos decimales con HALF_UP, conforme a la regla monetaria existente de M2.
- `valorImpuestoRecepcionCop`: impuesto total histórico guardado para la recepción completa. Es información de origen; no sumarlo repetidamente por cada aplicación ni utilizarlo como impuesto de un tramo parcial.

El porcentaje se mantiene como dato auxiliar: `19` significa 19 %. Por ejemplo, un tramo de 2 kg a 10 COP/kg con 19 % entrega `valorImpuestoCop=3.80`; el importe total de la recepción puede ser distinto. No se reparte el redondeo del impuesto total de compra entre tramos; la regla de valoración es por tramo y puede producir diferencias de centavos respecto de prorratear una recepción completa.

D-05 sigue pendiente: el equipo informó que M3 liquida actualmente sobre neto. M2 entrega precio neto e impuesto por separado y no cambia la fórmula contable de M3. El criterio de incluir o excluir impuesto debe quedar acordado por los responsables antes de dar por aceptada la liquidación.

Las partidas de medicamento se entregan planas por recepción; M3 puede agrupar por consumo manteniendo todos los tramos. No cambiar el modelo de M3 ni afirmar compatibilidad automática sin confirmar este mapeo.

## Confirmación directa por REST (opción B elegida)

M3 consulta M2 mediante GET y confirma que utilizó un resultado enviando un POST directamente a M2. Este intercambio no usa Kafka ni infraestructura de M1. No se modifica el código de M1 ni de M3. El equipo de M3 debe implementar su cliente conforme a este contrato.

```http
POST /api/integracion/modulo3/resultados-sacrificio/utilizado
Content-Type: application/json
```

Acceso público exclusivo desde la IP autorizada de M3, sin usuario ni contraseña. El proxy utiliza internamente el rol MODULO3 y sobrescribe cualquier Authorization del cliente. Un administrador, trabajador, veterinario o nutricionista no puede suplantar la confirmación de M3. La cuenta de integración se activa con las tres variables de autenticación descritas arriba; no tiene permisos generales de escritura.

```json
{
  "eventId": "11111111-1111-4111-8111-111111111111",
  "eventType": "ResultadoSacrificioUtilizado",
  "eventVersion": 1,
  "aggregateId": "22222222-2222-4222-8222-222222222222",
  "payload": {
    "resultadoId": "22222222-2222-4222-8222-222222222222",
    "loteId": "7331eec8-e447-4868-bdef-8eca3fff8d44"
  }
}
```

Los UUID del evento/resultado del ejemplo son ilustrativos, no datos insertados en AWS. `aggregateId` debe coincidir con `payload.resultadoId`. Todos los campos del ejemplo son obligatorios. `eventType` y `eventVersion` deben conservar exactamente los valores indicados. Los campos adicionales del sobre o del payload se ignoran.

Respuesta exitosa: **HTTP 200**, después de confirmar la transacción de M2:

```json
{
  "eventId": "11111111-1111-4111-8111-111111111111",
  "resultadoId": "22222222-2222-4222-8222-222222222222",
  "loteId": "7331eec8-e447-4868-bdef-8eca3fff8d44",
  "estado": "UTILIZADO"
}
```

Esta respuesta confirma que M2 bloqueó posteriores correcciones del resultado; no es una solicitud en cola. Repetir el mismo aviso devuelve 200 con el mismo contenido y conserva una sola confirmación y un solo cambio de versión. Dos reintentos simultáneos del mismo aviso también producen un solo efecto. No se exige que M3 esté disponible durante el registro original del resultado.

| HTTP | Situación | Acción del cliente M3 |
|---|---|---|
| 200 | Confirmación aplicada o ya registrada | Marcar el aviso como confirmado |
| 400 | JSON/campos inválidos, tipo/versión incorrectos o aggregateId distinto | Corregir el contrato; no tratarlo como aceptado |
| 401 | La identidad interna del proxy no está correctamente configurada | M2 debe revisar su configuración; M3 no debe enviar una contraseña |
| 403 | IP de salida no autorizada o permiso de ruta denegado | Confirmar que la solicitud sale de 52.15.95.91 y usa una ruta permitida |
| 404 | Resultado inexistente | Revisar el resultado consultado y el entorno |
| 409 | El resultado pertenece a otro lote o eventId ya se usó para otro resultado | Revisar identificadores; no generar automáticamente otro evento para ocultar el conflicto |

Si hay timeout, fallo de red o error de servidor, M3 debe conservar el aviso pendiente y reintentar con **el mismo eventId y el mismo cuerpo**: la respuesta puede haberse perdido después del commit. El bloqueo de resultado y la confirmación se guardan en una misma transacción usando la tabla existente de V18; no se agregan migraciones.

### Kafka heredado

`AVICONTROL_MODULO3_RESULTADO_UTILIZADO_ENABLED=false` mantiene apagado el listener Kafka provisional que ya existía antes de esta integración. No activarlo para la opción B. No se requiere broker, tópico ni autorización de M1 para comunicar M2 con M3. El Kafka utilizado por los flujos operativos de M2 hacia M1 conserva su configuración independiente.

## Pendientes para AWS y prueba coordinada

1. Acordar el mapeo de partidas y el criterio D-05 y compartir la ruta POST con M3 para que implemente su cliente REST.
2. Acceso REST HTTPS publicado y restringido a `52.15.95.91`; el equipo de M3 debe comprobarlo desde su EC2. PostgreSQL y el backend no publican sus puertos.
3. Compartir con M3 la base HTTPS `https://3.21.155.197/api/integracion/modulo3` y confirmar su IP de salida. No necesita credenciales; la interfaz administrativa sigue privada.
4. Revisar los datos existentes del lote `7331eec8-e447-4868-bdef-8eca3fff8d44`: resultado final con cantidad/peso reales y suministros/consumos asociados. Cualquier dato de prueba nuevo debe acordarse y registrarse mediante los flujos de negocio. No insertar cifras inventadas en AWS.
5. Consultar desde M3 y enviar un POST de confirmación coordinado. Verificar HTTP 200, UTILIZADO e imposibilidad de corrección posterior. Repetir el mismo aviso para comprobar idempotencia.

Las pruebas locales usan una base H2 aislada y valores sintéticos exclusivamente de prueba; no prueban conectividad AWS ni equivalencia del despliegue real de M3.

## Preparación del despliegue y comprobación sin escrituras

`deploy/aws/.env.example` incluye las variables de M3 desactivadas. Ambas plantillas Compose usan `env_file: .env`, por lo que transmitirán las variables cuando se configuren en el servidor. No copiar la plantilla sobre el `.env` existente: conserva las credenciales, la BD y las conexiones M1 ya configuradas. Activar solo la cuenta REST; mantener apagado el listener Kafka heredado de M3.

Nginx conserva la comprobación de identidad para la SPA, pero desactiva la subpetición de autenticación en `/api/`: cada petición se autentica y autoriza directamente en Spring Security. Esto permite que la cuenta M3 consulte sus rutas aunque no pueda acceder a la ruta de capacidades de M1 usada para autenticar la SPA. No elimina Basic Auth ni los permisos del backend. Requiere actualizar también la imagen de frontend; desplegar solo el JAR no elimina el bloqueo en Nginx.

Cuando la versión esté desplegada y el túnel SSH activo, ejecutar desde PowerShell local, en la raíz de M2:

```powershell
.\deploy\aws\modulo3\Comprobar-ConsultasM3.ps1
```

El script solicita las credenciales de M3, consulta el lote de prueba por defecto y muestra resultado, cantidad/peso y número de tramos de alimento/medicamento. Solo utiliza GET; no registra consumos, corrige resultados ni publica eventos. Para otro lote o un endpoint HTTPS acordado, usar `-LoteId` y `-BaseUrl`. HTTP se admite exclusivamente en loopback para el túnel local. No sigue redirecciones con las credenciales ni desactiva validación de certificados.

Un resultado ausente o listas vacías sirven para identificar datos pendientes; no completarlos con valores inventados. Las consultas exitosas no prueban el POST de confirmación: este último se verifica por separado con un resultado de prueba autorizado. El verificador GET no envía esa confirmación porque bloquearía la corrección del resultado.

## Revisión de datos en AWS — 20 de noviembre de 2026

Consulta PostgreSQL en transacción READ ONLY para `7331eec8-e447-4868-bdef-8eca3fff8d44`: cero resultados de sacrificio, cero suministros de alimento, cero consumos de medicamento y cero planes nutricionales. El historial de órdenes tiene una CANCELADA y una PROGRAMADA, esta última sin fecha de ejecución. La prueba anterior de vaciado no equivale al registro de cantidad/peso final. No se modificaron datos.

Antes de probar la liquidación, acordar el flujo válido para completar el lote y los valores de prueba. Crear los registros mediante los casos de uso correspondientes; no insertar filas directamente para forzar estados. Si el ciclo del lote ya está cerrado en M1, coordinar la preparación con su equipo antes de intentar registrar un plan o consumos nuevos.



## Validación local de la confirmación REST

La ejecución de `spotlessApply check bootJar` terminó correctamente: 209 pruebas aprobadas, 21 omitidas y ningún fallo. Las ocho pruebas nuevas verifican Basic Auth y roles, contrato y versión estricta, campos adicionales, resultado inexistente, lote incorrecto, reutilización incompatible de eventId, reintentos simultáneos y bloqueo de correcciones tras HTTP 200. Ambas plantillas Compose también validan sin iniciar contenedores. Estas comprobaciones no despliegan AWS ni envían confirmaciones a otro módulo.


## Despliegue verificado en AWS — 20 de noviembre de 2026

Se desplegó el código del commit `a454b66`, incluido en el PR #29 hacia `develop`:

- Backend: `avicontrol-m2-backend:m3-rest-a454b66`.
- Frontend/Nginx: `avicontrol-m2-frontend:m3-rest-a454b66`.
- Se conservó el contenedor PostgreSQL 16 y el mismo volumen. No se recreó la base de datos ni se ejecutaron migraciones nuevas.
- Las 35 tablas públicas (incluido el historial Flyway) mantienen los mismos conteos y huellas de contenido antes y después. La configuración previa ajena a M3 mantiene la misma huella; no se cambiaron credenciales existentes ni parámetros REST/Kafka de M1.
- M1: consultas REST HTTP 200 y respuesta Kafka ApiVersions correcta (61 APIs), sin publicar eventos de negocio.
- Administrador: SPA y listado de galpones HTTP 200; acceso anónimo a SPA HTTP 401.
- M3: consultas de alimento y medicamento HTTP 200; el lote de prueba devuelve listas vacías conforme a sus datos existentes. Resultado y plan ausentes devuelven 404.
- Confirmación REST: sin identidad 401, administrador 403 y cuenta M3 con UUID de resultado inexistente 404 estructurado. No se confirmó ningún resultado real ni se bloquearon correcciones de datos del usuario.
- Cuenta `modulo3_consulta` activada con contraseña aleatoria guardada solo en el `.env` del servidor (permisos 600). El listener Kafka de M3 continúa apagado.

Checkpoint del servidor: `/home/ec2-user/avicontrol-modulo2/backups/m3-rest-a454b66-20261121-021853`. El nombre utiliza UTC; la actualización corresponde al 20 de noviembre en Colombia. Contiene el dump, copias protegidas de configuración, comparación de tablas e informe `verification.json`.

Copia local del dump: `build/aws-deploy/m3-rest-a454b66-20261121-021853/database.dump` (ignorada por Git). Tamaño: 87 996 bytes. SHA256 coincidente en servidor y copia local: `14719bd170d73782081a425947b4ae8d26010323cf54d208f8a059b267d796be`. El archivo pg_dump se validó mediante pg_restore -l antes de aplicar las imágenes.

Las imágenes anteriores se conservaron para reversión. El procedimiento del servidor `/home/ec2-user/avicontrol-modulo2/deploy_m3_rest.py` permite restaurar su configuración anterior con `python3 deploy_m3_rest.py rollback`, sin eliminar volúmenes ni restaurar destructivamente la BD.

La actualización inicial conservó la publicación en `127.0.0.1:8088`. Posteriormente se habilitó HTTPS por IP para M3, con certificado público y renovación automática: ver [configuración y evidencia](https-modulo3-sin-dominio.md). M3 ya no necesita recibir credenciales: falta únicamente su primera prueba desde la EC2 autorizada. La comunicación no requiere infraestructura de M1.
