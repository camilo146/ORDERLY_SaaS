import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { PlanCatalogItem, SubscriptionStatus } from '../models/orderly.models';

@Injectable({ providedIn: 'root' })
export class BillingService {
  private readonly http = inject(HttpClient);

  createCheckoutSession(businessId: string, planCode: string, cycle: 'monthly' | 'annual'): Promise<{ url: string }> {
    return firstValueFrom(
      this.http.post<{ url: string }>(
        `/api/v1/businesses/${businessId}/billing/checkout-session`,
        { planCode, cycle }
      )
    );
  }

  createPortalSession(businessId: string): Promise<{ url: string }> {
    return firstValueFrom(
      this.http.post<{ url: string }>(
        `/api/v1/businesses/${businessId}/billing/portal-session`,
        {}
      )
    );
  }

  getSubscription(businessId: string): Promise<SubscriptionStatus> {
    return firstValueFrom(
      this.http.get<SubscriptionStatus>(`/api/v1/businesses/${businessId}/subscription`)
    );
  }

  getPlans(): Promise<PlanCatalogItem[]> {
    return firstValueFrom(
      this.http.get<PlanCatalogItem[]>('/api/v1/plans')
    );
  }

  cancelSubscription(businessId: string): Promise<void> {
    return firstValueFrom(
      this.http.post<void>(`/api/v1/businesses/${businessId}/subscription/cancel`, {})
    );
  }
}
