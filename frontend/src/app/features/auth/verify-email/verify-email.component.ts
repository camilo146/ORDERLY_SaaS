import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './verify-email.component.html',
  styleUrl: './verify-email.component.css'
})
export class VerifyEmailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly http  = inject(HttpClient);

  status  = signal<'loading' | 'success' | 'error'>('loading');
  message = signal<string>('Verificando tu correo...');

  async ngOnInit(): Promise<void> {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.status.set('error');
      this.message.set('Enlace de verificación inválido. Solicita uno nuevo desde tu panel.');
      return;
    }
    try {
      await firstValueFrom(this.http.post<{ message: string }>(
        `/api/v1/auth/verify-email?token=${encodeURIComponent(token)}`, {}
      ));
      this.status.set('success');
      this.message.set('¡Tu correo fue verificado exitosamente!');
    } catch (e: any) {
      this.status.set('error');
      this.message.set(e?.error?.message ?? 'El enlace ha expirado o ya fue usado. Solicita uno nuevo.');
    }
  }
}
