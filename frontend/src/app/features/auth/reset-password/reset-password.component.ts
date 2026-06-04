import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css'
})
export class ResetPasswordComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly http  = inject(HttpClient);

  token       = '';
  newPassword = '';
  confirm     = '';
  loading     = signal(false);
  success     = signal(false);
  error       = signal<string | null>(null);

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
  }

  get passwordsMatch(): boolean {
    return this.newPassword === this.confirm;
  }

  async onSubmit(): Promise<void> {
    if (!this.token || !this.newPassword || !this.passwordsMatch) return;
    if (this.newPassword.length < 8) {
      this.error.set('La contraseña debe tener al menos 8 caracteres.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.http.post<{ message: string }>(
        '/api/v1/auth/reset-password', { token: this.token, newPassword: this.newPassword }
      ));
      this.success.set(true);
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'El enlace es inválido o ha expirado. Solicita uno nuevo.');
    } finally {
      this.loading.set(false);
    }
  }
}
