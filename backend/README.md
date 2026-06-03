# ORDERLY Backend Base

Backend inicial y funcional para ORDERLY siguiendo:

- arquitectura por capas con enfoque ports and adapters
- principios SOLID
- separación entre dominio, aplicación, infraestructura y API
- validación de entrada y manejo uniforme de errores
- autenticación JWT, multi-tenancy, productos, pedidos y WebSocket

## Estructura

- src/main/java/com/orderly/api/business
- src/main/java/com/orderly/api/product
- src/main/java/com/orderly/api/order
- src/main/java/com/orderly/api/messaging
- src/main/java/com/orderly/api/shared

## Ejecución local

Modo local por defecto:

- H2 embebido
- repositorios de desarrollo en memoria para el flujo principal
- no requiere PostgreSQL para arrancar

Comando:

mvn spring-boot:run

## Perfil PostgreSQL

Cuando quieras validar persistencia externa:

- usar el perfil postgres
- configurar DATABASE_URL, DATABASE_USERNAME y DATABASE_PASSWORD
- el archivo de perfil está en src/main/resources/application-postgres.yml

## Credenciales demo

- correo: admin@orderly.local
- clave: Orderly123!

## Verificación

Pruebas automáticas:

mvn test

Smoke test local:

powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
