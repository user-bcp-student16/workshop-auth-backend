# Diseño

## Contexto

Consulta `proposal.md` para conocer la motivación y las dos specs para los requisitos observables. El proyecto usa Java 21, Quarkus 3.27.4, REST/Jackson y SQLite JDBC. `pom.xml` no incluye una dependencia de JWT/JOSE.

El checkout no tiene un directorio `src/` y Git no registra archivos bajo `src/`. El snapshot de código fuente disponible en JaCoCo muestra un recurso de login que valida username/password, consulta SQLite mediante un `PreparedStatement` posicional y crea manualmente un token HS256 con `iss`, `aud`, `sub`, `role`, `iat` y `exp`; `LoginRequest` ya tiene un campo `client`. El reporte de Surefire registra solo tres pruebas: `logsInWithWorkshopUser`, `rejectsInvalidCredentialsContainingSqlPunctuation` y `createsJwtUsingSharedHmacContract`. Estos reportes generados son evidencia de una compilación anterior, no código fuente editable ni autoritativo.

El repositorio contiene información contradictoria: `docs/ENDPOINTS.md` describe un token legacy en Base64, mientras que el snapshot generado describe un JWT y `HALLAZGOS.md` documenta correcciones posteriores. La propuesta y las specs aprobadas definen el comportamiento objetivo. Antes de implementar, se debe restaurar o poner a disposición el código fuente canónico y ejecutar pruebas de caracterización contra este; no se deben modificar archivos generados bajo `target/` ni inferir el comportamiento actual del runtime solo a partir de esos reportes.

Los contratos protegidos son `POST /auth/login` sin autenticación previa; los campos de una respuesta exitosa `token`, `expiresIn`, `user.username`, `user.displayName`, `user.role` y `debug`; los comportamientos documentados para los estados 400/401/500; y la vigencia de 3600 segundos. La obligatoriedad de `client` y la firma del token son los cambios incompatibles explícitos de la propuesta. `GET /q/health`, el esquema de SQLite y los endpoints no relacionados permanecen sin cambios.

## Objetivos / Fuera de alcance

**Objetivos:**

- Mantener parametrizada la búsqueda de credenciales e impedir que una entrada con sintaxis SQL altere la búsqueda de cuentas.
- Emitir un JWT HS256 verificable con `sub`, `client`, `iat` y `exp`, conservar la vigencia de 3600 segundos y devolver `expiresIn` dentro de la tolerancia especificada de un segundo.
- Exigir un `client` no vacío ni compuesto solo por espacios, obtener la clave de firma de la configuración y evitar que credenciales, claves y tokens aparezcan en los logs.
- Realizar los cambios mínimos en los límites de solicitud, autenticación, persistencia y token; conservar la forma documentada de la respuesta HTTP y los códigos de estado.

**Fuera de alcance:**

- Agregar endpoints de token o salud, cambiar el esquema de SQLite o rediseñar la persistencia con un ORM.
- Cambiar el almacenamiento de contraseñas, CORS, los consumidores frontend/API o el contrato documentado para el error 500 como parte de este cambio.
- Refactorizar partes no relacionadas de la aplicación o aceptar tokens en dos formatos.

## Componentes afectados

- Límite de solicitud y HTTP: `LoginRequest`, `AuthResource` y el límite del servicio de autenticación existente, si está presente en las fuentes restauradas. Validar `client` junto con los campos obligatorios existentes y mantener el mapeo de la respuesta en el límite HTTP.
- Búsqueda de credenciales: `LegacyDatabase` o su equivalente actual en el repositorio. Mantener SQLite y la vinculación posicional de parámetros; no construir SQL a partir de valores de la solicitud.
- Emisión de tokens y configuración: `LegacyTokenUtil` o su reemplazo, `pom.xml` y la configuración de Quarkus para `auth.token.secret` / `AUTH_TOKEN_SECRET`.
- Pruebas y documentación del contrato: `AuthResourceTest` o su equivalente; después, actualizar `docs/ENDPOINTS.md` para reflejar los cambios incompatibles aprobados.
- No modificar `/q/health` ni los repositorios consumidores. Requirements API es únicamente un límite de integración.

