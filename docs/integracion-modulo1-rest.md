# Consultas REST al Módulo 1

El perfil `modulo1-rest` activa los cuatro puertos de consulta remota de galpones y lotes. No combinar con `local` ni `test`: esos perfiles conservan los simuladores JDBC. Sin ninguno de esos perfiles, los adaptadores de indisponibilidad siguen siendo la alternativa segura.

## Ejecución en PowerShell

Con el Módulo 1 disponible en el puerto 8080:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'modulo1-rest'
$env:AVICONTROL_MODULO1_BASE_URL = 'http://localhost:8080'
$env:SERVER_PORT = '8081'
.\gradlew.bat bootRun
```

La URL es la del servidor, sin `/api/v1`. En contenedores usar el nombre del servicio accesible desde el backend, no localhost. Las credenciales de PostgreSQL del Módulo 2 siguen configurándose mediante `AVICONTROL_DB_URL`, `AVICONTROL_DB_USERNAME` y `AVICONTROL_DB_PASSWORD`.

Los tiempos de espera se configuran mediante `AVICONTROL_MODULO1_CONNECT_TIMEOUT` (2s) y `AVICONTROL_MODULO1_READ_TIMEOUT` (5s por petición). El cliente solo envía GET, no reintenta automáticamente y no incorpora credenciales de usuarios del Módulo 2. La API revisada del Módulo 1 no requiere autenticación.

## Traducción del contrato

- `capacidad` se convierte a `aforoMaximo`. Una capacidad fuera del rango int se considera información no disponible, sin truncamiento.
- `loteActivo.nombre` se utiliza como etiqueta `loteCodigo`, porque el contrato no ofrece un código funcional separado.
- `loteActivo` omitido representa ausencia de lote; 404 de detalle representa ausencia de galpón. Otros errores HTTP, JSON inválido y errores de conexión se traducen a las excepciones de indisponibilidad existentes.
- Los estados se conservan exactamente, incluidos valores desconocidos. No se traduce PRODUCTIVO a PRODUCTIVA.
- Se recorren las páginas remotas de diez y se ordenan y paginan en memoria para conservar page desde 0, size hasta 100 y orden por nombre/estado/id del Módulo 2. El resumen también se calcula con esas páginas; no se invoca un endpoint de resumen inexistente. El costo crece con el número de galpones y las lecturas no constituyen una instantánea transaccional. Si cambia el listado durante el recorrido, una inconsistencia detectable se reporta como indisponibilidad.
- Se ignoran campos JSON nuevos. Los ejemplos de pruebas fueron copiados del repositorio del Módulo 1; las pruebas son independientes de esa carpeta.

## Alcance

No se cambió el Módulo 1. Los flujos de mortalidad, aislamiento, reintegro y vaciado se publican ahora mediante Kafka; consultar integracion-modulo1-kafka.md. El cambio a cosecha sigue siendo administrativo y el sacrificio sanitario requiere ampliar el contrato. Los casos de uso que todavía comparan PRODUCTIVA requieren la siguiente adaptación antes de habilitar esos flujos con datos remotos.

`GalponLoteQueryPort.bloquear` realiza una lectura remota: no bloquea filas del Módulo 1 ni garantiza atomicidad entre servicios. La consistencia de las escrituras deberá resolverse en la integración por eventos.

Las migraciones actuales del Módulo 2 aún crean tablas de simulación; este perfil deja de usarlas para lecturas, pero no elimina ni modifica esas migraciones.

El perfil modulo1-rest conserva el catálogo de demostración de alimentos y medicamentos para permitir el arranque completo. Ese catálogo no integra servicios externos y debe sustituirse antes de producción. Los simuladores JDBC de galpones y lotes permanecen desactivados.


La publicación de solicitudes mediante outbox y sus límites se describen en [integracion-modulo1-kafka.md](integracion-modulo1-kafka.md).
