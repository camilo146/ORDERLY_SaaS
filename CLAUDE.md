# CLAUDE.md — Contexto del Proyecto ORDERLY

> Este archivo es memoria persistente para sesiones de Claude Code.
> Contiene únicamente información real detectada en el proyecto.
> Última actualización: 2026-05-10

---

## Resumen del Proyecto

**Orderly** es una plataforma SaaS B2B que permite a negocios (restaurantes, farmacias, tiendas) gestionar pedidos por WhatsApp mediante un chatbot conversacional automatizado.

**Flujo central:**
1. Cliente escribe al WhatsApp del negocio
2. Bot (Evolution API) guía al cliente por un menú → carrito → pago → confirmación
3. El pedido aparece en tiempo real en el dashboard Kanban del operador
4. El operador confirma, despacha y entrega — el bot notifica al cliente en cada paso

**Roles de usuario:**
- `SUPER_ADMIN` — Acceso total a admin panel de la plataforma
- `ADMIN` — Dueño del negocio (dashboard, productos, settings, complaints)
- `OPERATOR` — Panel simplificado de atención

---

## Stack Tecnológico

### Frontend
| Tecnología | Versión | Uso |
|-----------|---------|-----|
| Angular | 17.3.0 | Framework SPA |
| TypeScript | ~5.4.2 | Lenguaje |
| lucide-angular | ^1.0.0 | Iconos SVG |
| RxJS | ~7.8.0 | Streams reactivos |
| Zone.js | ~0.14.3 | Change detection |
| Angular CLI | 17.3.17 | Build/serve |

**Dev server:** `ng serve --proxy-config proxy.conf.json --host 0.0.0.0` (puerto 4200)  
**Proxy:** `/api` redirigido a `http://localhost:8080`

### Backend
| Tecnología | Versión | Uso |
|-----------|---------|-----|
| Spring Boot | 3.3.5 | Framework |
| Java | 21 | Lenguaje |
| Spring Security | (boot) | Auth JWT |
| Spring Data JPA | (boot) | ORM |
| Spring Data Redis | (boot) | Sesiones chatbot |
| Spring WebSocket | (boot) | Notificaciones real-time |
| JJWT | 0.12.6 | Generación/validación JWT |
| PostgreSQL Driver | (boot) | Base de datos |
| Flyway | (boot) | Migraciones SQL |
| Jakarta Validation | (boot) | Bean validation |
| Spring Actuator | (boot) | Health checks |

### Infraestructura (docker-compose.yml)
| Servicio | Imagen | Puerto |
|---------|--------|--------|
| PostgreSQL | postgres:16-alpine | 5433:5432 |
| Redis | redis:7-alpine | 6379:6379 |
| MongoDB | mongo:6-jammy | 27017 (legacy) |
| Evolution API | evoapicloud/evolution-api:latest | 8085:8080 |

**Base de datos:** `orderly` / Usuario: `orderly_user` / Pass: `orderly_pass`  
**Redis password:** `orderly_redis_dev_pass`  
**Evolution API key:** `orderly-dev-secret-key`  
**Backend URL:** `http://localhost:8080`

---

## Arquitectura

### Backend — Arquitectura Hexagonal (Ports & Adapters)

Cada módulo sigue esta estructura de capas:

```
com.orderly.api.{módulo}/
├── domain/
│   ├── model/          ← Entidades inmutables (final class, private constructor)
│   ├── port/           ← Interfaces (repositorios, servicios externos)
│   └── event/          ← Eventos de dominio
├── application/
│   ├── {UseCase}UseCase.java       ← Interface del caso de uso
│   ├── Default{UseCase}Service.java ← Implementación Spring @Service
│   └── {Action}Command.java        ← Comandos (records)
├── infrastructure/
│   ├── persistence/    ← JPA entities + adapters (@Repository @Primary)
│   ├── repository/     ← Fallback in-memory (ConcurrentHashMap)
│   └── storage/        ← File storage, external APIs
└── interfaces/
    └── rest/           ← @RestController + Request/Response records
```

