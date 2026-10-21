# Spec Delta

## Purpose

Define el comportamiento observable del login al validar credenciales, incluyendo la resistencia a entradas que intenten alterar la autenticacion.

## ADDED Requirements

### Requirement: Login validates required credentials
The system SHALL keep `POST /auth/login` available without prior authentication and SHALL reject a null request or a missing, empty, or blank username or password with HTTP 400.

#### Scenario: Request omits a required credential
- **WHEN** a client submits a null request or omits or blanks `username` or `password`
- **THEN** the service responds with HTTP 400 and does not authenticate the request

### Requirement: Credential input cannot bypass authentication
The system SHALL treat submitted credential values as data and SHALL NOT let SQL-like input alter account matching or bypass password validation.

#### Scenario: SQL-like credentials do not authenticate without a matching account and password
- **WHEN** a client submits SQL-like text as credentials that do not match a stored username and its password
- **THEN** the service responds with HTTP 401 and does not return an authenticated user or token

### Requirement: Invalid credentials remain unauthorized
The system SHALL respond with HTTP 401 when the username is unknown or the password does not match.

#### Scenario: Unknown username or mismatched password
- **WHEN** a client submits a nonblank username and password that are not valid credentials
- **THEN** the service responds with HTTP 401