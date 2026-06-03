import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { AdminService } from '../../core/services/admin.service';
import { BusinessService } from '../../core/services/business.service';
import {
  GlobalDashboardMetrics, BusinessAdminDetail, AuditLogEntry,
  EmailLogEntry, PlanAdminInfo, ImpersonationResponse, CreateBusinessPayload,
  PlanCatalogItem
} from '../../core/models/orderly.models';

type Tab = 'dashboard' | 'businesses' | 'subscriptions' | 'emails' | 'audit';

@Component({
  selector: 'app-super-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './super-admin.component.html',
  styleUrl: './super-admin.component.css'
})
export class SuperAdminComponent implements OnInit {
  private readonly adminSvc = inject(AdminService);
  private readonly bizSvc   = inject(BusinessService);

  // ── Global state ──────────────────────────────────────────────────────────
  activeTab    = signal<Tab>('dashboard');
  loading      = signal(true);
  actionLoading = signal(false);
  error        = signal<string | null>(null);
  successMsg   = signal<string | null>(null);

  // ── Dashboard ─────────────────────────────────────────────────────────────
  metrics     = signal<GlobalDashboardMetrics | null>(null);
  plans       = signal<PlanAdminInfo[]>([]);
  catalogPlans = signal<PlanCatalogItem[]>([]);

  // ── Businesses ────────────────────────────────────────────────────────────
  businesses      = signal<BusinessAdminDetail[]>([]);
  searchQuery     = signal('');
  filterStatus    = signal('');
  showCancelled   = signal(false);
  selectedBiz     = signal<BusinessAdminDetail | null>(null);
  showBizPanel    = signal(false);
  showCreateForm  = signal(false);
  saving          = signal(false);

  filteredBusinesses = computed(() => {
    const q           = this.searchQuery().toLowerCase();
    const status      = this.filterStatus();
    const showCancel  = this.showCancelled();
    return this.businesses().filter(b => {
      if (!showCancel && b.status === 'CANCELLED') return false;
      const matchesQ = !q || b.name.toLowerCase().includes(q)
        || b.ownerEmail?.toLowerCase().includes(q)
        || b.slug?.toLowerCase().includes(q);
      const matchesS = !status || b.status === status;
      return matchesQ && matchesS;
    });
  });

  cancelledCount = computed(() => this.businesses().filter(b => b.status === 'CANCELLED').length);

  newBusiness: CreateBusinessPayload = this.emptyBusiness();

  // ── Action modals ─────────────────────────────────────────────────────────
  confirmAction  = signal<{ type: string; bizId: string; bizName: string } | null>(null);
  planModal      = signal<{ bizId: string; bizName: string } | null>(null);
  trialModal     = signal<{ bizId: string; bizName: string } | null>(null);
  monthsModal    = signal<{ bizId: string; bizName: string } | null>(null);
  impersonResult = signal<ImpersonationResponse | null>(null);

  selectedPlanCode = '';
  trialDays        = 14;
  freeMonths       = 1;

  // ── Emails ────────────────────────────────────────────────────────────────
  emailLogs    = signal<EmailLogEntry[]>([]);
  emailForm    = { recipientEmail: '', recipientName: '', subject: '', bodyText: '' };

  // ── Audit log ─────────────────────────────────────────────────────────────
  auditLogs = signal<AuditLogEntry[]>([]);

  readonly businessTypes = [
    { value: 'RESTAURANT',    label: 'Restaurante' },
    { value: 'PHARMACY',      label: 'Droguería' },
    { value: 'GENERAL_STORE', label: 'Tienda General' },
    { value: 'AUTO_PARTS',    label: 'Repuestos' },
    { value: 'FASHION',       label: 'Ropa y Moda' }
  ];

  readonly statusOptions = [
    { value: '',           label: 'Todos los estados' },
    { value: 'ACTIVE',     label: 'Activo' },
    { value: 'TRIALING',   label: 'Trial' },
    { value: 'SUSPENDED',  label: 'Suspendido' },
    { value: 'SETUP',      label: 'En setup' },
    { value: 'CANCELLED',  label: 'Cancelado' },
  ];

  ngOnInit(): void {
    this.loadDashboardData();
  }

  // ── Navigation ────────────────────────────────────────────────────────────

  setTab(tab: Tab): void {
    this.activeTab.set(tab);
    if (tab === 'businesses' && this.businesses().length === 0) this.loadBusinesses();
    if (tab === 'emails')    this.loadEmails();
    if (tab === 'audit')     this.loadAuditLogs();
  }

  // ── Data loading ──────────────────────────────────────────────────────────

