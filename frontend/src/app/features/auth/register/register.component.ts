import { Component, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { AuthResponse } from '../../../core/models/orderly.models';
import { LucideAngularModule } from 'lucide-angular';
import { WhatsAppService } from '../../../core/services/whatsapp.service';

export interface BusinessTypeOption {
  value: string;
  label: string;
  icon: string;
  description: string;
  sampleProducts: Array<{ name: string; price: number }>;
}

const BUSINESS_TYPES: BusinessTypeOption[] = [
  {
    value: 'RESTAURANT', label: 'Restaurante', icon: 'utensils', description: 'Comidas, bebidas y domicilios',
    sampleProducts: [
      { name: 'Almuerzo del día', price: 15000 },
      { name: 'Agua botella 500ml', price: 2000 },
      { name: 'Bandeja paisa', price: 18000 },
    ]
  },
  {
    value: 'PHARMACY', label: 'Droguería', icon: 'pill', description: 'Medicamentos y salud',
    sampleProducts: [
      { name: 'Acetaminofén 500mg x10', price: 3500 },
      { name: 'Tensiómetro digital', price: 85000 },
      { name: 'Vitamina C 1000mg', price: 12000 },
    ]
  },
  {
    value: 'AUTO_PARTS', label: 'Repuestos Moto', icon: 'wrench', description: 'Piezas y accesorios para motos',
    sampleProducts: [
      { name: 'Aceite motor 20W-50', price: 28000 },
      { name: 'Filtro de aire moto', price: 15000 },
      { name: 'Pastillas de freno', price: 45000 },
    ]
  },
  {
    value: 'FASHION', label: 'Ropa y Moda', icon: 'shirt', description: 'Tienda de ropa y accesorios',
    sampleProducts: [
      { name: 'Camiseta básica', price: 25000 },
      { name: 'Jean clásico', price: 65000 },
      { name: 'Zapatos casuales', price: 80000 },
    ]
  },
  {
    value: 'GENERAL_STORE', label: 'Tienda General', icon: 'store', description: 'Abarrotes y productos variados',
    sampleProducts: [
      { name: 'Arroz 1kg', price: 4500 },
      { name: 'Aceite vegetal 1L', price: 8000 },
      { name: 'Azúcar blanca 1kg', price: 4000 },
    ]
  },
];

const PAYMENT_OPTIONS = [
  'Efectivo',
  'Nequi',
  'Daviplata',
  'Transferencia bancaria',
  'Tarjeta crédito/débito',
];

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, LucideAngularModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly whatsappSvc = inject(WhatsAppService);

  constructor() {
    this.auth.clearSession();
  }

  readonly businessTypes = BUSINESS_TYPES;
  readonly paymentOptions = PAYMENT_OPTIONS;

  step = signal(1);
  loading = signal(false);
  errorMsg = signal('');

  // ── Step 1 — Cuenta ──────────────────────────────────────────────────────────
  fullName = '';
  email = '';
  password = '';
  confirmPassword = '';

  // ── Step 2 — Tipo de negocio ─────────────────────────────────────────────────
  selectedType = signal('');

  // ── Step 3 — Datos del negocio ───────────────────────────────────────────────
  businessName = '';
  countryCode = 'CO';
  currencyCode = 'COP';
  selectedPayments = signal<string[]>([]);
  paymentMethodStates = signal<{name: string, details: string, checked: boolean}[]>(
    PAYMENT_OPTIONS.map(b => ({name: b, details: '', checked: false}))
  );

  // ── Step 4 — Catálogo inicial (3 productos) ──────────────────────────────────
  products = signal([
    { name: '', price: 0 },
    { name: '', price: 0 },
    { name: '', price: 0 },
  ]);

  // ── Step 5 — Mascota del bot ──────────────────────────────────────────────────
  botName = '';
  botEmoji = '🤖';

  readonly mascotOptions = [
    { emoji: '🐱', label: 'Gatito' },
    { emoji: '🐶', label: 'Perrito' },
    { emoji: '🐰', label: 'Conejito' },
    { emoji: '🐻', label: 'Osito' },
    { emoji: '🦊', label: 'Zorrito' },
    { emoji: '🐼', label: 'Pandita' },
    { emoji: '🦉', label: 'Búho' },
    { emoji: '🐧', label: 'Pingüino' },
    { emoji: '🐸', label: 'Ranita' },
    { emoji: '🐥', label: 'Pollito' },
    { emoji: '🐹', label: 'Hámster' },
    { emoji: '🐨', label: 'Koala' },
    { emoji: '🐯', label: 'Tigrito' },
    { emoji: '🦁', label: 'León' },
    { emoji: '🦄', label: 'Unicornio' },
    { emoji: '🐙', label: 'Pulpito' },
    { emoji: '🦋', label: 'Mariposa' },
    { emoji: '🤖', label: 'Robot' },
  ];

  // ── Step 6 — WhatsApp QR ──────────────────────────────────────────────────────
  registeredBusinessId = signal<string | null>(null);
  qrConfirmed = signal(false);
  qrBase64 = signal<string | null>(null);
  qrStatus = signal<string>('loading');
  private qrPollTimer: ReturnType<typeof setInterval> | null = null;

  // ── Computed ─────────────────────────────────────────────────────────────────
  readonly selectedTypeObj = computed(() =>
    this.businessTypes.find(b => b.value === this.selectedType())
  );

  // ── Helpers ───────────────────────────────────────────────────────────────────
  selectType(value: string) {
    this.selectedType.set(value);
    // Pre-fill products with typical items for the business type
    const typeObj = this.businessTypes.find(b => b.value === value);
    if (typeObj) {
      this.products.set(typeObj.sampleProducts.map(p => ({ name: p.name, price: p.price })));
    }
  }

  onPaymentChecked(method: any) {
    if (!method.checked) method.details = '';
  }

  hasPaymentSelected(): boolean {
    return this.paymentMethodStates().some(p => p.checked);
  }

  updateProduct(index: number, field: 'name' | 'price', value: string | number) {
    const updated = [...this.products()];
    updated[index] = { ...updated[index], [field]: value };
    this.products.set(updated);
  }

  canNext(): boolean {
    switch (this.step()) {
      case 1:
        return !!this.fullName && !!this.email && this.password.length >= 8
          && this.password === this.confirmPassword;
      case 2:
        return !!this.selectedType();
      case 3:
        return !!this.businessName && this.hasPaymentSelected();
      case 4:
        return this.products().every(p => !!p.name && p.price > 0);
      case 5:
        return !!this.botName.trim() && !!this.botEmoji;
      case 6:
        return this.qrConfirmed();
      default:
        return true;
    }
  }

  next() {
    if (!this.canNext()) return;
    if (this.step() === 5) {
      // Register account + business + products + mascota on transition 5→6
      this.register();
    } else if (this.step() < 7) {
      this.step.set(this.step() + 1);
    }
  }

  back() {
    if (this.step() > 1 && this.step() < 6) {
      this.step.set(this.step() - 1);
    }
  }

  private register() {
    this.loading.set(true);
    this.errorMsg.set('');

    const payload = {
      fullName: this.fullName,
      email: this.email,
      password: this.password,
      businessName: this.businessName,
      businessType: this.selectedType(),
      countryCode: this.countryCode,
      currencyCode: this.currencyCode,
      timezone: 'America/Bogota',
      products: this.products().map(p => ({
        name: p.name,
        description: '',
        price: p.price,
      })),
      botName: this.botName.trim(),
      botEmoji: this.botEmoji,
    };

    this.http.post<AuthResponse>('/api/v1/auth/register', payload).subscribe({
      next: (res) => {
        this.auth.saveSession(res);
        const bizId = res.businesses?.[0]?.id ?? null;
        this.registeredBusinessId.set(bizId);
        this.loading.set(false);
        this.step.set(6);
        if (bizId) this.startQrPolling(bizId);
      },
      error: (err) => {
        this.errorMsg.set(err.error?.message || 'Error al crear la cuenta. Intenta de nuevo.');
        this.loading.set(false);
      }
    });
  }

  private startQrPolling(bizId: string) {
    this.qrBase64.set(null);
    this.qrStatus.set('loading');
    if (this.qrPollTimer) clearInterval(this.qrPollTimer);

    const fetchQr = () => {
      this.whatsappSvc.getWhatsAppQr(bizId).subscribe({
        next: (res) => {
          if (res.status === 'open') {
            this.qrStatus.set('open');
            this.stopQrPolling();
          } else if (res.base64) {
            // Evolution API may return 'data:image/png;base64,...' or just base64
            const src = res.base64.startsWith('data:') ? res.base64 : `data:image/png;base64,${res.base64}`;
            this.qrBase64.set(src);
            this.qrStatus.set('qr');
          } else {
            this.qrStatus.set('waiting');
          }
        },
        error: () => this.qrStatus.set('waiting')
      });
    };

    fetchQr();
    this.qrPollTimer = setInterval(fetchQr, 5000);
  }

  private stopQrPolling() {
    if (this.qrPollTimer) { clearInterval(this.qrPollTimer); this.qrPollTimer = null; }
  }

  confirmQr() {
    this.stopQrPolling();
    this.step.set(7);
  }

  finishOnboarding() {
    this.stopQrPolling();
    this.router.navigate(['/dashboard']);
  }
}