**Módulos backend (12):**
- `analytics` — Alertas de churn, métricas
- `business` — Negocio principal, slug, onboarding
- `complaint` — Quejas/reclamos vía WhatsApp
- `conversation` — Motor del chatbot (máquina de estados Redis)
- `customer` — Clientes del negocio
- `dashboard` — Vistas por rol (business, admin, operator)
- `messaging` — Evolution API, templates WhatsApp, settings de canal
- `notification` — Cola de notificaciones
- `order` — Pedidos, líneas de pedido, ciclo de vida
- `plan` — Límites de plan (órdenes activas, productos)
- `product` — Catálogo, imágenes, stock
- `shared` — Security, JWT, WebSocket, tenant context, servicios transversales

### Frontend — Angular 17 Standalone

```
src/app/
├── core/
│   ├── guards/         ← auth.guard.ts, role.guard.ts
│   ├── interceptors/   ← auth.interceptor.ts (inyecta JWT)
│   ├── models/         ← orderly.models.ts (todas las interfaces TS)
│   └── services/       ← auth, business, order, product, whatsapp, template
├── features/
│   ├── auth/           ← login, register
│   ├── complaints/     ← listado de quejas
│   ├── dashboard/      ← Kanban pedidos + order-detail
│   ├── onboarding/     ← setup inicial
│   ├── operator/       ← panel operador simplificado
│   ├── products/       ← catálogo de productos
│   ├── public/         ← NUEVO: sitio web marketing (ver sección más abajo)
│   │   ├── layout/     ← PublicLayoutComponent (navbar + footer wrapper)
│   │   ├── navbar/     ← NavbarComponent (sticky, blur en scroll, mobile)
│   │   ├── footer/     ← FooterComponent (4 columnas)
│   │   ├── home/       ← HomeComponent (landing page completa)
│   │   ├── features-page/ ← FeaturesPageComponent
│   │   ├── pricing/    ← PricingComponent (toggle mensual/anual)
│   │   ├── faq/        ← FaqComponent (accordion)
│   │   └── contact/    ← ContactComponent (formulario + canales)
│   ├── settings/       ← tabs: WhatsApp, mensajes, pagos, horarios, bot
│   └── super-admin/    ← panel administrador plataforma
└── shared/
    └── layout/         ← sidebar + topbar layout
```

---

## Frontend

### Sistema de Estilos

**Tema:** Dark mode únicamente. Naranja como color primario.

**Variables CSS principales** (definidas en `src/styles.css`):
```css
/* Colores */
--color-primary: #FF6B35          /* Naranja principal */
--color-primary-dark: #e05520
--color-primary-soft: rgba(255,107,53,0.1)
--color-bg: #09090b               /* Fondo oscuro */
--color-surface: #111113          /* Cards */
--color-surface-2: #18181b        /* Inputs, items */
--color-surface-3: #27272a        /* Chips, tags */
--color-border: #27272a
--color-border-subtle: #1f1f22
--color-text: #fafafa
--color-text-secondary: #a1a1aa
--color-text-muted: #71717a
--color-danger: #ef4444
--color-warning: #f59e0b
--color-success: #22c55e

/* Tipografía */
--font-heading: 'Inter', system-ui, sans-serif
--font-body: 'Inter', system-ui, sans-serif
--font-mono: 'SF Mono', 'Fira Code', ui-monospace, monospace

/* Espaciado (base 4px) */
--space-page: 28px  /* padding horizontal de páginas */
--radius: 8px
--radius-sm: 6px
--radius-lg: 12px
--radius-full: 9999px

/* Sombras */
--shadow-lg: 0 8px 32px rgba(0,0,0,0.55), 0 2px 8px rgba(0,0,0,0.4)

/* Transiciones */
--transition-fast: 120ms ease
--transition-base: 200ms ease
```

**Clases de botón globales** (definidas en `styles.css`, usadas en toda la app):
- `.btn` — base
- `.btn-primary` — fondo naranja
- `.btn-secondary` — fondo surface-2
- `.btn-danger` — fondo danger-soft
- `.btn-ghost` — sin fondo
- `.btn-outline` — borde visible
- `.btn-sm`, `.btn-lg` — tamaños
- `.card` — contenedor surface con borde y radius

### Manejo de Estado