  async loadDashboardData(): Promise<void> {
    this.loading.set(true);
    try {
      const [metrics, plans, catalogPlans] = await Promise.all([
        this.adminSvc.getDashboard(),
        this.adminSvc.getPlans(),
        this.adminSvc.getCatalogPlans()
      ]);
      this.metrics.set(metrics);
      this.plans.set(plans);
      this.catalogPlans.set(catalogPlans);
    } catch (e: any) {
      this.setError(e);
    } finally {
      this.loading.set(false);
    }
  }

  async loadBusinesses(): Promise<void> {
    this.loading.set(true);
    try {
      const list = await this.adminSvc.listBusinesses();
      this.businesses.set(list);
    } catch (e: any) {
      this.setError(e);
    } finally {
      this.loading.set(false);
    }
  }

  async loadEmails(): Promise<void> {
    try {
      const logs = await this.adminSvc.getEmailLogs();
      this.emailLogs.set(logs);
    } catch (e: any) {
      this.setError(e);
    }
  }

  async loadAuditLogs(): Promise<void> {
    this.loading.set(true);
    try {
      const logs = await this.adminSvc.getAuditLogs(100);
      this.auditLogs.set(logs);
    } catch (e: any) {
      this.setError(e);
    } finally {
      this.loading.set(false);
    }
  }

  async refreshBusiness(id: string): Promise<void> {
    try {
      const updated = await this.adminSvc.getBusiness(id);
      this.businesses.update(list => list.map(b => b.id === id ? updated : b));
      if (this.selectedBiz()?.id === id) this.selectedBiz.set(updated);
    } catch { /* silent */ }
  }

  // ── Business CRUD ─────────────────────────────────────────────────────────

  async createBusiness(): Promise<void> {
    this.saving.set(true);
    try {
      const created = await this.bizSvc.create(this.newBusiness);
      await this.loadBusinesses();
      this.newBusiness = this.emptyBusiness();
      this.showCreateForm.set(false);
      this.setSuccess('Negocio creado correctamente.');
    } catch (e: any) {
      this.setError(e);
    } finally {
      this.saving.set(false);
    }
  }

  openBizPanel(biz: BusinessAdminDetail): void {
    this.selectedBiz.set(biz);
    this.showBizPanel.set(true);
  }

  closeBizPanel(): void {
    this.showBizPanel.set(false);
    this.selectedBiz.set(null);
  }

  // ── Business actions ──────────────────────────────────────────────────────

  askConfirm(type: string, biz: BusinessAdminDetail): void {
    this.confirmAction.set({ type, bizId: biz.id, bizName: biz.name });
  }

  async confirmDo(): Promise<void> {
    const action = this.confirmAction();
    if (!action) return;
    this.confirmAction.set(null);
    this.actionLoading.set(true);
    try {
      switch (action.type) {
        case 'suspend':
          await this.adminSvc.suspend(action.bizId);
          this.setSuccess(`${action.bizName} suspendido.`);
          break;
        case 'activate':
          await this.adminSvc.activate(action.bizId);
          this.setSuccess(`${action.bizName} activado.`);
          break;
        case 'delete':
          await this.adminSvc.delete(action.bizId);
          this.businesses.update(list => list.filter(b => b.id !== action.bizId));
          this.closeBizPanel();
          this.setSuccess(`${action.bizName} eliminado.`);
          void this.loadDashboardData();
          return;
        case 'forceLogout':
          await this.adminSvc.forceLogout(action.bizId);
          this.setSuccess(`Sesiones de ${action.bizName} invalidadas.`);
          break;
        case 'resetUsage':
          await this.adminSvc.resetUsage(action.bizId);
          this.setSuccess(`Uso mensual de ${action.bizName} reiniciado.`);
          break;
      }
      await this.refreshBusiness(action.bizId);
    } catch (e: any) {
      this.setError(e);
    } finally {
      this.actionLoading.set(false);
    }
  }

  openPlanModal(biz: BusinessAdminDetail): void {
    this.selectedPlanCode = biz.plan?.code ?? '';
    this.planModal.set({ bizId: biz.id, bizName: biz.name });
  }

  async submitChangePlan(): Promise<void> {
    const modal = this.planModal();
    if (!modal || !this.selectedPlanCode) return;
    this.planModal.set(null);
    this.actionLoading.set(true);
    try {
      await this.adminSvc.changePlan(modal.bizId, this.selectedPlanCode);
      await this.refreshBusiness(modal.bizId);
      this.setSuccess(`Plan cambiado para ${modal.bizName}.`);
    } catch (e: any) {
      this.setError(e);
    } finally {
      this.actionLoading.set(false);
    }
  }

  openTrialModal(biz: BusinessAdminDetail): void {
    this.trialDays = 14;
    this.trialModal.set({ bizId: biz.id, bizName: biz.name });
  }