Estos son objetivos lógicos basados en los archivos mencionados por la documentación del repositorio y el reporte generado. Sus fuentes no están presentes en este checkout, por lo que antes de implementar se debe confirmar la estructura canónica de componentes.

## Decisiones

### Usar una biblioteca JOSE para JWT HS256

Usar Nimbus JOSE + JWT para firmar y verificar tokens en las pruebas, en vez de mantener en el código de la aplicación la serialización JWT, el manejo de Base64URL y el ensamblado de HMAC. Mantener HS256 porque el repositorio documenta un `AUTH_TOKEN_SECRET` compartido con Requirements API y el snapshot generado usa HS256; obtener la clave solo mediante la configuración de Quarkus y rechazar claves ausentes o demasiado cortas. Agregar la dependencia únicamente después de confirmar su compatibilidad con las versiones base de Quarkus/Java y con la política de dependencias del proyecto.

Un algoritmo asimétrico reduciría la distribución de una clave compartida, pero requiere coordinar cambios en la clave y el verificador del consumidor externo, por lo que queda fuera de este contrato. Continuar con el JWT ensamblado manualmente evitaría agregar una dependencia, pero mantendría un manejo personalizado de tokens sensible para la seguridad, así que se descarta. Además de los claims obligatorios, conservar `iss`, `aud` y `role`, observados en el snapshot generado; no cambiar sus valores hasta caracterizar el código fuente canónico y el contrato con el consumidor.

### Mantener SQLite JDBC con parámetros vinculados

Conservar el límite de persistencia actual de SQLite/JDBC y vincular username y password como valores. El snapshot generado ya muestra `PreparedStatement`; primero verificarlo contra el código fuente canónico y cambiar la búsqueda solo si no cumple ese comportamiento. Un ORM o rediseño del esquema ampliaría las migraciones y las dependencias sin fortalecer el contrato de este endpoint.

### Validar `client` sin transformarlo

Rechazar con HTTP 400 los valores de `client` ausentes, vacíos o compuestos solo por espacios, antes de autenticar o emitir un token. Si el valor no está en blanco, incluirlo sin cambios en el claim; no agregar reglas de formato, longitud o normalización que no estén en HU-01.

### Derivar los tiempos del token de una sola lectura del reloj

Capturar una sola vez la hora de emisión, asignarla a `iat` y establecer `exp` 3600 segundos después. Calcular `expiresIn` a partir de esos mismos valores y de la hora de creación de la respuesta. Permitir controlar o inyectar el reloj en las pruebas para verificar de forma determinista la expiración y la tolerancia de un segundo; no agregar un endpoint para verificar tokens.

### Fallar de forma segura ante una configuración de firma inválida y redactar los logs

Usar `AUTH_TOKEN_SECRET` / `auth.token.secret` sin un valor predeterminado fuera del laboratorio. Rechazar una configuración inválida en vez de firmar silenciosamente con un valor alternativo. No registrar cuerpos de solicitud, contraseñas, claves de firma ni tokens; evitar registrar DTOs o mensajes de excepción que puedan contenerlos. Conservar los códigos de estado y la forma de respuesta exigidos por las specs aprobadas.

## Estrategia incremental y pruebas de caracterización

