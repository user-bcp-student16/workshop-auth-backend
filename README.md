# Workshop Auth Backend — Java Quarkus

> **Laboratorio intencionalmente vulnerable.** Ejecutar únicamente en local, repositorios privados y ambientes aislados. No desplegar en Internet, CERT o PROD.

Backend Quarkus que autentica usuarios de laboratorio almacenados en SQLite y entrega un token consumido por el frontend Angular y la API Python.

## Ejecución

Requisitos: JDK 21 y Maven 3.9+.

```powershell
$env:AUTH_TOKEN_SECRET = "<shared-local-secret>"
mvn quarkus:dev
```

En Bash, usa `export AUTH_TOKEN_SECRET="<shared-local-secret>"` antes de iniciar el servicio.

`AUTH_TOKEN_SECRET` debe configurarse con el mismo valor en Requirements API y contener al menos 32 bytes UTF-8. Usa un secreto local aleatorio y no lo guardes en el repositorio.

Servicio: `http://localhost:8080`

Credenciales locales:

- `student` / `Workshop123!`
- `trainer` / `Trainer123!`

## Endpoints

- `POST /auth/login`
- `GET /q/health`

## Propósito de la rama `main`

La rama conserva deuda técnica y de seguridad deliberada, como contraseñas en texto plano, CORS permisivo, métodos con varias responsabilidades, manejo genérico de excepciones y dependencias con CVEs.
