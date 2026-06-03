import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import {
  CreateOrderPayload,
  OperatorOverviewResponse,
  OrderResponse
} from '../models/orderly.models';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1';

  getOperatorOverview(): Promise<OperatorOverviewResponse> {
    return firstValueFrom(this.http.get<OperatorOverviewResponse>(`${this.base}/operator/overview`));
  }

  list(businessId: string): Promise<OrderResponse[]> {
    return firstValueFrom(
      this.http.get<OrderResponse[]>(`${this.base}/businesses/${businessId}/orders`)
    );
  }

  create(businessId: string, payload: CreateOrderPayload): Promise<OrderResponse> {
    return firstValueFrom(
      this.http.post<OrderResponse>(`${this.base}/businesses/${businessId}/orders`, payload)
    );
  }

  updateStatus(businessId: string, orderId: string, status: string): Promise<OrderResponse> {
    return firstValueFrom(
      this.http.patch<OrderResponse>(
        `${this.base}/businesses/${businessId}/orders/${orderId}/status`,
        { status }
      )
    );
  }

  setPaymentProof(businessId: string, orderId: string, proofUrl: string): Promise<OrderResponse> {
    return firstValueFrom(
      this.http.patch<OrderResponse>(
        `${this.base}/businesses/${businessId}/orders/${orderId}/payment-proof`,
        { proofUrl }
      )
    );
  }

  cancelWithReason(businessId: string, orderId: string, reason: string): Promise<OrderResponse> {
    return firstValueFrom(
      this.http.post<OrderResponse>(
        `${this.base}/businesses/${businessId}/orders/${orderId}/cancel`,
        { reason }
      )
    );
  }

  getServiceStatus(businessId: string): Promise<{ enabled: boolean }> {
    return firstValueFrom(
      this.http.get<{ enabled: boolean }>(`${this.base}/businesses/${businessId}/settings/service-status`)
    );
  }

  toggleServiceStatus(businessId: string): Promise<{ enabled: boolean }> {
    return firstValueFrom(
      this.http.post<{ enabled: boolean }>(`${this.base}/businesses/${businessId}/settings/service-status/toggle`, {})
    );
  }
}
