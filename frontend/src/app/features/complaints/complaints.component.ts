import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { firstValueFrom } from 'rxjs';

export interface ComplaintResponse {
  id: string;
  customerPhone: string;
  description: string;
  evidenceUrl: string | null;
  status: 'PENDING' | 'REVIEWED';
  createdAt: string;
}

@Component({
  selector: 'app-complaints',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './complaints.component.html',
  styleUrl: './complaints.component.css'
})
export class ComplaintsComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);

  complaints = signal<ComplaintResponse[]>([]);
  loading    = signal(true);
  error      = signal<string | null>(null);

  get businessId(): string | null { return this.auth.getActiveBusinessId(); }

  ngOnInit(): void {
    this.load();
  }

  async load(): Promise<void> {
    const bId = this.businessId;
    if (!bId) { this.error.set('Sin negocio activo.'); this.loading.set(false); return; }
    try {
      const data = await firstValueFrom(
        this.http.get<ComplaintResponse[]>(`/api/v1/businesses/${bId}/complaints`)
      );
      this.complaints.set(data);
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Error cargando quejas.');
    } finally {
      this.loading.set(false);
    }
  }

  dismissError(): void { this.error.set(null); }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString('es-CO', {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }
}
