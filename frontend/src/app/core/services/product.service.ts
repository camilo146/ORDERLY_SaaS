import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { CreateProductPayload, ProductResponse } from '../models/orderly.models';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1';

  list(businessId: string): Promise<ProductResponse[]> {
    return firstValueFrom(
      this.http.get<ProductResponse[]>(`${this.base}/businesses/${businessId}/products`)
    );
  }

  create(businessId: string, payload: CreateProductPayload): Promise<ProductResponse> {
    return firstValueFrom(
      this.http.post<ProductResponse>(`${this.base}/businesses/${businessId}/products`, payload)
    );
  }

  uploadImage(businessId: string, productId: string, file: File): Promise<ProductResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return firstValueFrom(
      this.http.post<ProductResponse>(
        `${this.base}/businesses/${businessId}/products/${productId}/image`,
        formData
      )
    );
  }

  update(businessId: string, productId: string, payload: Partial<CreateProductPayload>): Promise<ProductResponse> {
    return firstValueFrom(
      this.http.put<ProductResponse>(`${this.base}/businesses/${businessId}/products/${productId}`, payload)
    );
  }

  delete(businessId: string, productId: string): Promise<void> {
    return firstValueFrom(
      this.http.delete<void>(`${this.base}/businesses/${businessId}/products/${productId}`)
    );
  }
}
