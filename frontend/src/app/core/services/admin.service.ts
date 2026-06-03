import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import {
  GlobalDashboardMetrics,
  BusinessAdminDetail,
  AuditLogEntry,
  EmailLogEntry,
  ImpersonationResponse,
  PlanAdminInfo,
  PlanCatalogItem,
  SubscriptionStatus
} from '../models/orderly.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/ceo';

  // ── Dashboard ─────────────────────────────────────────────────────────────

  getDashboard(): Promise<GlobalDashboardMetrics> {
    return firstValueFrom(this.http.get<GlobalDashboardMetrics>(`${this.base}/dashboard`));
  }

  getPlans(): Promise<PlanAdminInfo[]> {
    return firstValueFrom(this.http.get<PlanAdminInfo[]>(`${this.base}/plans`));
  }

  // ── Businesses ────────────────────────────────────────────────────────────

  listBusinesses(): Promise<BusinessAdminDetail[]> {
    return firstValueFrom(this.http.get<BusinessAdminDetail[]>(`${this.base}/businesses`));
  }

  getBusiness(id: string): Promise<BusinessAdminDetail> {
    return firstValueFrom(this.http.get<BusinessAdminDetail>(`${this.base}/businesses/${id}`));
  }

  suspend(id: string): Promise<BusinessAdminDetail> {
    return firstValueFrom(this.http.post<BusinessAdminDetail>(`${this.base}/businesses/${id}/suspend`, {}));
  }

  activate(id: string): Promise<BusinessAdminDetail> {
    return firstValueFrom(this.http.post<BusinessAdminDetail>(`${this.base}/businesses/${id}/activate`, {}));
  }

  delete(id: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`${this.base}/businesses/${id}`));
  }

  changePlan(id: string, planCode: string): Promise<BusinessAdminDetail> {
    return firstValueFrom(this.http.post<BusinessAdminDetail>(`${this.base}/businesses/${id}/change-plan`, { planCode }));
  }

  extendTrial(id: string, days: number): Promise<BusinessAdminDetail> {
    return firstValueFrom(this.http.post<BusinessAdminDetail>(`${this.base}/businesses/${id}/extend-trial`, { days }));
  }

  grantFreeMonths(id: string, months: number): Promise<BusinessAdminDetail> {
    return firstValueFrom(this.http.post<BusinessAdminDetail>(`${this.base}/businesses/${id}/free-months`, { months }));
  }

  resetUsage(id: string): Promise<BusinessAdminDetail> {
    return firstValueFrom(this.http.post<BusinessAdminDetail>(`${this.base}/businesses/${id}/reset-usage`, {}));
  }

  impersonate(id: string): Promise<ImpersonationResponse> {
    return firstValueFrom(this.http.post<ImpersonationResponse>(`${this.base}/businesses/${id}/impersonate`, {}));
  }

  forceLogout(id: string): Promise<void> {
    return firstValueFrom(this.http.post<void>(`${this.base}/businesses/${id}/force-logout`, {}));
  }

  // ── Emails ────────────────────────────────────────────────────────────────

  sendEmail(payload: { recipientEmail: string; recipientName?: string; subject: string; bodyText: string }): Promise<EmailLogEntry> {
    return firstValueFrom(this.http.post<EmailLogEntry>(`${this.base}/emails/send`, payload));
  }

  getEmailLogs(): Promise<EmailLogEntry[]> {
    return firstValueFrom(this.http.get<EmailLogEntry[]>(`${this.base}/emails/logs`));
  }

  // ── Audit logs ────────────────────────────────────────────────────────────

  getAuditLogs(limit = 50): Promise<AuditLogEntry[]> {
    return firstValueFrom(this.http.get<AuditLogEntry[]>(`${this.base}/audit-logs?limit=${limit}`));
  }

  // ── Plan catalogue (shared endpoint) ─────────────────────────────────────

  getCatalogPlans(): Promise<PlanCatalogItem[]> {
    return firstValueFrom(this.http.get<PlanCatalogItem[]>('/api/v1/plans'));
  }

  getBusinessSubscription(businessId: string): Promise<SubscriptionStatus> {
    return firstValueFrom(this.http.get<SubscriptionStatus>(`/api/v1/businesses/${businessId}/subscription`));
  }
}
