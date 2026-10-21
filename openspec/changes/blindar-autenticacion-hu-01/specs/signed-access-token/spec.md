# Spec Delta

## Purpose

Define el contrato del token de autenticacion emitido por un login exitoso, su vigencia y el resguardo de los datos usados para firmarlo.

## ADDED Requirements

### Requirement: Successful login requires a valid client
The system SHALL require `client` to be a nonempty, nonblank string before authenticating a login and issuing a token.

#### Scenario: Client is missing or blank
- **WHEN** a client submits a login request with `client` missing, empty, or blank
- **THEN** the service responds with HTTP 400 and issues no token

### Requirement: Successful login preserves the documented response shape
The system SHALL return HTTP 200 with the documented `token`, `expiresIn`, `user`, and `debug` fields for a successful login.

#### Scenario: Valid login response
- **WHEN** a client submits valid credentials and a valid `client`
- **THEN** the JSON response contains a string `token`, integer `expiresIn`, `user.username`, `user.displayName`, `user.role`, and string `debug`

### Requirement: Issued tokens are signed and contain required claims
The system SHALL issue a verifiably signed token whose `sub` matches the authenticated username and whose `client`, `iat`, and `exp` claims represent the submitted client and the token's issuance and expiration times.

#### Scenario: Token claims describe the authenticated login
- **WHEN** a client submits valid credentials and a nonblank `client`
- **THEN** the returned token has a valid signature, `sub` equals the authenticated username, `client` equals the submitted value, and `iat` and `exp` identify its issuance and expiration

#### Scenario: Token content is modified
- **WHEN** a token's signed content is modified and the token is verified
- **THEN** verification rejects the token as invalid

### Requirement: Token expiration and response lifetime agree
The system SHALL set token expiration to 3600 seconds after issuance and SHALL report `expiresIn` within one second of `exp - now` at response creation.

#### Scenario: Successful login reports remaining token lifetime
- **WHEN** a client receives a token from a successful login
- **THEN** the token's `exp` is 3600 seconds after `iat` and `expiresIn` differs from `exp - now` by no more than one second

#### Scenario: Expired token is verified
- **WHEN** the system verifies an issued token whose `exp` is at or before the current time
- **THEN** the token is rejected as expired

### Requirement: Non-laboratory signing does not use an implicit secret
Outside the laboratory, the system SHALL NOT issue a successful login token signed with an implicit default secret.

#### Scenario: Non-laboratory runtime has no configured signing secret
- **WHEN** a login is attempted outside the laboratory without an explicitly configured `AUTH_TOKEN_SECRET`
- **THEN** the system does not return a successful login with a token signed by a default secret

### Requirement: Authentication logs exclude sensitive values
The system SHALL NOT write credentials, signing secrets, or issued tokens to application logs.

#### Scenario: Login activity is logged
- **WHEN** a login succeeds or fails
- **THEN** the resulting application logs contain neither the submitted credentials, the signing secret, nor the issued token