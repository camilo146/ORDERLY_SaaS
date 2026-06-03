import { Component, OnInit, OnDestroy, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '../../core/services/auth.service';
import { WhatsAppService, ChannelConfig, BotIdentity } from '../../core/services/whatsapp.service';
import { TemplateService, TemplateDto } from '../../core/services/template.service';
import { DaySchedule, PlanCatalogItem, SubscriptionStatus } from '../../core/models/orderly.models';

type Tab = 'whatsapp' | 'messages' | 'business' | 'hours' | 'bot' | 'plan';

const PAYMENT_OPTIONS = [
  'Efectivo', 'Nequi', 'Daviplata', 'Transferencia bancaria', 'Tarjeta crédito/débito',
];

const DAYS = ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'];
const DAY_LABELS: Record<string, string> = {
  MONDAY:'Lunes', TUESDAY:'Martes', WEDNESDAY:'Miércoles',
  THURSDAY:'Jueves', FRIDAY:'Viernes', SATURDAY:'Sábado', SUNDAY:'Domingo'
};

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.css'
})
export class SettingsComponent implements OnInit, OnDestroy {
  private readonly auth = inject(AuthService);
  private readonly whatsappSvc = inject(WhatsAppService);
  private readonly templateSvc = inject(TemplateService);
  private readonly http = inject(HttpClient);

  activeTab = signal<Tab>('whatsapp');
  loading = signal(false);
  saved = signal(false);
  error = signal('');

  // ── Subscription / Plan tab ───────────────────────────────────────────────
  subscription = signal<SubscriptionStatus | null>(null);
  allPlans = signal<PlanCatalogItem[]>([]);
  subLoading = signal(false);
  changingPlan = signal(false);
  selectedNewPlan = signal('');

  readonly paymentOptions = PAYMENT_OPTIONS;

  // WhatsApp form
  displayName = '';
  phoneNumber = '';
  channelStatus = signal('NOT_CONNECTED');
  paymentMethods = signal<string[]>([]);
  paymentMethodStates = signal<{name: string, details: string, checked: boolean}[]>(
    this.paymentOptions.map(b => ({name: b, details: '', checked: false}))
  );
  isLoadingTemplates = signal(true);

  // Evolution API QR
  qrBase64 = signal<string | null>(null);
  qrStatus = signal<'idle' | 'loading' | 'connecting' | 'connected' | 'unavailable'>('idle');
  private qrPollInterval: ReturnType<typeof setInterval> | null = null;

  // Message templates
  templates = signal<TemplateDto[]>([]);
  editingTemplate: { eventType: string; body: string } | null = null;
  previewText = signal('');

  // Bot identity
  botName = '';
  botEmoji = '';
  isSavingBot = signal(false);
  botSaved = signal(false);

  // Business hours
  readonly dayLabels = DAY_LABELS;
  hours = signal<DaySchedule[]>(DAYS.map(d => ({ day: d, openTime: '08:00', closeTime: '20:00', closed: false })));
  isSavingHours = signal(false);
  hoursSaved = signal(false);

  private businessId = '';

  ngOnInit() {
    this.businessId = this.auth.getActiveBusinessId() ?? '';
    if (this.businessId) {
      this.loadChannel();
      this.loadTemplates();
      this.loadBotIdentity();
      this.loadHours();
    }
  }

  ngOnDestroy() {
    this.stopQrPolling();
  }

  setTab(tab: Tab) {
    this.activeTab.set(tab);
    this.saved.set(false);
    this.error.set('');
    if (tab === 'plan' && !this.subscription()) this.loadSubscription();
  }

  // ── WhatsApp ──────────────────────────────────────────────────────────────

  get whatsappQrUrl(): string {
    const num = this.phoneNumber || '573000000000';
    const link = `https://wa.me/${num}`;
    return `https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(link)}`;
  }

  private loadChannel() {
    this.whatsappSvc.getChannel(this.businessId).subscribe({
      next: (cfg) => {
        this.displayName  = cfg.displayName;
        this.phoneNumber  = cfg.phoneNumber ?? '';
        this.channelStatus.set(cfg.status);
        this.paymentMethods.set(cfg.paymentMethods ?? []);
        this.paymentMethodStates.set(
          this.paymentOptions.map(opt => {
            const backendVal = cfg.paymentMethods?.find((val: string) => val.startsWith(opt));
            if (backendVal) {
              const details = backendVal.includes(' - ') ? backendVal.substring(backendVal.indexOf(' - ') + 3) : '';
              return { name: opt, details, checked: true };
            }
            return { name: opt, details: '', checked: false };
          })
        );
      },
      error: () => {}
    });
  }

