import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import {
  Business,
  BusinessDashboardResponse,
  CreateBusinessPayload,
  SystemOverviewResponse
} from '../models/orderly.models';

@Injectable({ providedIn: 'root' })
export class BusinessService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1';

  getSystemOverview(): Promise<SystemOverviewResponse> {
    return firstValueFrom(this.http.get<SystemOverviewResponse>(`${this.base}/admin/overview`));
  }

  listAll(): Promise<Business[]> {
    return firstValueFrom(this.http.get<Business[]>(`${this.base}/admin/businesses`));
  }

  create(payload: CreateBusinessPayload): Promise<Business> {
    return firstValueFrom(this.http.post<Business>(`${this.base}/businesses`, payload));
  }

  getById(businessId: string): Promise<Business> {
    return firstValueFrom(this.http.get<Business>(`${this.base}/businesses/${businessId}`));
  }

  getDashboard(businessId: string): Promise<BusinessDashboardResponse> {
    return firstValueFrom(
      this.http.get<BusinessDashboardResponse>(`${this.base}/businesses/${businessId}/dashboard`)
    );
  }
}
