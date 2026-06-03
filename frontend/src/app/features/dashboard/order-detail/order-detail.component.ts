import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { OrderService } from '../../../core/services/order.service';
import { OrderResponse } from '../../../core/models/orderly.models';

const STATUS_FLOW = ['PENDING','CONFIRMED','IN_PROGRESS','READY','OUT_FOR_DELIVERY','DELIVERED'];
const STATUS_LABELS: Record<string, string> = {
  PENDING: 'Pendiente', CONFIRMED: 'Confirmado', IN_PROGRESS: 'En preparación',
  READY: 'Listo', OUT_FOR_DELIVERY: 'En camino', DELIVERED: 'Entregado', CANCELLED: 'Cancelado'
};

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './order-detail.component.html',
  styleUrl: './order-detail.component.css'
})
export class OrderDetailComponent implements OnInit {
  private readonly auth   = inject(AuthService);
  private readonly ordSvc = inject(OrderService);
  private readonly route  = inject(ActivatedRoute);
  private readonly router = inject(Router);

  order   = signal<OrderResponse | null>(null);
  loading = signal(true);
  saving  = signal(false);
  error   = signal<string | null>(null);

  readonly statusLabels = STATUS_LABELS;
  readonly statusFlow   = STATUS_FLOW;

  get businessId(): string | null { return this.auth.getActiveBusinessId(); }

  ngOnInit(): void {
    const nav = this.router.getCurrentNavigation()?.extras.state as { order?: OrderResponse } | undefined;
    if (nav?.order) {
      this.order.set(nav.order);
      this.loading.set(false);
    } else {
      this.loading.set(false);
      this.error.set('No se encontraron datos del pedido.');
    }
  }

  async setStatus(status: string): Promise<void> {
    const bId = this.businessId;
    const ord = this.order();
    if (!bId || !ord) return;
    this.saving.set(true);
    try {
      const updated = await this.ordSvc.updateStatus(bId, ord.id, status);
      this.order.set(updated);
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Error actualizando estado.');
    } finally {
      this.saving.set(false);
    }
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }

  onProofFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = async () => {
      const dataUrl = reader.result as string;
      const bId = this.businessId;
      const ord = this.order();
      if (!bId || !ord) return;
      this.saving.set(true);
      try {
        const updated = await this.ordSvc.setPaymentProof(bId, ord.id, dataUrl);
        this.order.set(updated);
      } catch (e: any) {
        this.error.set(e?.error?.message ?? 'Error subiendo comprobante.');
      } finally {
        this.saving.set(false);
      }
    };
    reader.readAsDataURL(file);
  }

  statusIndex(status: string): number {
    return STATUS_FLOW.indexOf(status);
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(amount);
  }

  formatDate(dateStr: string): string {
    return new Intl.DateTimeFormat('es-CO', {
      dateStyle: 'medium', timeStyle: 'short'
    }).format(new Date(dateStr));
  }
}
