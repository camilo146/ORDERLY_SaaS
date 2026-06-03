import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '../../../core/services/auth.service';
import { BillingService } from '../../../core/services/billing.service';

type Cycle = 'monthly' | 'annual';

@Component({
  selector: 'app-pricing',
  standalone: true,
  imports: [RouterLink, LucideAngularModule],
  templateUrl: './pricing.component.html',
  styleUrl: './pricing.component.css'
})
export class PricingComponent {
  private readonly authSvc    = inject(AuthService);
  private readonly billingSvc = inject(BillingService);
  private readonly router     = inject(Router);

  cycle         = signal<Cycle>('monthly');
  checkoutLoading = signal(false);

  prices: Record<string, Record<Cycle, number>> = {
    starter:  { monthly: 17,  annual: 14  },
    growth:   { monthly: 55,  annual: 44  },
    business: { monthly: 199, annual: 159 }
  };

  plans = [
    {
      key: 'starter',
      name: 'Starter',
      desc: 'Para negocios que están empezando con WhatsApp',
      featured: false,
      features: [
        { text: '1 número de WhatsApp',           included: true },
        { text: 'Hasta 500 pedidos/mes',           included: true },
        { text: 'Catálogo de productos ilimitado', included: true },
        { text: '1 operador',                      included: true },
        { text: 'Soporte por email',               included: true },
        { text: 'Analytics avanzado',              included: false },
        { text: 'Múltiples operadores',            included: false },
        { text: 'Multi-negocio',                   included: false },
        { text: 'Soporte prioritario',             included: false },
        { text: 'API de integración',              included: false }
      ]
    },
    {
      key: 'growth',
      name: 'Growth',
      desc: 'Para negocios en crecimiento con mayor volumen',
      featured: true,
      features: [
        { text: '1 número de WhatsApp',           included: true },
        { text: 'Hasta 2,000 pedidos/mes',         included: true },
        { text: 'Catálogo de productos ilimitado', included: true },
        { text: 'Hasta 3 operadores',              included: true },
        { text: 'Soporte por email',               included: true },
        { text: 'Analytics avanzado',              included: true },
        { text: 'Múltiples operadores',            included: true },
        { text: 'Multi-negocio',                   included: false },
        { text: 'Soporte prioritario',             included: true },
        { text: 'API de integración',              included: false }
      ]
    },
    {
      key: 'business',
      name: 'Business',
      desc: 'Para cadenas y operaciones de alto volumen',
      featured: false,
      features: [
        { text: 'Múltiples números WhatsApp',      included: true },
        { text: 'Pedidos ilimitados',              included: true },
        { text: 'Catálogo de productos ilimitado', included: true },
        { text: 'Operadores ilimitados',           included: true },
        { text: 'Soporte por email',               included: true },
        { text: 'Analytics avanzado',              included: true },
        { text: 'Múltiples operadores',            included: true },
        { text: 'Multi-negocio',                   included: true },
        { text: 'Soporte prioritario 24/7',        included: true },
        { text: 'API de integración',              included: true }
      ]
    }
  ];

  faq = [
    { q: '¿Puedo cambiar de plan en cualquier momento?',
      a: 'Sí. Puedes subir o bajar de plan cuando lo necesites desde tu panel de facturación. Los cambios se aplican inmediatamente a través de Stripe.' },
    { q: '¿Qué pasa cuando supero el límite de pedidos?',
      a: 'Te avisamos cuando estás cerca del límite. Puedes subir de plan o esperar al siguiente mes.' },
    { q: '¿Necesito un número de WhatsApp Business?',
      a: 'Recomendamos un número dedicado al negocio. Puede ser un número normal o Business.' },
    { q: '¿Los 7 días de prueba son realmente gratis?',
      a: 'Sí, 100% gratis. Al configurar tu método de pago, el cobro inicia solo después de los 7 días de prueba. Puedes cancelar antes sin costo.' }
  ];

  getPrice(key: string): number {
    return this.prices[key][this.cycle()];
  }

  savings(key: string): number {
    const m = this.prices[key].monthly;
    const a = this.prices[key].annual;
    return Math.round(((m - a) / m) * 100);
  }

  async onPlanClick(planKey: string): Promise<void> {
    const businessId = this.authSvc.getActiveBusinessId();

    // Business plan → always to contact
    if (planKey === 'business') {
      this.router.navigate(['/contact']);
      return;
    }

    // Not authenticated → register
    if (!businessId) {
      this.router.navigate(['/register']);
      return;
    }

    // Authenticated + has business → Stripe Checkout
    this.checkoutLoading.set(true);
    try {
      const { url } = await this.billingSvc.createCheckoutSession(businessId, planKey, this.cycle());
      window.location.href = url;
    } catch {
      // Fallback to billing dashboard on error
      this.router.navigate(['/billing']);
    } finally {
      this.checkoutLoading.set(false);
    }
  }
}