- **Signals API** de Angular 17 (`signal()`, `computed()`, `signal.update()`)
- **No hay NgRx ni estado global** — cada componente maneja su propio estado local con signals
- **Polling cada 5 segundos** en el dashboard para actualizar pedidos (`setInterval`)
- **WebSocket** disponible en backend pero el frontend usa polling actualmente

### Routing

```typescript
// app.routes.ts — estructura actualizada (2026-05-10)
// 1. Sitio público (marketing) — no requiere auth
{
  path: '',
  component: PublicLayoutComponent,       // navbar + router-outlet + footer
  children: [
    { path: '', component: HomeComponent, pathMatch: 'full' },   // landing
    { path: 'features', component: FeaturesPageComponent },
    { path: 'pricing', component: PricingComponent },
    { path: 'faq', component: FaqComponent },
    { path: 'contact', component: ContactComponent },
  ]
}
// 2. Auth — standalone sin layout
{ path: 'login', component: LoginComponent }
{ path: 'register', component: RegisterComponent }
// 3. App — layout protegido con authGuard
{
  path: '',
  component: LayoutComponent,
  canActivate: [authGuard],
  children: [
    { path: 'dashboard', ... },
    { path: 'dashboard/orders/:id', ... },
    { path: 'products', ... },
    { path: 'settings', ... },
    { path: 'operator', ... },
    { path: 'complaints', ... },
    { path: 'admin', ... },
    { path: 'onboarding', ... },
  ]
}
{ path: '**', redirectTo: '' }   // wildcard → home (antes era → login)
```

**Comportamiento de routing:**
- `/` → Home (landing page pública)
- `/features`, `/pricing`, `/faq`, `/contact` → páginas marketing con navbar/footer
- `/login`, `/register` → auth sin layout
- `/dashboard`, `/products`, etc. → app protegida con sidebar
- Angular usa backtracking: si ningún hijo del primer bloque `path:''` coincide, pasa al segundo bloque

### Autenticación Frontend

- `AuthService` almacena el JWT en `localStorage`
- `auth.interceptor.ts` inyecta `Authorization: Bearer {token}` en cada request
- `auth.guard.ts` redirige a `/login` si no hay token
- `role.guard.ts` redirige al dashboard si el rol no es suficiente
- `AuthService.getActiveBusinessId()` retorna el business activo del usuario

### Iconos

- Librería: `lucide-angular` v1.0.0
- **Todos los iconos deben registrarse explícitamente** en `app.config.ts`:
```typescript
// Importar Y registrar en LucideIconProvider:
import { Package, Plus, X, ... } from 'lucide-angular';
useValue: new LucideIconProvider({ Package, Plus, X, ... })
```
- ⚠️ Si un ícono no está registrado en `app.config.ts`, no renderiza (sin error visible)

### Componentes Clave

| Componente | Path | Descripción |
|-----------|------|-------------|
| `LayoutComponent` | `shared/layout/` | Sidebar + contenido. Logo: `<img src="assets/logo.png">` |
| `DashboardComponent` | `features/dashboard/` | Kanban 4 columnas: PENDING, CONFIRMED, OUT_FOR_DELIVERY, DELIVERED |
| `OrderDetailComponent` | `features/dashboard/order-detail/` | Detalle de pedido individual |
| `ProductsComponent` | `features/products/` | Grid de productos con modal de edición |
| `SettingsComponent` | `features/settings/` | 5 tabs: whatsapp, messages, business, hours, bot |
| `LoginComponent` | `features/auth/login/` | Logo imagen (no lucide-icon) |

### Sitio Web Público (Marketing)

Creado 2026-05-10. Toda la carpeta `features/public/`.

**Estructura:**
```
features/public/
├── layout/          PublicLayoutComponent — wrapper: NavbarComponent + router-outlet + FooterComponent
├── navbar/          NavbarComponent — sticky, blur con scroll, hamburger mobile, RouterLink/RouterLinkActive
├── footer/          FooterComponent — 3 columnas links + copyright
├── home/            HomeComponent — landing completa (~10 secciones)
├── features-page/   FeaturesPageComponent — features por categoría
├── pricing/         PricingComponent — toggle mensual/anual + 3 planes
├── faq/             FaqComponent — accordion (12 preguntas)
└── contact/         ContactComponent — formulario + canales de soporte
```

