# Plan de pruebas - HU-02

## Referencia y objetivo
- **HU:** HU-02 - Limitar a 10 caracteres el identificador de login.
- **Objetivo:** verificar que el backend acepte identificadores de hasta 10 caracteres y rechace los que excedan el limite sin autenticar ni emitir token.

## Alcance y precondiciones
- Probar `POST /auth/login` directamente, sin depender de validacion en una interfaz cliente.
- Usar datos de prueba aislados. No incluir credenciales ni tokens reales en la evidencia.
- Para probar el login valido del caso limite, debe existir una cuenta de prueba con un identificador de exactamente 10 caracteres y credenciales validas.
- La HU no define el nombre del campo, el conteo de Unicode/espacios ni el formato de error. Confirmar estos puntos con el contrato vigente antes de fijar aserciones sobre ellos.

## Trazabilidad
| Criterio HU-02 | Casos |
|---|---|
| CA-1: 10 caracteres no se rechaza por longitud y se conserva la respuesta vigente con credenciales validas | TP-01 |
| CA-2: 11 caracteres se rechaza en backend y no se emite token | TP-02 |
| CA-3: la regla se aplica a solicitudes directas a la API | TP-01, TP-02 |
| CA-4: el identificador no se trunca ni altera silenciosamente | TP-02 |
| CA-5: existen pruebas automatizadas de los limites y evidencia de ejecucion | TP-01, TP-02, EV-01 |

## Casos de prueba
| ID | Tipo | Precondiciones y datos | Pasos | Resultado esperado |
|---|---|---|---|---|
| TP-01 | Funcional / frontera | Cuenta aislada cuyo identificador tiene exactamente 10 caracteres; credenciales validas. | Enviar una solicitud directa a `POST /auth/login` con ese identificador. | La regla de longitud no rechaza la solicitud; el login conserva la respuesta vigente. |
| TP-02 | Funcional / frontera / seguridad | Solicitud directa con identificador de exactamente 11 caracteres. | Enviar la solicitud a `POST /auth/login` y revisar la respuesta. | El backend rechaza la solicitud; no autentica ni emite token. El valor no se trunca ni se cambia silenciosamente. |
| TP-03 | Unitario | Validador del identificador disponible en la implementacion. | Ejecutar pruebas automatizadas con valores de 10 y 11 caracteres. | El valor de 10 pasa la regla de longitud y el de 11 no; el resultado queda registrado por la suite. |

## Regresion y seguridad
- Confirmar en TP-01 que las credenciales validas y la respuesta de login mantienen el comportamiento vigente.
- Confirmar en TP-02 que una llamada directa no evita la regla y que no se genera token.
- No registrar el token ni secretos durante la prueba. Verificar codigo HTTP y cuerpo de error cuando se confirme el contrato.

## Pendientes y bloqueos
- Confirmar el nombre del campo de identificador en el payload; afecta TP-01, TP-02 y TP-03.
- Definir si el limite cuenta puntos de codigo Unicode o unidades UTF-16 y el tratamiento de espacios; afecta TP-03 y casos adicionales con entradas Unicode/espacios.
- Confirmar el codigo HTTP y cuerpo de error para exceso de longitud; bloquea la asercion exacta de respuesta en TP-02, pero no la verificacion de rechazo y ausencia de token.

## Evidencia de ejecucion
- **EV-01:** salida de la suite automatizada con TP-03.
- **EV-02:** resumen de solicitudes/respuestas de TP-01 y TP-02, sin credenciales ni tokens.
- Este documento define pruebas planificadas; no afirma que se hayan ejecutado.