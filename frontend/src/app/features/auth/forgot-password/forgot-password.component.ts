import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css'
})
export class ForgotPasswordComponent {
  private readonly http = inject(HttpClient);

  email   = '';
  loading = signal(false);
  sent    = signal(false);
  error   = signal<string | null>(null);

  async onSubmit(): Promise<void> {
    if (!this.email) return;
    this.loading.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.http.post<{ message: string }>(
        '/api/v1/auth/forgot-password', { email: this.email }
      ));
      this.sent.set(true);
    } catch {
      // Always show success to prevent email enumeration
      this.sent.set(true);
    } finally {
      this.loading.set(false);
    }
  }
}
