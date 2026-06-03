# ORDERLY Backend Status

Estado actual: funcional para desarrollo local y validación del flujo principal.

## Módulos listos

- autenticación JWT
- aislamiento multi-tenant por cabecera de tenant
- negocios
- productos
- pedidos
- mensajes personalizados
- eventos en tiempo real por WebSocket
- panel backend para superAdmin
- panel backend para admin de negocio
- panel backend para operaciones

## Verificación realizada

- pruebas automáticas con Maven
- smoke test real contra la API local
- flujo validado: health, login, crear negocio, crear producto, crear pedido y cambiar estado a READY

## Punto de entrada

- API local: http://localhost:8080
- WebSocket STOMP: /ws
- tópico de pedidos por tenant: /topic/business/{businessId}/orders
- superAdmin overview: /api/v1/admin/overview
- tenant admin dashboard: /api/v1/businesses/{businessId}/dashboard
- operator overview: /api/v1/operator/overview

## Credenciales demo

- admin@orderly.local
- Orderly123!

## Script de verificación

- scripts/smoke-test.ps1
