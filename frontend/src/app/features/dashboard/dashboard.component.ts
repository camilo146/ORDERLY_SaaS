import { Component, inject, OnDestroy, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { BusinessService } from '../../core/services/business.service';
import { OrderService } from '../../core/services/order.service';
import { ProductService } from '../../core/services/product.service';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import {
  BusinessDashboardResponse,
  CreateOrderPayload,
  OrderResponse,
  ProductResponse
} from '../../core/models/orderly.models';

export interface KanbanColumn {
  status: string;
  label: string;
  color: string;
  orders: OrderResponse[];
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit, OnDestroy {
  private readonly auth    = inject(AuthService);
  private readonly bizSvc  = inject(BusinessService);
  private readonly ordSvc  = inject(OrderService);
  private readonly prodSvc = inject(ProductService);
  private readonly http    = inject(HttpClient);
  private readonly router  = inject(Router);

  loading          = signal(true);
  error            = signal<string | null>(null);
  dashboard        = signal<BusinessDashboardResponse | null>(null);
  orders           = signal<OrderResponse[]>([]);
  products         = signal<ProductResponse[]>([]);
  showForm         = signal(false);
  saving           = signal(false);
  waConnected      = signal(false);
  serviceEnabled   = signal(true);
  togglingService  = signal(false);
  cancellingOrder  = signal<OrderResponse | null>(null);
  cancelReason     = signal('');
  proofViewOrder   = signal<OrderResponse | null>(null);

  /** Onboarding progress: 5 checkpoints × 20 % */
  readonly setupProgress = computed(() => {
    let pct = 0;
    if (this.dashboard()) pct += 20;                      // 1. Business profile complete
    if (this.products().length >= 3)  pct += 20;          // 2. At least 3 products
    if (this.waConnected())           pct += 20;          // 3. WhatsApp connected
    if (this.orders().length > 0)     pct += 20;          // 4. First order received
    return pct;
  });

  readonly setupSteps = computed(() => [
    { label: 'Perfil del negocio',    done: !!this.dashboard() },
    { label: 'Catálogo (3 productos)', done: this.products().length >= 3 },
    { label: 'WhatsApp conectado',    done: this.waConnected() },
    { label: 'Primer pedido',         done: this.orders().length > 0 },
  ]);

  columns: KanbanColumn[] = [
    { status: 'PENDING',          label: 'Pendientes',  color: 'warning', orders: [] },
    { status: 'CONFIRMED',        label: 'Confirmados', color: 'accent',  orders: [] },
    { status: 'OUT_FOR_DELIVERY', label: 'En camino',   color: 'orange',  orders: [] },
    { status: 'DELIVERED',        label: 'Entregados',  color: 'success', orders: [] },
  ];

  // New order form
  newOrder: CreateOrderPayload = this.emptyOrder();

  private pollTimer: ReturnType<typeof setInterval> | null = null;

  get businessId(): string | null { return this.auth.getActiveBusinessId(); }

  ngOnInit(): void {
    this.loadAll();
    this.pollTimer = setInterval(() => this.loadOrders(), 5_000);
  }

  ngOnDestroy(): void {
    if (this.pollTimer) clearInterval(this.pollTimer);
  }

  async loadAll(): Promise<void> {
    const bId = this.businessId;
    if (!bId) { this.error.set('Sin negocio activo. Inicia sesión de nuevo.'); this.loading.set(false); return; }
    try {
      const [dash, ords, prods] = await Promise.all([
        this.bizSvc.getDashboard(bId),
        this.ordSvc.list(bId),
        this.prodSvc.list(bId)
      ]);
      this.dashboard.set(dash);
      this.products.set(prods);
      this.applyOrders(ords);
      try {
        const wa = await firstValueFrom(
          this.http.get<{ status: string }>(`/api/v1/businesses/${bId}/settings/whatsapp-channel`)
        );
        this.waConnected.set(wa.status === 'ACTIVE');
      } catch { /* ignore */ }
      try {
        const svc = await this.ordSvc.getServiceStatus(bId);
        this.serviceEnabled.set(svc.enabled);
      } catch { /* ignore */ }
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Error cargando datos.');
    } finally {
      this.loading.set(false);
    }
  }

  async loadOrders(): Promise<void> {
    const bId = this.businessId;
    if (!bId) return;
    try {
      const ords = await this.ordSvc.list(bId);
      this.applyOrders(ords);
    } catch { /* silent poll */ }
  }

  private applyOrders(ords: OrderResponse[]): void {
    this.orders.set(ords);
    for (const col of this.columns) {
      col.orders = ords.filter(o => o.status === col.status);
    }
  }

  async advanceStatus(order: OrderResponse): Promise<void> {
    const bId = this.businessId;
    if (!bId) return;
    const next = this.nextStatus(order.status);
    if (!next) return;
    try {
      await this.ordSvc.updateStatus(bId, order.id, next);
      await this.loadOrders();
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Error actualizando estado.');
    }
  }

  requestCancel(order: OrderResponse): void {
    this.cancellingOrder.set(order);
    this.cancelReason.set('');
  }

  dismissCancel(): void {
    this.cancellingOrder.set(null);
    this.cancelReason.set('');
  }

  async confirmCancel(): Promise<void> {
    const bId = this.businessId;
    const order = this.cancellingOrder();
    if (!bId || !order) return;
    this.saving.set(true);
    try {
      await this.ordSvc.cancelWithReason(bId, order.id, this.cancelReason());
      this.cancellingOrder.set(null);
      this.cancelReason.set('');
      await this.loadOrders();
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Error cancelando pedido.');
    } finally {
      this.saving.set(false);
    }
  }

  async toggleService(): Promise<void> {
    const bId = this.businessId;
    if (!bId) return;
    this.togglingService.set(true);
    try {
      const res = await this.ordSvc.toggleServiceStatus(bId);
      this.serviceEnabled.set(res.enabled);
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Error cambiando estado del servicio.');
    } finally {
      this.togglingService.set(false);
    }
  }

  openProofView(order: OrderResponse): void {
    this.proofViewOrder.set(order);
  }

  closeProofView(): void {
    this.proofViewOrder.set(null);
  }

  async approveProof(order: OrderResponse): Promise<void> {
    const bId = this.businessId;
    if (!bId) return;
    try {
      await this.ordSvc.updateStatus(bId, order.id, 'CONFIRMED');
      this.proofViewOrder.set(null);
      await this.loadOrders();
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Error aprobando comprobante.');
    }
  }

  async rejectProof(order: OrderResponse): Promise<void> {
    const bId = this.businessId;
    if (!bId) return;
    try {
      await this.ordSvc.cancelWithReason(bId, order.id, 'Comprobante de pago no auténtico');
      this.proofViewOrder.set(null);
      await this.loadOrders();
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Error rechazando comprobante.');
    }
  }

  viewDetail(order: OrderResponse): void {
    this.router.navigate(['/dashboard/orders', order.id], {
      state: { order, businessId: this.businessId }
    });
  }

  async createOrder(): Promise<void> {
    const bId = this.businessId;
    if (!bId) return;
    this.saving.set(true);
    try {
      await this.ordSvc.create(bId, this.newOrder);
      this.newOrder = this.emptyOrder();
      this.showForm.set(false);
      await this.loadOrders();
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Error creando pedido.');
    } finally {
      this.saving.set(false);
    }
  }

  nextStatus(current: string): string | null {
    const flow: Record<string, string> = {
      PENDING: 'CONFIRMED',
      CONFIRMED: 'OUT_FOR_DELIVERY',
      OUT_FOR_DELIVERY: 'DELIVERED'
    };
    return flow[current] ?? null;
  }

  nextLabel(current: string): string {
    const labels: Record<string, string> = {
      PENDING: 'Confirmar',
      CONFIRMED: 'Enviar',
      OUT_FOR_DELIVERY: 'Entregar'
    };
    return labels[current] ?? '';
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(amount);
  }

  timeAgo(dateStr: string): string {
    const diff = Date.now() - new Date(dateStr).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'Ahora';
    if (mins < 60) return `${mins}m`;
    return `${Math.floor(mins / 60)}h ${mins % 60}m`;
  }

  dismissError(): void { this.error.set(null); }

  private emptyOrder(): CreateOrderPayload {
    return { customerName: '', customerWhatsapp: '', deliveryType: 'DELIVERY', notes: '', items: [] };
  }
}


