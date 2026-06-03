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

  loading       = signal(true);
  actionLoading = signal(false);
  error         = signal<string | null>(null);
  successMsg    = signal<string | null>(null);

  subscription = signal<SubscriptionStatus | null>(null);
  plans        = signal<PlanCatalogItem[]>([]);
  cycle        = signal<Cycle>('monthly');

  showSuccessBanner = signal(false);
  showCancelBanner  = signal(false);

  currentPlan = computed(() => {
    const sub = this.subscription();
    if (!sub) return null;
    return this.plans().find(p => p.code === sub.planCode) ?? null;
  });

  usagePercent = computed(() => {
    const usage = this.subscription()?.currentUsage;
    if (!usage || !usage.planOrderLimit) return 0;
    return Math.min(100, Math.round((usage.ordersCount / usage.planOrderLimit) * 100));
  });

  ordersRemaining = computed(() => this.subscription()?.currentUsage?.ordersRemaining ?? 0);

  otherPlans = computed(() => {
    const code = this.subscription()?.planCode;
    return this.plans().filter(p => p.code !== code);
  });

  isTrialing = computed(() => this.subscription()?.status === 'TRIAL');
  isPastDue  = computed(() => this.subscription()?.status === 'PAST_DUE');
  isCanceled = computed(() => this.subscription()?.status === 'CANCELED' || this.subscription()?.status === 'SUSPENDED');

  readonly planOrder: Record<string, number> = { starter: 1, growth: 2, business: 3 };

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['success'] === 'true')    this.showSuccessBanner.set(true);
      if (params['cancelled'] === 'true')  this.showCancelBanner.set(true);
    });
    this.loadData();
  }

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
      this.plans.set(plans.sort((a, b) => (this.planOrder[a.code] ?? 9) - (this.planOrder[b.code] ?? 9)));
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Error al cargar suscripción.');
    } finally {
      this.loading.set(false);
    }
  }

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

  getPrice(plan: PlanCatalogItem): number {
    return this.cycle() === 'monthly' ? plan.monthlyPriceUsd : plan.annualPriceUsd / 12;
  }

  isUpgrade(planCode: string): boolean {
    const current = this.subscription()?.planCode ?? 'starter';
    return (this.planOrder[planCode] ?? 9) > (this.planOrder[current] ?? 1);
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      TRIAL: 'Trial', ACTIVE: 'Activo', PAST_DUE: 'Pago pendiente',
      SUSPENDED: 'Suspendido', CANCELED: 'Cancelado'
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
    return new Intl.DateTimeFormat('es-CO', { dateStyle: 'long' }).format(new Date(d));
  }

  dismissError():   void { this.error.set(null); }
  dismissSuccess(): void { this.showSuccessBanner.set(false); this.showCancelBanner.set(false); }
}
