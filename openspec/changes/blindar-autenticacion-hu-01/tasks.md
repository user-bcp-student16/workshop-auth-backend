# Tareas

## 1. Recuperación y caracterización previa

- [ ] 1.1 Restablecer o poner a disposición las fuentes Java canónicas dentro de este repositorio y ejecutar la suite existente antes de cambiar comportamiento; verificar que `git ls-files src` encuentre fuentes y que `mvn test` las compile y ejecute, sin modificar archivos bajo `target/`.
- [ ] 1.2 Agregar pruebas de caracterización del contrato actual de `POST /auth/login`: respuesta y campos de usuario para login válido, request/credenciales inválidas y sus estados 400/401, respuesta 500 y tratamiento actual de `client` omitido; verificar que pasen contra la implementación sin cambios funcionales.
- [ ] 1.3 Caracterizar el formato y los claims de token actuales desde las fuentes canónicas y contrastarlos con `docs/ENDPOINTS.md`; verificar mediante una prueba reproducible qué formato y claims emite el baseline antes de sustituirlos.

## 2. Protección de credenciales

- [ ] 2.1 Asegurar que la búsqueda de credenciales vincule username y password como parámetros y no interpole valores en SQL; probar login válido, usuario/contraseña inválidos y entradas con sintaxis SQL, verificando que ninguna entrada inválida autentique ni cambie la cuenta consultada.

## 3. Validación del cliente

- [ ] 3.1 Exigir `client` no nulo, no vacío y no compuesto solo por espacios antes de autenticar o emitir token; verificar que esos casos devuelvan 400 y que un valor no vacío válido llegue sin cambios al claim.
- [ ] 3.2 Actualizar `docs/ENDPOINTS.md` para declarar `client` obligatorio y sus errores 400; verificar que el contrato documentado coincida con las pruebas del endpoint.

## 4. Token firmado y manejo de secretos

- [ ] 4.1 Agregar Nimbus JOSE + JWT tras confirmar compatibilidad con Java 21, Quarkus y la política de dependencias del proyecto; verificar resolución de dependencias y compilación.
- [ ] 4.2 Emitir JWT HS256 con `sub`, `client`, `iat` y `exp`, conservando `iss`, `aud` y `role` caracterizados en la tarea 1.3; verificar firma, valores de claims, expiración a 3600 segundos, tolerancia de un segundo para `expiresIn`, y rechazo de tokens modificados o vencidos mediante pruebas deterministas.
- [ ] 4.3 Exigir configuración explícita de `AUTH_TOKEN_SECRET` fuera del laboratorio y rechazar una clave ausente o inválida; verificar que el login no emita token con un secreto predeterminado.
- [ ] 4.4 Evitar que credenciales, secreto o token aparezcan en los logs de login exitoso o fallido; verificarlo con captura de logs en pruebas.
- [ ] 4.5 Actualizar `docs/ENDPOINTS.md` con el token firmado, claims, vigencia y coherencia de `expiresIn`, preservando la ruta y forma de respuesta aprobadas; verificar cada campo documentado contra una respuesta de prueba.

## 5. Verificación integrada

- [ ] 5.1 Ejecutar la suite completa y las pruebas de contrato de `POST /auth/login`; verificar que pasen los casos de login válido, credenciales inválidas, SQL-like, `client` ausente/en blanco, claims, expiración, secreto y logs.
- [ ] 5.2 Confirmar que `GET /q/health` conserva su comportamiento y que no se agregaron rutas ni cambios al esquema SQLite; verificar con prueba de humo y revisión del diff limitado a este cambio.

## Criterios de salida medibles

El cambio se considera terminado cuando se cumplen todos estos puntos:

- `mvn test` finaliza con código 0 y reporta 0 fallos y 0 errores.
- Las pruebas de `POST /auth/login` verifican respuestas 200 para credenciales y `client` válidos; 400 para request, credenciales requeridas o `client` ausentes/en blanco; y 401 para credenciales inválidas y entradas SQL-like que no correspondan a una cuenta válida.
- El login válido devuelve todos los campos documentados; la firma del token se verifica y sus claims `sub`, `client`, `iat` y `exp` coinciden con el usuario, la solicitud y los tiempos de emisión.
- `exp - iat` es exactamente 3600 segundos; `expiresIn` difiere de `exp - now` como máximo en 1 segundo; tokens vencidos o modificados se rechazan en la verificación.
- Fuera del laboratorio no se emite un token si falta una configuración explícita válida; las pruebas de logs no encuentran ninguno de los valores centinela de credenciales, secreto o token.
- `GET /q/health` conserva su respuesta de salud; no aparecen endpoints nuevos ni cambios al esquema SQLite, y `docs/ENDPOINTS.md` coincide con el comportamiento probado.
- `openspec validate "blindar-autenticacion-hu-01" --type change --json` informa `valid: true` y cero hallazgos.