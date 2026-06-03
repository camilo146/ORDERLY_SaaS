import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, LucideAngularModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private readonly auth = inject(AuthService);

  constructor() {
    this.auth.clearSession();
  }

  email    = '';
  password = '';
  loading  = signal(false);
  error    = signal<string | null>(null);

  async onSubmit(): Promise<void> {
    if (!this.email || !this.password) return;
    this.loading.set(true);
    this.error.set(null);
    try {
      await this.auth.login(this.email, this.password);
    } catch (e: any) {
      this.error.set(e?.error?.message ?? 'Credenciales incorrectas. Verifica e intenta de nuevo.');
    } finally {
      this.loading.set(false);
    }
  }
}
