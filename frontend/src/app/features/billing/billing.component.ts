import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '../../core/services/auth.service';
import { BillingService } from '../../core/services/billing.service';
import { PlanCatalogItem, SubscriptionStatus } from '../../core/models/orderly.models';

type Cycle = 'monthly' | 'annual';

@Component({
  selector: 'app-billing',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule],
  templateUrl: './billing.component.html',
  styleUrl: './billing.component.css'
})
export class BillingComponent implements OnInit {
  private readonly authSvc    = inject(AuthService);
  private readonly billingSvc = inject(BillingService);
  private readonly route      = inject(ActivatedRoute);

  readonly FREE_ORDER_LIMIT = 20;

  loading         = signal(true);
  actionLoading   = signal(false);
  error           = signal<string | null>(null);
  showBudgetPanel = signal(false);

  subscription = signal<SubscriptionStatus | null>(null);
  plans        = signal<PlanCatalogItem[]>([]);
  cycle        = signal<Cycle>('monthly');

  showSuccessBanner = signal(false);
  showCancelBanner  = signal(false);

  // ── Current plan ──────────────────────────────────────────────────────────

  currentPlan = computed(() => {
    const sub = this.subscription();
    if (!sub) return null;
    return this.plans().find(p => p.code === sub.planCode) ?? null;
  });

  // ── Plan usage ────────────────────────────────────────────────────────────

  usagePercent = computed(() => {
    const u = this.subscription()?.currentUsage;
    if (!u?.planOrderLimit) return 0;
    return Math.min(100, Math.round((u.ordersCount / u.planOrderLimit) * 100));
  });

  ordersRemaining = computed(() =>
    this.subscription()?.currentUsage?.ordersRemaining ?? 0
  );

  // ── Free orders (20 gratis incluidos) ────────────────────────────────────

  freeOrdersUsed = computed(() =>
    Math.min(this.subscription()?.currentUsage?.ordersCount ?? 0, this.FREE_ORDER_LIMIT)
  );

  freeOrdersRemaining = computed(() =>
    Math.max(0, this.FREE_ORDER_LIMIT - this.freeOrdersUsed())
  );

  freePercent = computed(() =>
    Math.min(100, Math.round((this.freeOrdersUsed() / this.FREE_ORDER_LIMIT) * 100))
  );

  isFreeDepleted = computed(() => this.freeOrdersUsed() >= this.FREE_ORDER_LIMIT);

  // Show free credits when on trial OR when total orders are still within free limit
  showFreeCredits = computed(() => {
    const sub = this.subscription();
    if (!sub) return true;
    const onTrial = sub.status === 'TRIAL' || sub.status === 'TRIALING';
    const withinFreeLimit = (sub.currentUsage?.ordersCount ?? 0) <= this.FREE_ORDER_LIMIT;
    return onTrial || withinFreeLimit;
  });

  hasPlanLimit = computed(() =>
    (this.subscription()?.currentUsage?.planOrderLimit ?? 0) > 0
  );

  hasOverage = computed(() =>
    (this.subscription()?.currentUsage?.overageCount ?? 0) > 0
  );

  // ── Status flags ──────────────────────────────────────────────────────────

  isTrialing = computed(() => {
    const s = this.subscription()?.status;
    return s === 'TRIAL' || s === 'TRIALING';
  });

  isPastDue = computed(() => this.subscription()?.status === 'PAST_DUE');

  isCanceled = computed(() => {
    const s = this.subscription()?.status;
    return s === 'CANCELED' || s === 'SUSPENDED';
  });

  // ── Plans ─────────────────────────────────────────────────────────────────

  readonly planOrder: Record<string, number> = { starter: 1, growth: 2, business: 3 };

  otherPlans = computed(() =>
    this.plans().filter(p => p.code !== this.subscription()?.planCode)
  );

  // ── Lifecycle ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['success'] === 'true')   this.showSuccessBanner.set(true);
      if (params['cancelled'] === 'true') this.showCancelBanner.set(true);
    });
    this.loadData();
  }

  // ── Data ──────────────────────────────────────────────────────────────────

  async loadData(): Promise<void> {
    this.loading.set(true);
    try {
      const businessId = this.authSvc.getActiveBusinessId();
      if (!businessId) { this.error.set('No hay un negocio activo.'); return; }

      const [sub, plans] = await Promise.all([
        this.billingSvc.getSubscription(businessId).catch((e: any) => {
          if (e?.status === 404 || e?.status === 400) return null;
          throw e;
        }),
        this.billingSvc.getPlans()
      ]);
      this.subscription.set(sub);
      this.plans.set(plans.sort((a, b) =>
        (this.planOrder[a.code] ?? 9) - (this.planOrder[b.code] ?? 9)
      ));
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Error al cargar suscripción.');
    } finally {
      this.loading.set(false);
    }
  }

  // ── Actions ───────────────────────────────────────────────────────────────

  async manageBilling(): Promise<void> {
    this.actionLoading.set(true);
    try {
      const businessId = this.authSvc.getActiveBusinessId()!;
      const { url } = await this.billingSvc.createPortalSession(businessId);
      window.location.href = url;
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Error al abrir portal de facturación.');
      this.actionLoading.set(false);
    }
  }

  async checkout(planCode: string): Promise<void> {
    this.actionLoading.set(true);
    try {
      const businessId = this.authSvc.getActiveBusinessId()!;
      const { url } = await this.billingSvc.createCheckoutSession(businessId, planCode, this.cycle());
      window.location.href = url;
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Error al iniciar pago. Verifica tu configuración de Stripe.');
      this.actionLoading.set(false);
    }
  }

  toggleCycle(): void {
    this.cycle.update(c => c === 'monthly' ? 'annual' : 'monthly');
  }

  toggleBudgetPanel(): void {
    this.showBudgetPanel.update(v => !v);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  getPrice(plan: PlanCatalogItem): number {
    return this.cycle() === 'monthly' ? plan.monthlyPriceUsd : plan.annualPriceUsd / 12;
  }

  isUpgrade(planCode: string): boolean {
    const current = this.subscription()?.planCode ?? 'starter';
    return (this.planOrder[planCode] ?? 9) > (this.planOrder[current] ?? 1);
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      TRIAL: 'Trial', TRIALING: 'Trial', ACTIVE: 'Activo',
      PAST_DUE: 'Pago pendiente', SUSPENDED: 'Suspendido', CANCELED: 'Cancelado'
    };
    return map[status] ?? status;
  }

  planLabel(code: string): string {
    const map: Record<string, string> = {
      starter: 'Starter', growth: 'Growth', business: 'Business'
    };
    return map[code] ?? code;
  }

  formatDate(d: string | null | undefined): string {
    if (!d) return '—';
    return new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium' }).format(new Date(d));
  }

  dismissError():   void { this.error.set(null); }
  dismissSuccess(): void { this.showSuccessBanner.set(false); this.showCancelBanner.set(false); }
}