**Secciones del Home (orden):**
1. Hero — badge + h1 + CTA + mockup Kanban CSS
2. Trust bar — negocios + tipos
3. How it works — 3 pasos
4. Features grid — 6 cards 3×2
5. Bot section — contenido + mockup WhatsApp CSS (teléfono con chat)
6. Metrics — 4 números grandes
7. Pricing preview — 3 planes con toggle
8. Testimonials — 3 cards
9. Final CTA

**Íconos nuevos registrados en `app.config.ts`:**
`Menu, ChevronDown, ArrowUpRight, Mail, Phone, MapPin, Globe, CreditCard, Play`

**Notas de diseño:**
- CSS-based mockups: el Kanban y el chat de WhatsApp están construidos con HTML/CSS puro
- Colores: naranja solo en CTAs, iconos activos y accents — nunca saturar la UI
- Todos los componentes usan `var(--color-*)` del design system de `styles.css`
- `btn-outline`, `btn-ghost`, `btn-primary`, `btn-secondary` — todos de `styles.css`

---

## Backend

### Autenticación

- **JWT** con `JJWT 0.12.6`
- Token enviado en header `Authorization: Bearer {token}`
- Validez configurada en `orderly.security.token-validity-minutes` (default: 240 min)
- `JwtAuthenticationFilter` intercepta cada request
- `TenantContextFilter` extrae el `businessId` activo del usuario

**Endpoints públicos** (no requieren JWT):
```
POST /api/v1/auth/login
POST /api/v1/auth/register
GET  /api/v1/health/**
GET  /actuator/health
/ws/**
/webhook/**
/uploads/**
```

**CORS permitido:** `http://localhost:4200`, `http://10.*.*.*:4200`

### APIs REST principales

```
# Auth
POST /api/v1/auth/login
POST /api/v1/auth/register

# Business
GET/POST /api/v1/businesses
GET /api/v1/businesses/{id}/dashboard

# Products
GET    /api/v1/businesses/{id}/products
POST   /api/v1/businesses/{id}/products
PUT    /api/v1/businesses/{id}/products/{productId}
DELETE /api/v1/businesses/{id}/products/{productId}
POST   /api/v1/businesses/{id}/products/{productId}/image

# Orders
GET    /api/v1/businesses/{id}/orders
POST   /api/v1/businesses/{id}/orders
PATCH  /api/v1/businesses/{id}/orders/{orderId}/status
POST   /api/v1/businesses/{id}/orders/{orderId}/cancel
PATCH  /api/v1/businesses/{id}/orders/{orderId}/payment-proof

# Settings
GET/PUT /api/v1/businesses/{id}/settings/whatsapp-channel
GET/PUT /api/v1/businesses/{id}/settings/hours
GET/POST /api/v1/businesses/{id}/settings/service-status
POST    /api/v1/businesses/{id}/settings/service-status/toggle

# Templates
GET/PUT /api/v1/businesses/{id}/templates

# WhatsApp QR (Evolution API)
POST /api/v1/businesses/{id}/settings/whatsapp-channel/qr
GET  /api/v1/businesses/{id}/settings/whatsapp-channel/status

# Webhook (Evolution API → backend)
POST /webhook/{businessSlug}
```

### Chatbot — Máquina de Estados

**Ubicación:** `conversation/application/ConversationService.java`  
**Persistencia:** Redis con TTL 30 minutos (`ConversationSessionStore`)  
**Dispatcher:** `WhatsAppMessageDispatcher` (interface implementada por `ConversationService`)

**Estados (`ConversationState` enum):**
```
IDLE → GREETING → BROWSING_CATEGORIES → BROWSING_PRODUCTS
→ ADDING_ITEM → ADDRESS_REQUEST → PAYMENT_REQUEST
→ [PAYMENT_PROOF_UPLOAD] → ORDER_CONFIRMATION → ORDER_CREATED
COMPLAINT_DESCRIBE → COMPLAINT_PHOTO → (queja guardada)
HUMAN_HANDOFF (bot desactivado, humano responde)
```

