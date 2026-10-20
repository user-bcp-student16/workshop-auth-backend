---
name: plan-pruebas-hu
description: 'Genera un plan de pruebas trazable desde una historia de usuario Markdown. Usar cuando se solicite planificar pruebas para una HU, especialmente HU-02 y el limite del identificador de login.'
argument-hint: 'Ruta de la HU, por ejemplo HU-02.md'
---

# Plan de pruebas desde una HU

## Objetivo
Convertir una historia de usuario en un plan de pruebas verificable, trazable a sus criterios de aceptacion y compatible con las decisiones y restricciones documentadas.

## Entrada
- Recibe la ruta de un archivo Markdown de historia de usuario como argumento.
- Si no se proporciona una ruta y el contexto de la solicitud menciona HU-02, usa `HU-02.md` en la raiz del workspace.
- Lee la HU completa antes de redactar el plan. Consulta solo los contratos o documentos locales que la HU cite y que esten disponibles.
- No uses credenciales, tokens ni datos reales en ejemplos o evidencia.

## Procedimiento
1. Identifica el ID y titulo de la HU, alcance, supuestos, decisiones pendientes, riesgos y criterios de aceptacion.
2. Convierte cada criterio de aceptacion en uno o mas casos con identificadores unicos y trazabilidad explicita al criterio.
3. Incluye pruebas unitarias y funcionales cuando correspondan; agrega comprobaciones de seguridad y regresion solo si aplican al comportamiento de la HU.
4. Para cada caso especifica tipo, precondiciones, datos de prueba no sensibles, pasos y resultado esperado. Incluye limites y valores inmediatamente dentro/fuera del limite cuando la HU defina una frontera.
5. No inventes nombres de campos, codigos HTTP, mensajes, reglas de normalizacion, herramientas ni ubicaciones de codigo. Si dependen de informacion ausente, marcarlos como pendientes y expresar el resultado esperado en terminos observables que no requieran esa decision.
6. Separa los bloqueos de ejecucion de las pruebas que ya pueden ejecutarse. Cada pregunta abierta debe indicar que casos afecta.
7. Guarda el documento como `Plan-Pruebas.md` en la raiz del workspace, preservando cualquier archivo existente. Si la ruta ya existe, revisa su contenido y actualizalo sin descartar material ajeno a esta HU.
8. Comprueba que todos los criterios tengan cobertura, que los casos tengan resultados observables y que ninguna decision pendiente aparezca como requisito confirmado.

## Formato de salida
Genera un documento Markdown breve con estas secciones:

1. **Referencia y objetivo**: ID, titulo y resultado bajo prueba.
2. **Alcance y precondiciones**: incluido, excluido y dependencias necesarias.
3. **Trazabilidad**: mapa de criterios de aceptacion a IDs de casos.
4. **Casos de prueba**: tabla con ID, tipo, precondiciones/datos, pasos y resultado esperado.
5. **Regresion y seguridad**: comprobaciones pertinentes para la HU.
6. **Pendientes y bloqueos**: decision faltante, casos afectados y quien debe resolverla si se puede inferir de la HU.
7. **Evidencia de ejecucion**: artefactos a adjuntar; no afirmar que las pruebas se ejecutaron si solo se planificaron.

## Reglas especificas para HU-02
- La regla bajo prueba es el maximo inclusivo de 10 caracteres en el identificador del login, no en la contrasena.
- Cubrir al menos 10 caracteres (limite permitido) y 11 caracteres (debe rechazarse); verificar que el rechazo ocurre en backend y no emite token.
- Cubrir el envio directo a `POST /auth/login`, sin depender de una validacion de interfaz.
- No truncar ni alterar silenciosamente el identificador.
- Dejar como pendientes el nombre del campo, el criterio de conteo (Unicode/UTF-16 y espacios) y el codigo/cuerpo de error hasta que se confirmen contra el contrato vigente.
- Mantener las credenciales de prueba ficticias o aisladas y no registrar tokens.