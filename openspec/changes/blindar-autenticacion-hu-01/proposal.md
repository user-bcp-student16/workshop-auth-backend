# Proposal

## Why

El Issue [#13](https://github.com/user-bcp-student16/workshop-auth-backend/issues/13) compromete blindar el login contra inyeccion SQL y pide generar el trabajo desde HU-01. La HU requiere que los tokens sean firmados, tengan claims minimos verificables y no expongan secretos en logs; esto importa para evitar que una entrada de login altere la autenticacion o que un token manipulado o vencido se considere valido.

## What Changes

- Tratar `username` y `password` como datos literales durante la autenticacion: una entrada con sintaxis de inyeccion no debe alterar la busqueda ni permitir autenticacion sin credenciales validas.
- Emitir tokens firmados para logins validos con los claims `sub`, `client`, `iat` y `exp`. `expiresIn` debe corresponder a `exp - now` con una tolerancia de un segundo; se conserva la duracion actual de 3600 segundos.
- Exigir `client` para un login exitoso y rechazar valores ausentes, vacios o en blanco. Esto cambia el contrato del Dia 1, donde `client` era opcional y no intervenia en la autenticacion; el cambio se justifica porque HU-01 requiere validar el dato y reflejarlo como claim obligatorio.
- No usar un secreto implicito fuera del laboratorio y no registrar tokens, secretos ni valores sensibles.
- **BREAKING**: el significado de `token` cambia de Base64 de campos concatenados a un token firmado, y `client` pasa de opcional a obligatorio. Se mantienen el tipo string de `token` y la estructura del resto de la respuesta exitosa.
- Se conserva `POST /auth/login`, sin autenticacion previa, los campos `expiresIn`, `user` y `debug`, y los codigos documentados para request invalido (400), credenciales invalidas (401) y excepcion no controlada (500). `GET /q/health` y su comportamiento quedan fuera del cambio.
- El Issue no resuelve si la firma debe usar HMAC directamente o una biblioteca JWT; esa decision queda fuera de esta propuesta de comportamiento.

## Capabilities

### New Capabilities

- `credential-authentication`: el login autentica credenciales sin que entradas con sintaxis SQL alteren la busqueda o evadan la validacion.
- `signed-access-token`: el login emite tokens firmados con los claims, expiracion y resguardo de secretos requeridos por HU-01.

### Modified Capabilities

No hay capacidades existentes en `openspec/specs/`; las capacidades anteriores se introducen como nuevas.

## Impact

- Contrato observable de `POST /auth/login` descrito en `docs/ENDPOINTS.md`: cambia la semantica de `token` y `client` pasa a ser obligatorio; permanecen ruta, campos de respuesta, datos del usuario y codigos de error documentados.
- Comportamiento de autenticacion de credenciales, emision y vigencia de token, configuracion de `AUTH_TOKEN_SECRET` y registro de datos sensibles.
- No se agregan endpoints ni se altera `GET /q/health`. La seleccion entre HMAC y una biblioteca JWT se reserva para una decision posterior.

## Fuentes y supuestos

- Alcance comprometido: Issue #13 y su instruccion de generar el trabajo desde [HU-01](../../../HU-01.md).
- Baseline preservado: [docs/ENDPOINTS.md](../../../docs/ENDPOINTS.md), contrato del Dia 1.
- HU-01 no define formato ni longitud para `client`; para estos escenarios, se considera valido cualquier string no vacio ni compuesto solo por espacios. Los valores ausentes o en blanco se rechazan como request invalido (400).
- HU-01 no fija una tolerancia numerica para `expiresIn`; se asume un segundo por expresarse en segundos enteros.
- HU-01 solicita pruebas para tokens expirados, pero el baseline no documenta un endpoint que consuma tokens. La spec limita el requisito a que un token con `exp` vencido sea invalido al verificarse, sin agregar una ruta de verificacion.