  async submitExtendTrial(): Promise<void> {
    const modal = this.trialModal();
    if (!modal) return;
    this.trialModal.set(null);
    this.actionLoading.set(true);
    try {
      await this.adminSvc.extendTrial(modal.bizId, this.trialDays);
      await this.refreshBusiness(modal.bizId);
      this.setSuccess(`Trial extendido ${this.trialDays} días para ${modal.bizName}.`);
    } catch (e: any) {
      this.setError(e);
    } finally {
      this.actionLoading.set(false);
    }
  }

  openMonthsModal(biz: BusinessAdminDetail): void {
    this.freeMonths = 1;
    this.monthsModal.set({ bizId: biz.id, bizName: biz.name });
  }

  async submitFreeMonths(): Promise<void> {
    const modal = this.monthsModal();
    if (!modal) return;
    this.monthsModal.set(null);
    this.actionLoading.set(true);
    try {
      await this.adminSvc.grantFreeMonths(modal.bizId, this.freeMonths);
      await this.refreshBusiness(modal.bizId);
      this.setSuccess(`${this.freeMonths} mes(es) gratis concedidos a ${modal.bizName}.`);
    } catch (e: any) {
      this.setError(e);
    } finally {
      this.actionLoading.set(false);
    }
  }

  async doImpersonate(biz: BusinessAdminDetail): Promise<void> {
    this.actionLoading.set(true);
    try {
      const result = await this.adminSvc.impersonate(biz.id);
      this.impersonResult.set(result);
    } catch (e: any) {
      this.setError(e);
    } finally {
      this.actionLoading.set(false);
    }
  }

  copyImpersonToken(): void {
    const t = this.impersonResult()?.token;
    if (t) {
      navigator.clipboard.writeText(t);
      this.setSuccess('Token copiado al portapapeles.');
    }
  }

  // ── Email actions ─────────────────────────────────────────────────────────

  async sendEmail(): Promise<void> {
    this.actionLoading.set(true);
    try {
      await this.adminSvc.sendEmail(this.emailForm);
      this.emailForm = { recipientEmail: '', recipientName: '', subject: '', bodyText: '' };
      await this.loadEmails();
      this.setSuccess('Email enviado correctamente.');
    } catch (e: any) {
      this.setError(e);
    } finally {
      this.actionLoading.set(false);
    }
  }

  // ── Formatting helpers ────────────────────────────────────────────────────

  formatCurrency(amount: number, currency = 'COP'): string {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency', currency,
      maximumFractionDigits: 0
    }).format(amount);
  }

  formatDate(dateStr: string | null | undefined): string {
    if (!dateStr) return '—';
    return new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(dateStr));
  }

  formatDateShort(dateStr: string | null | undefined): string {
    if (!dateStr) return '—';
    return new Intl.DateTimeFormat('es-CO', { dateStyle: 'short' }).format(new Date(dateStr));
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'Activo', TRIALING: 'Trial', SUSPENDED: 'Suspendido',
      SETUP: 'Setup', CANCELLED: 'Cancelado', PENDING_WHATSAPP: 'Pendiente WA'
    };
    return map[status] ?? status;
  }

  actionLabel(action: string): string {
    const map: Record<string, string> = {
      BUSINESS_SUSPENDED: 'Negocio suspendido',
      BUSINESS_ACTIVATED: 'Negocio activado',
      BUSINESS_DELETED: 'Negocio eliminado',
      PLAN_CHANGED: 'Plan cambiado',
      TRIAL_EXTENDED: 'Trial extendido',
      FREE_MONTHS_GRANTED: 'Meses gratis',
      USAGE_RESET: 'Uso reiniciado',
      BUSINESS_IMPERSONATED: 'Impersonación',
      FORCE_LOGOUT: 'Logout forzado',
      EMAIL_SENT: 'Email enviado',
      BUSINESS_CREATED: 'Negocio creado'
    };
    return map[action] ?? action;
  }

  usagePercent(usage: { ordersThisMonth: number; orderLimit: number }): number {
    if (!usage.orderLimit || usage.orderLimit === 0) return 0;
    return Math.min(100, Math.round((usage.ordersThisMonth / usage.orderLimit) * 100));
  }

  dismissError():   void { this.error.set(null); }
  dismissSuccess(): void { this.successMsg.set(null); }

  // ── Private helpers ───────────────────────────────────────────────────────

  private setError(e: any): void {
    this.error.set(e?.error?.message ?? e?.message ?? 'Error inesperado.');
  }

  private setSuccess(msg: string): void {
    this.successMsg.set(msg);
    setTimeout(() => this.successMsg.set(null), 4000);
  }

  private emptyBusiness(): CreateBusinessPayload {
    return { name: '', businessType: 'RESTAURANT', countryCode: 'CO', currencyCode: 'COP', timezone: 'America/Bogota' };
  }
}