**Lógica previa al state machine (en `dispatch()`):**
1. Verificar servicio habilitado (`ServiceStatusService`) — si off, responde fuera de servicio
2. Detectar foto de entrega: si cliente en OUT_FOR_DELIVERY envía imagen → auto-DELIVERED
3. Entrar al state machine normal

**Comando global:** `#bot` reactiva el bot desde HUMAN_HANDOFF  
**Reset global:** `0`, `cancelar`, `salir` → vuelve a GREETING

**Convención de medios:** imágenes llegan con prefijo `[MEDIA:url]` inyectado por el webhook controller

### Validaciones

- **Jakarta Validation** en request records (`@NotBlank`, `@NotNull`, `@DecimalMin`)
- `ApiExceptionHandler` maneja `DomainException` (400) y otros errores (500)
- `TenantAccessService.validateTenantAccess(businessId)` en cada endpoint protegido

### Patrón de Dominio

```java
// Las entidades de dominio son inmutables (final class, private constructor)
public final class Order {
    private final UUID id;
    // ...
    
    // Factory methods estáticos
    public static Order create(...) { return new Order(...); }
    public static Order reconstitute(...) { return new Order(...); }
    
    // Mutaciones retornan nueva instancia
    public Order updateStatus(OrderStatus newStatus) { return new Order(...); }
    public Order withCancelReason(String reason) { return new Order(...); }
}
```

### Servicios en Memoria (transversales)

- **`ServiceStatusService`** — `ConcurrentHashMap<UUID, Boolean>` por businessId (on/off servicio)
- **`BusinessHoursService`** — `ConcurrentHashMap<UUID, List<DaySchedule>>` por businessId
- ⚠️ Estos datos se pierden al reiniciar el backend (no persistidos en BD)

### Notificaciones WhatsApp

```java
// Patrón para enviar mensajes:
String instance = "orderly-" + business.slug();
String jid = customerPhone + "@s.whatsapp.net";
evolutionApi.sendTextMessage(instance, jid, message);
evolutionApi.sendImageMessage(instance, jid, imageUrl, caption);
```

---

## Base de Datos

**Motor:** PostgreSQL 16  
**Puerto:** 5433 (mapeado desde 5432 interno)  
**ORM:** Hibernate / Spring Data JPA  
**Migraciones:** Flyway (actualmente `enabled: false` en dev — Hibernate usa `ddl-auto: update`)

### Tablas principales

| Tabla | JPA Entity | Descripción |
|-------|-----------|-------------|
| `businesses` | `BusinessJpaEntity` | Negocios/tenants |
| `orders` | `OrderJpaEntity` | Pedidos |
| `order_items` | `OrderItemJpaEntity` | Líneas de pedido |
| `products` | `ProductJpaEntity` | Catálogo |
| `customers` | `CustomerJpaEntity` | Clientes WhatsApp |
| `whatsapp_channels` | (settings controller) | Config canal WA |
| `plans` | `PlanJpaEntity` | Planes de suscripción |
| `message_templates` | (template service) | Plantillas de mensajes |
| `complaints` | `ComplaintJpaEntity` | Quejas de clientes |
| `churn_alerts` | `ChurnAlertJpaEntity` | Alertas de abandono |
| `notification_queue` | `NotificationJpaEntity` | Cola de notificaciones |
| `app_users` | `UserJpaEntity` | Usuarios de la plataforma |

### Migraciones SQL

Ubicación: `backend/src/main/resources/db/migration/`

| Versión | Descripción |
|---------|-------------|
| V1 | Esquema base completo |
| V2 | Evolution API columns, estados conversación, app_users, churn_alerts |
| V3 | Bot identity (bot_name, bot_emoji) |
| V4 | Payment info columns + tabla complaints |
| V5 | cancel_reason (orders) + stock (products) |

### Redis

- **Propósito:** Sesiones de conversación del chatbot
- **TTL:** 30 minutos por sesión
- **Clave:** `{businessId}:{customerPhone}`
- **Valor:** `ConversationSession` serializado (JSON)
- **Password:** `orderly_redis_dev_pass`

---

## Convenciones de Código

### Frontend