  saveChannel() {
    this.loading.set(true);
    this.error.set('');
    this.whatsappSvc.saveChannel(this.businessId, {
      displayName: this.displayName,
      phoneNumber: this.phoneNumber,
      paymentMethods: this.paymentMethodStates().filter(p => p.checked).map(p => p.details ? p.name + ' - ' + p.details : p.name),
    }).subscribe({
      next: (cfg) => {
        this.channelStatus.set(cfg.status);
        this.loading.set(false);
        this.saved.set(true);
        setTimeout(() => this.saved.set(false), 3000);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'Error al guardar la configuración.');
        this.loading.set(false);
      }
    });
  }

  confirmChannel() {
    this.loading.set(true);
    this.error.set('');
    this.whatsappSvc.confirmChannel(this.businessId).subscribe({
      next: (cfg) => {
        this.channelStatus.set(cfg.status);
        this.loading.set(false);
        this.saved.set(true);
        setTimeout(() => this.saved.set(false), 3000);
      },
      error: () => { this.loading.set(false); }
    });
  }

  onPaymentChecked(method: any) {
    if (!method.checked) method.details = '';
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      'NOT_CONNECTED': 'Sin conectar',
      'PENDING_VERIFICATION': 'Pendiente verificación',
      'ACTIVE': 'Activo',
      'SUSPENDED': 'Suspendido',
    };
    return map[status] ?? status;
  }

  // ── Evolution API QR ─────────────────────────────────────────────────────

  loadEvolutionQr() {
    if (!this.businessId) return;
    this.qrStatus.set('loading');
    this.qrBase64.set(null);
    this.whatsappSvc.getWhatsAppQr(this.businessId).subscribe({
      next: (res) => {
        if (res.status === 'CONNECTED') {
          this.qrStatus.set('connected');
          this.channelStatus.set('ACTIVE');
          this.stopQrPolling();
        } else if (res.status === 'UNAVAILABLE' || !res.base64) {
          this.qrStatus.set('unavailable');
        } else {
          this.qrBase64.set(res.base64);
          this.qrStatus.set('connecting');
          this.startQrPolling();
        }
      },
      error: () => this.qrStatus.set('unavailable'),
    });
  }

  private startQrPolling() {
    this.stopQrPolling();
    this.qrPollInterval = setInterval(() => {
      this.whatsappSvc.getWhatsAppStatus(this.businessId).subscribe({
        next: (res) => {
          if (res.state === 'open') {
            this.qrStatus.set('connected');
            this.channelStatus.set('ACTIVE');
            this.stopQrPolling();
          }
        },
        error: () => {},
      });
    }, 3000);
  }

  private stopQrPolling() {
    if (this.qrPollInterval !== null) {
      clearInterval(this.qrPollInterval);
      this.qrPollInterval = null;
    }
  }

  disconnectWhatsApp() {
    this.whatsappSvc.deleteWhatsAppInstance(this.businessId).subscribe({
      next: () => {
        this.qrStatus.set('idle');
        this.qrBase64.set(null);
        this.channelStatus.set('NOT_CONNECTED');
      },
      error: () => {},
    });
  }

  // ── Message Templates ────────────────────────────────────────────────────

  private loadTemplates() {
    this.templateSvc.getTemplates(this.businessId).subscribe({
      next: (list) => { this.templates.set(list); this.isLoadingTemplates.set(false); },
      error: () => { this.isLoadingTemplates.set(false); } // catchall
    });
  }

  startEdit(tpl: TemplateDto) {
    this.editingTemplate = { eventType: tpl.eventType, body: tpl.body };
    this.previewText.set('');
  }

  cancelEdit() {
    this.editingTemplate = null;
    this.previewText.set('');
  }

  saveTemplate() {
    if (!this.editingTemplate) return;
    this.loading.set(true);
    this.templateSvc.updateTemplate(
      this.businessId,
      this.editingTemplate.eventType,
      this.editingTemplate.body
    ).subscribe({
      next: (updated) => {
        this.templates.update(list =>
          list.map(t => t.eventType === updated.eventType ? updated : t)
        );
        this.editingTemplate = null;
        this.loading.set(false);
        this.saved.set(true);
        setTimeout(() => this.saved.set(false), 3000);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'Error al guardar plantilla.');
        this.loading.set(false);
      }
    });
  }

  previewTemplate() {
    if (!this.editingTemplate) return;
    const sample: Record<string, string> = {
      customer_name: 'Juan Pérez',
      business_name: this.displayName || 'Mi Negocio',
      order_number: 'ORD-001',
      total: '$45.000',
      eta_minutes: '30',
    };
    let preview = this.editingTemplate.body;
    for (const [k, v] of Object.entries(sample)) {
      preview = preview.replaceAll(`{{${k}}}`, v);
    }
    this.previewText.set(preview);
  }

  // ── Bot Identity ─────────────────────────────────────────────────────────

  private loadBotIdentity() {
    this.whatsappSvc.getBotIdentity(this.businessId).subscribe({
      next: (id) => {
        this.botName = id.botName ?? 'Orderly';
        this.botEmoji = id.botEmoji ?? '🤖';
      },
      error: () => {
        this.botName = 'Orderly';
        this.botEmoji = '🤖';
      }
    });
  }

  saveBotIdentity() {
    this.isSavingBot.set(true);
    this.error.set('');
    const name = this.botName.trim() || 'Orderly';
    const emoji = this.botEmoji.trim() || '🤖';
    this.whatsappSvc.saveBotIdentity(this.businessId, { botName: name, botEmoji: emoji }).subscribe({
      next: (id) => {
        this.botName = id.botName;
        this.botEmoji = id.botEmoji;
        this.isSavingBot.set(false);
        this.botSaved.set(true);
        setTimeout(() => this.botSaved.set(false), 3000);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'Error al guardar la identidad del bot.');
        this.isSavingBot.set(false);
      }
    });
  }

  // ── Business Hours ────────────────────────────────────────────────────────

  private async loadHours(): Promise<void> {
    try {
      const list = await firstValueFrom(
        this.http.get<DaySchedule[]>(`/api/v1/businesses/${this.businessId}/settings/hours`)
      );
      if (list && list.length > 0) {
        this.hours.set(list);
      }
    } catch { /* defaults kept */ }
  }

  async saveHours(): Promise<void> {
    this.isSavingHours.set(true);
    this.error.set('');
    try {
      await firstValueFrom(
        this.http.put<DaySchedule[]>(`/api/v1/businesses/${this.businessId}/settings/hours`, this.hours())
      );
      this.hoursSaved.set(true);
      setTimeout(() => this.hoursSaved.set(false), 3000);
    } catch (err: any) {
      this.error.set(err?.error?.message || 'Error al guardar horarios.');
    } finally {
      this.isSavingHours.set(false);
    }
  }

  updateHour(index: number, field: keyof DaySchedule, value: string | boolean): void {
    this.hours.update(list => {
      const copy = [...list];
      copy[index] = { ...copy[index], [field]: value };
      return copy;
    });
  }

  get botPreview(): string {
    const n = this.botName || 'Orderly';
    const e = this.botEmoji || '🤖';
    return `¡Hola! Soy *${n}* ${e}, el asistente virtual de tu negocio 😊`;
  }

  // ── Subscription / Plan ───────────────────────────────────────────────────

  private async loadSubscription(): Promise<void> {
    this.subLoading.set(true);
    try {
      const [sub, plans] = await Promise.all([
        firstValueFrom(this.http.get<SubscriptionStatus>(`/api/v1/businesses/${this.businessId}/subscription`)),
        firstValueFrom(this.http.get<PlanCatalogItem[]>('/api/v1/plans'))
      ]);
      this.subscription.set(sub);
      this.allPlans.set(plans);
      this.selectedNewPlan.set(sub.planCode ?? '');
    } catch { /* not subscribed yet */ }
    finally { this.subLoading.set(false); }
  }

  async changePlan(): Promise<void> {
    const code = this.selectedNewPlan();
    if (!code) return;
    this.changingPlan.set(true);
    try {
      const updated = await firstValueFrom(
        this.http.patch<SubscriptionStatus>(
          `/api/v1/businesses/${this.businessId}/subscription/plan`,
          { planCode: code }
        )
      );
      this.subscription.set(updated);
      this.saved.set(true);
      setTimeout(() => this.saved.set(false), 3000);
    } catch (err: any) {
      this.error.set(err?.error?.message || 'Error al cambiar el plan.');
    } finally {
      this.changingPlan.set(false);
    }
  }

  usagePercent(used: number, limit: number): number {
    if (!limit) return 0;
    return Math.min(100, Math.round((used / limit) * 100));
  }

  subStatusLabel(status: string): string {
    const m: Record<string, string> = {
      TRIAL: 'Trial activo', ACTIVE: 'Activo', PAST_DUE: 'Vencido',
      SUSPENDED: 'Suspendido', CANCELED: 'Cancelado'
    };
    return m[status] ?? status;
  }

  formatDate(d: string | null): string {
    if (!d) return '—';
    return new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium' }).format(new Date(d));
  }

  formatCurrency(n: number, currency = 'COP'): string {
    return new Intl.NumberFormat('es-CO', { style: 'currency', currency, maximumFractionDigits: 0 }).format(n);
  }
}