1. Restaurar o identificar las fuentes Java canónicas y ejecutar la suite actual antes de cambiar el comportamiento. Registrar la respuesta real del login, el tratamiento de `client`, el formato y los claims del token, los cuerpos de error, el mapeo de configuración y las expectativas de issuer/audience del consumidor. La documentación actual y el snapshot generado discrepan, así que no usar el snapshot como sustituto de esta línea base.
2. Caracterizar el endpoint existente antes de los cambios incompatibles: forma de la respuesta y campos del usuario en un login válido; username y password nulos o en blanco (400); credenciales inválidas (401); tratamiento actual de `client` omitido; y comportamiento actual ante un error 500. Mantener `GET /q/health` sin cambios. Caracterizar la representación del token por separado porque las fuentes discrepan.
3. Conservar y ampliar las pruebas de credenciales: una cuenta válida conocida sigue autenticándose; usuarios desconocidos y contraseñas incorrectas siguen dando 401; una entrada con sintaxis SQL no autentica ni altera la cuenta seleccionada. Afirmar que los valores de búsqueda se vinculan como parámetros y no se interpolan en el SQL. Hacerlo antes de cambiar el comportamiento del token.
4. Incorporar el requisito de `client` en un cambio separado: valores ausentes, vacíos o compuestos solo por espacios devuelven 400 y no generan token; un valor no vacío se refleja exactamente en el JWT. Actualizar la caracterización para documentar el cambio intencional de opcional a obligatorio.
5. Sustituir o adaptar la emisión de tokens para usar la biblioteca JOSE seleccionada. Verificar la firma, los claims obligatorios, los claims de compatibilidad conservados, `iat`/`exp`, la vigencia de 3600 segundos y la tolerancia de `expiresIn`. Verificar con el validador de pruebas que los tokens vencidos o modificados se rechazan; no exponer una nueva ruta HTTP.
6. Verificar que la configuración de firma ausente o inválida falla de forma segura y que los logs capturados de la aplicación no contienen credenciales, clave de firma ni token. Ejecutar las pruebas del contrato del endpoint y una prueba básica de salud; luego actualizar la documentación del endpoint para reflejar los cambios aprobados en el token y `client`.

## Riesgos y compensaciones

- [Faltan las fuentes canónicas y los artefactos del repositorio discrepan] -> Restaurar el árbol de fuentes y establecer una línea base respaldada por el código antes de implementar; nunca modificar los resultados generados bajo `target/`.
- [Hacer obligatorio `client` y cambiar la representación del token puede romper clientes] -> Coordinar el cliente Angular y Requirements API durante el despliegue; verificar antes las expectativas HS256, issuer y audience del consumidor.
- [HS256 comparte la autoridad de firma y verificación] -> Mantener la clave fuera del control de versiones, exigir entropía y longitud adecuadas, usar el mismo contrato de configuración protegida y no registrarla nunca. La firma asimétrica será un cambio aparte si la distribución de claves del consumidor se vuelve un requisito.
- [Una dependencia JOSE agrega mantenimiento y riesgo de cadena de suministro] -> Usar una biblioteca mantenida y compatible con Java 21, revisar sus dependencias transitivas y limitar su uso al manejo de tokens.
- [Los límites de tiempo pueden volver inestables las aserciones de `expiresIn`] -> Usar un reloj controlable y una tolerancia documentada de un segundo; evitar pausas artificiales en las pruebas.
- [Tras una reversión, los consumidores podrían no poder verificar los tokens emitidos] -> Revertir el emisor y el consumidor como una unidad coordinada y exigir una nueva autenticación; los tokens tienen una vigencia máxima de 3600 segundos.

## Plan de migración y reversión

No se planifica una migración de base de datos. Antes del despliegue, configurar el mismo secreto HS256 protegido para el emisor y su verificador, confirmar que los clientes envíen un `client` no vacío y desplegar la verificación compatible de tokens antes del cambio del emisor o junto con él. No habilitar una alternativa que acepte a la vez los formatos de token legacy y JWT.

Si es necesario revertir la versión, volver a desplegar el par anterior compatible de emisor y consumidor y exigir que los usuarios se autentiquen de nuevo. No intentar reinterpretar los JWT nuevos como tokens legacy; esperar como máximo la vigencia de 3600 segundos o invalidarlos en el consumidor. Como no hay cambios de esquema, la reversión no requiere migrar datos.

## Preguntas abiertas

No hay preguntas pendientes sobre el comportamiento aprobado. La disponibilidad del código fuente canónico es un requisito previo para implementar, no una razón para cambiar las specs; resolverla en este repositorio antes de empezar a modificar código.