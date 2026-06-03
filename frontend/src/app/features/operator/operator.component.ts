import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '../../core/services/auth.service';
import { OrderService } from '../../core/services/order.service';
import { OperatorOverviewResponse, OrderResponse } from '../../core/models/orderly.models';

@Component({
  selector: 'app-operator',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './operator.component.html',
  styleUrl: './operator.component.css'
})
export class OperatorComponent implements OnInit {
  private readonly auth   = inject(AuthService);
  private readonly ordSvc = inject(OrderService);

  overview = signal<OperatorOverviewResponse | null>(null);
  orders   = signal<OrderResponse[]>([]);
  loading  = signal(true);
  error    = signal<string | null>(null);
  saving   = signal(false);

  get businessId(): string | null { return this.auth.getActiveBusinessId(); }

  ngOnInit(): void {
    this.loadAll();
  }

  async loadAll(): Promise<void> {
    const bId = this.businessId;
    try {
      const [ov, ords] = await Promise.all([
        this.ordSvc.getOperatorOverview(),
        bId ? this.ordSvc.list(bId) : Promise.resolve([])
      ]);
      this.overview.set(ov);
      this.orders.set(ords.filter(o => ['PENDING','CONFIRMED','IN_PROGRESS','READY'].includes(o.status)));
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Error cargando datos de operaciones.');
    } finally {
      this.loading.set(false);
    }
  }

  async advanceStatus(order: OrderResponse): Promise<void> {
    const bId = this.businessId;
    if (!bId) return;
    const flow: Record<string, string> = {
      PENDING: 'CONFIRMED', CONFIRMED: 'IN_PROGRESS',
      IN_PROGRESS: 'READY', READY: 'OUT_FOR_DELIVERY'
    };
    const next = flow[order.status];
    if (!next) return;
    this.saving.set(true);
    try {
      await this.ordSvc.updateStatus(bId, order.id, next);
      await this.loadAll();
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Error actualizando estado.');
    } finally {
      this.saving.set(false);
    }
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'Pendiente', CONFIRMED: 'Confirmado',
      IN_PROGRESS: 'En preparación', READY: 'Listo'
    };
    return map[status] ?? status;
  }

  statusColor(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'warning', CONFIRMED: 'accent', IN_PROGRESS: 'primary', READY: 'purple'
    };
    return map[status] ?? 'text';
  }

  nextActionLabel(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'Confirmar', CONFIRMED: 'Preparar', IN_PROGRESS: 'Listo', READY: 'En camino'
    };
    return map[status] ?? '';
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
}