1. **Componentes standalone** — todos usan `standalone: true`, sin NgModules
2. **Estado con Signals** — `signal<T>()`, `computed()`, `signal.update()`. No usar BehaviorSubject para UI state
3. **Async/await** — servicios usan `firstValueFrom(observable)` para convertir a Promise
4. **CSS scoped** — cada componente tiene su propio `.css`. Nunca usar `::ng-deep`
5. **Variables CSS** — siempre usar `var(--color-*)`, nunca hardcodear colores hex en componentes
6. **Botones globales** — usar clases `.btn .btn-primary`, `.btn .btn-danger` etc. definidas en `styles.css`
7. **Naming:** componentes en `kebab-case` de carpeta, `PascalCase` de clase, archivos `feature.component.ts`
8. **Template syntax nueva:** usar `@if`, `@for`, `@else` (Angular 17 control flow), NO `*ngIf`, `*ngFor`

### Backend

1. **Records para Commands y Requests:**
   ```java
   public record CreateProductCommand(String name, BigDecimal price, ...) {}
   public record UpdateProductRequest(@NotBlank String name, ...) {}
   ```
2. **Adapters @Primary:** `JpaXxxAdapter` siempre tiene `@Primary` sobre `InMemoryXxxRepository`
3. **Interfaces de dominio para puertos:** nunca inyectar JPA repositories directamente en servicios de aplicación
4. **DomainException** para errores de negocio (se mapea a 400 por `ApiExceptionHandler`)
5. **UUID como IDs:** todos los IDs son `UUID`, nunca `Long`
6. **OffsetDateTime** para fechas, no `LocalDateTime`
7. **Package structure por módulo de negocio**, no por capa técnica
8. **Logger:** `private static final Logger log = LoggerFactory.getLogger(Xxx.class)`

### Git / Proyecto

- No hay `.gitignore` revisado, verificar que `target/`, `node_modules/`, `.env` estén ignorados
- No hay CI/CD configurado actualmente

---

## Reglas para Futuras Modificaciones

### Nunca hacer
- ❌ Añadir `*ngIf` o `*ngFor` — usar `@if` / `@for` (Angular 17)
- ❌ Hardcodear colores en CSS de componentes — usar `var(--color-*)`
- ❌ Usar `NgModule` — todos los componentes son standalone
- ❌ Inyectar `JpaRepository` directamente en servicios de aplicación — siempre a través del port
- ❌ Usar `@Transient` para marcar campos no persistidos sin revisar el impacto en validación de Hibernate
- ❌ Cambiar `ddl-auto` a `validate` sin haber corrido las migraciones Flyway correspondientes
- ❌ Añadir íconos `lucide-angular` sin registrarlos en `app.config.ts`
- ❌ Usar `new Subject()` / `new BehaviorSubject()` para estado de componentes — usar `signal()`
- ❌ Crear columnas `NOT NULL` sin valor por defecto en migraciones sobre tablas con datos

### Siempre hacer
- ✅ Registrar nuevos iconos en `app.config.ts` (import + `LucideIconProvider`)
- ✅ Al agregar campo a entidad JPA: crear migración V{n+1} con `ADD COLUMN IF NOT EXISTS`
- ✅ Al agregar método a interface de dominio (port): implementarlo en AMBOS adapters (JPA e InMemory)
- ✅ Usar `TenantAccessService.validateTenantAccess(businessId)` en cada endpoint nuevo
- ✅ Manejar errores en llamadas Evolution API con try/catch y `log.warn` (no fallar el flujo principal)
- ✅ En Angular: usar `firstValueFrom()` para convertir Observables a Promises en métodos `async`
- ✅ En domain models inmutables: las mutaciones retornan nueva instancia (no mutan `this`)
- ✅ Prefijo `[MEDIA:url]` para detectar imágenes en mensajes WhatsApp entrantes

---

## Problemas Detectados y Deuda Técnica

### Arquitectura
1. **`ServiceStatusService` y `BusinessHoursService` son in-memory** — datos se pierden al reiniciar. Deben persistirse en la tabla `businesses` o en una nueva tabla para ser production-ready.
2. **Polling cada 5s en dashboard** en lugar de WebSocket — el backend tiene WebSocket (`OrderWebSocketNotifier`) pero el frontend no lo consume aún.
3. **MongoDB en docker-compose marcado como "legacy"** — no hay código que lo use; genera dependencia innecesaria en docker-compose.
4. **Flyway disabled en dev** — riesgo de que el esquema local diverja del de producción. Habilitar con `baseline-on-migrate: true` cuando el equipo crezca.

