export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'OPERATOR' | string;

export interface AuthUser {
  id: string;
  fullName: string;
  email: string;
  role: UserRole;
}

export interface Business {
  id: string;
  name: string;
  slug: string;
  businessType: string;
  status: string;
  countryCode: string;
  currencyCode: string;
  timezone: string;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  user: AuthUser;
  businesses: Business[];
}

export interface BusinessDashboardResponse {
  role: string;
  businessId: string;
  businessName: string;
  totalProducts: number;
  totalOrders: number;
  pendingOrders: number;
  inProgressOrders: number;
  readyOrders: number;
  deliveredOrders: number;
  totalRevenue: number;
}

export interface SystemOverviewResponse {
  role: string;
  totalBusinesses: number;
  totalProducts: number;
  totalOrders: number;
  pendingOrders: number;
  readyOrders: number;
  totalRevenue: number;
}

export interface OperatorOverviewResponse {
  role: string;
  visibleBusinesses: number;
  pendingOrders: number;
  inProgressOrders: number;
  readyOrders: number;
  openHandoffs: number;
}

export interface ProductResponse {
  id: string;
  businessId: string;
  name: string;
  description: string;
  imageUrl?: string;
  price: number;
  available: boolean;
  createdAt: string;
  stock?: number | null;
}

export interface OrderLineResponse {
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  notes?: string;
}

export interface OrderResponse {
  id: string;
  businessId: string;
  customerName: string;
  customerWhatsapp: string;
  deliveryType: string;
  notes?: string;
  status: string;
  totalAmount: number;
  items: OrderLineResponse[];
  createdAt: string;
  paymentMethod?: string;
  paymentProofUrl?: string;
  cancelReason?: string;
}

export interface DaySchedule {
  day: string;
  openTime: string;
  closeTime: string;
  closed: boolean;
}

export interface ServiceStatus {
  enabled: boolean;
}

export interface CreateBusinessPayload {
  name: string;
  businessType: string;
  countryCode: string;
  currencyCode: string;
  timezone: string;
}

export interface CreateProductPayload {
  name: string;
  description: string;
  imageUrl?: string;
  price: number;
  available?: boolean;
  stock?: number | null;
}

export interface CreateOrderPayload {
  customerName: string;
  customerWhatsapp: string;
  deliveryType: string;
  notes?: string;
  items: Array<{
    productId: string;
    quantity: number;
    notes?: string;
  }>;
}

export interface DemoSeedResult {
  businesses: Business[];
  activeBusinessId: string | null;
}

// ── CEO Panel models ──────────────────────────────────────────────────────────

export interface GlobalDashboardMetrics {
  totalBusinesses: number;
  activeBusinesses: number;
  trialingBusinesses: number;
  suspendedBusinesses: number;
  cancelledBusinesses: number;
  setupBusinesses: number;
  activeSubscriptions: number;
  trialSubscriptions: number;
  suspendedSubscriptions: number;
  ordersToday: number;
  ordersThisMonth: number;
  ordersTotal: number;
  revenueThisMonth: number;
  revenueTotal: number;
  recentActivity: RecentActivityItem[];
}

export interface RecentActivityItem {
  id: string;
  actorEmail: string;
  action: string;
  targetType: string | null;
  targetId: string | null;
  targetName: string | null;
  details: string | null;
  createdAt: string;
}

export interface BusinessPlanInfo {
  id: string;
  code: string;
  name: string;
  monthlyPrice: number;
  monthlyPriceUsd: number;
}

export interface BusinessSubscriptionInfo {
  status: string;
  trialEndsAt: string | null;
  currentPeriodEnd: string | null;
  provider: string;
}

export interface BusinessUsageInfo {
  ordersThisMonth: number;
  orderLimit: number;
  overageCount: number;
}

export interface BusinessAdminDetail extends Business {
  ownerId: string;
  ownerEmail: string;
  ownerName: string;
  plan: BusinessPlanInfo | null;
  subscription: BusinessSubscriptionInfo | null;
  usage: BusinessUsageInfo;
  totalOrders: number;
  totalRevenue: number;
}

export interface AuditLogEntry {
  id: string;
  actorId: string;
  actorEmail: string;
  action: string;
  targetType: string;
  targetId: string | null;
  targetName: string | null;
  details: string | null;
  createdAt: string;
}

export interface EmailLogEntry {
  id: string;
  recipientEmail: string;
  recipientName: string | null;
  subject: string;
  status: string;
  sentAt: string | null;
  sentByEmail: string;
  createdAt: string;
}

export interface ImpersonationResponse {
  token: string;
  ownerEmail: string;
  ownerName: string;
  expiresAt: string;
}

export interface PlanAdminInfo {
  id: string;
  code: string;
  name: string;
  monthlyPrice: number;
  monthlyPriceUsd: number;
  maxOrdersPerMonth: number | null;
  maxProducts: number | null;
  trialDays: number;
}

// ── Plan catalogue (public endpoint /api/v1/plans) ─────────────────────────
export interface PlanCatalogItem {
  id: string;
  code: string;
  name: string;
  monthlyPriceCop: number;
  monthlyPriceUsd: number;
  annualPriceCop: number;
  annualPriceUsd: number;
  monthlyOrderLimit: number;
  productLimit: number;
  userLimit: number;
  trialDays: number;
  analyticsEnabled: boolean;
  multiLocation: boolean;
  customBranding: boolean;
  prioritySupport: boolean;
  overageRateCop: number;
  overageRateUsd: number;
  overageBlockSize: number;
}

// ── Billing (Stripe) ─────────────────────────────────────────────────────

export interface CheckoutSessionResponse {
  url: string;
}

export interface PortalSessionResponse {
  url: string;
}

// ── Subscription status + usage (per business) ────────────────────────────
export interface UsagePeriodInfo {
  periodStart: string;
  periodEnd: string;
  planOrderLimit: number;
  ordersCount: number;
  ordersRemaining: number;
  overageCount: number;
  overageBlocks: number;
  overageChargeCop: number;
  overageChargeUsd: number;
  isFinalized: boolean;
}

export interface SubscriptionStatus {
  subscriptionId: string;
  businessId: string;
  planCode: string | null;
  planName: string | null;
  status: string;
  trialEndsAt: string | null;
  currentPeriodStart: string | null;
  currentPeriodEnd: string | null;
  cancelAtPeriodEnd: boolean;
  cancelledAt: string | null;
  currentUsage: UsagePeriodInfo | null;
}