### Frontend
5. **Sin librería de formularios** — validaciones de formulario son manuales (`[disabled]="!name || !price"`), no usa `ReactiveFormsModule`. Funcional pero difícil de escalar.
6. **Sin manejo de carga de imágenes progresiva** — `resolveImageUrl()` en productos no tiene fallback visual si la imagen falla.
7. **No hay tests de componentes** — solo existe `app.component.spec.ts` vacío.
8. **`dayLabels` definido en settings component** pero podría ser un pipe reutilizable.
9. **Cancelación de pedidos en dashboard** aparece inline en la card (ocupando espacio visual) en vez de un modal.

### Backend
10. **`OrderStatus` enum en V1 SQL** incluye `IN_PROGRESS` y `READY` que el código ya eliminó del flujo — columna `status` en BD aún tiene esos CHECK values pero el código ya no los usa.
11. **`delivery_type` CHECK constraint** en V1 SQL solo permite `'pickup'|'delivery'` pero el chatbot envía `'DOMICILIO'|'RECOGIDA'` — funciona porque la constraint no existe en las tablas creadas por Hibernate (no por Flyway).
12. **`ConversationSession.setPendingOrderId()`** se usa temporalmente para guardar la URL del comprobante de pago — semánticamente incorrecto (el campo se llama `pendingOrderId` pero guarda una URL).
13. **No hay rate limiting en APIs de negocio** — solo existe en `/auth/login`.
14. **Tests de integración** (`OrderWorkflowIntegrationTest`, `RoleDashboardIntegrationTest`) pueden estar desactualizados con los cambios recientes.

---

## Recomendaciones

### Alta Prioridad
1. **Persistir `ServiceStatusService` y `BusinessHoursService`** en BD (tabla `businesses` o nueva tabla `business_settings`) para sobrevivir reinicios.
2. **Conectar WebSocket en frontend** — `OrderWebSocketNotifier` ya existe en backend, elimina el polling de 5s y mejora la UX.
3. **Habilitar Flyway correctamente** con `baseline-on-migrate: true` para tener control de migraciones en todos los entornos.

### Media Prioridad
4. **Agregar campo `proofImageUrl` a `ConversationSession`** en vez de reutilizar `pendingOrderId` para guardar la URL del comprobante.
5. **Formularios reactivos** (`ReactiveFormsModule`) en lugar de template-driven para settings y creación de productos.
6. **Fallback de imagen** en cards de productos con imagen de placeholder.
7. **Eliminar MongoDB** del `docker-compose.yml` si no se usa.

### Baja Prioridad / Largo Plazo
8. **Tests unitarios en Angular** para servicios y componentes clave (OrderService, DashboardComponent).
9. **Internacionalización (i18n)** — textos hardcodeados en español en templates y en el chatbot.
10. **Documentación OpenAPI** — agregar `springdoc-openapi` para generar Swagger automático.
11. **Separar `styles.css` global** en tokens de diseño (`_tokens.css`) y clases utilitarias (`_utilities.css`).
12. **Agregar `@CheckReturnValue`** en métodos del dominio que retornan nueva instancia (previene bugs de mutación perdida).

---

## Referencias Rápidas

```bash
# Levantar infraestructura
docker-compose up -d

# Backend
cd backend && mvn spring-boot:run

# Frontend
cd frontend && npm start
# → http://localhost:4200

# Health check backend
curl http://localhost:8080/actuator/health

# Ver logs backend en tiempo real
cd backend && mvn spring-boot:run 2>&1 | grep -E "ERROR|WARN|Started"

# Puerto Evolution API
http://localhost:8085
```

```
# Credenciales dev por defecto
DB: orderly_user / orderly_pass (puerto 5433)
Redis: :orderly_redis_dev_pass (puerto 6379)
Evolution API key: orderly-dev-secret-key
JWT secret: orderly-super-secret-key-orderly-super-secret-key (CAMBIAR en prod)
```
