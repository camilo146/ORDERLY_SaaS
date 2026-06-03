import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  AuthResponse,
  Business,
  BusinessDashboardResponse,
  CreateBusinessPayload,
  CreateOrderPayload,
  CreateProductPayload,
  DemoSeedResult,
  OperatorOverviewResponse,
  OrderResponse,
  ProductResponse,
  SystemOverviewResponse
} from '../models/orderly.models';

@Injectable({
  providedIn: 'root'
})
export class OrderlyApiService {
  readonly demoPassword = 'Orderly123!';

  private readonly apiBase = '/api/v1';
  private readonly tokenKey = 'orderly.front.token';
  private readonly activeBusinessKey = 'orderly.front.business';

  constructor(private readonly http: HttpClient) {}

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  setSession(response: AuthResponse): void {
    localStorage.setItem(this.tokenKey, response.accessToken);
    if (!this.getActiveBusiness() && response.businesses?.length) {
      this.setActiveBusiness(response.businesses[0].id);
    }
  }

  clearSession(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.activeBusinessKey);
  }

  setActiveBusiness(businessId: string): void {
    localStorage.setItem(this.activeBusinessKey, businessId);
  }

  getActiveBusiness(): string | null {
    return localStorage.getItem(this.activeBusinessKey);
  }

  login(email: string, password: string): Promise<AuthResponse> {
    return firstValueFrom(this.http.post<AuthResponse>(`${this.apiBase}/auth/login`, { email, password }));
  }

  me(): Promise<AuthResponse> {
    return firstValueFrom(this.http.get<AuthResponse>(`${this.apiBase}/me`, this.buildOptions()));
  }

  getAdminOverview(): Promise<SystemOverviewResponse> {
    return firstValueFrom(this.http.get<SystemOverviewResponse>(`${this.apiBase}/admin/overview`, this.buildOptions()));
  }

  listAdminBusinesses(): Promise<Business[]> {
    return firstValueFrom(this.http.get<Business[]>(`${this.apiBase}/admin/businesses`, this.buildOptions()));
  }

  getOperatorOverview(): Promise<OperatorOverviewResponse> {
    return firstValueFrom(this.http.get<OperatorOverviewResponse>(`${this.apiBase}/operator/overview`, this.buildOptions()));
  }

  createBusiness(payload: CreateBusinessPayload): Promise<Business> {
    return firstValueFrom(this.http.post<Business>(`${this.apiBase}/businesses`, payload, this.buildOptions()));
  }

  getBusinessDashboard(businessId: string): Promise<BusinessDashboardResponse> {
    return firstValueFrom(
      this.http.get<BusinessDashboardResponse>(`${this.apiBase}/businesses/${businessId}/dashboard`, this.buildOptions(businessId))
    );
  }

  listProducts(businessId: string): Promise<ProductResponse[]> {
    return firstValueFrom(
      this.http.get<ProductResponse[]>(`${this.apiBase}/businesses/${businessId}/products`, this.buildOptions(businessId))
    );
  }

  createProduct(businessId: string, payload: CreateProductPayload): Promise<ProductResponse> {
    return firstValueFrom(
      this.http.post<ProductResponse>(`${this.apiBase}/businesses/${businessId}/products`, payload, this.buildOptions(businessId))
    );
  }

  listOrders(businessId: string): Promise<OrderResponse[]> {
    return firstValueFrom(
      this.http.get<OrderResponse[]>(`${this.apiBase}/businesses/${businessId}/orders`, this.buildOptions(businessId))
    );
  }

  createOrder(businessId: string, payload: CreateOrderPayload): Promise<OrderResponse> {
    return firstValueFrom(
      this.http.post<OrderResponse>(`${this.apiBase}/businesses/${businessId}/orders`, payload, this.buildOptions(businessId))
    );
  }

  updateOrderStatus(businessId: string, orderId: string, status: string): Promise<OrderResponse> {
    return firstValueFrom(
      this.http.patch<OrderResponse>(
        `${this.apiBase}/businesses/${businessId}/orders/${orderId}/status`,
        { status },
        this.buildOptions(businessId)
      )
    );
  }

  async ensureDemoBusinessData(existingBusinesses: Business[]): Promise<DemoSeedResult> {
    const businesses = [...existingBusinesses];
    let activeBusinessId = this.getActiveBusiness() ?? businesses[0]?.id ?? null;

    if (!businesses.length) {
      const createdBusiness = await this.createBusiness({
        name: 'Restaurante El Sabor',
        businessType: 'restaurant',
        countryCode: 'CO',
        currencyCode: 'COP',
        timezone: 'America/Bogota'
      });
      businesses.push(createdBusiness);
      activeBusinessId = createdBusiness.id;
      this.setActiveBusiness(createdBusiness.id);
    }

    if (!activeBusinessId) {
      return { businesses, activeBusinessId: null };
    }

    let products = await this.listProducts(activeBusinessId);
    if (!products.length) {
      const seeds: CreateProductPayload[] = [
        { name: 'Hamburguesa especial', description: 'Carne artesanal con papas', price: 22000 },
        { name: 'Pechuga a la plancha', description: 'Acompañada de ensalada fresca', price: 18000 },
        { name: 'Pizza especial', description: 'Masa delgada y queso extra', price: 32000 },
        { name: 'Limonada natural', description: 'Bebida refrescante', price: 7000 }
      ];

      for (const seed of seeds) {
        await this.createProduct(activeBusinessId, seed);
      }

      products = await this.listProducts(activeBusinessId);
    }

    const orders = await this.listOrders(activeBusinessId);
    if (!orders.length && products.length) {
      const createdPending = await this.createOrder(activeBusinessId, {
        customerName: 'Valentina Ruiz',
        customerWhatsapp: '+573128459021',
        deliveryType: 'DELIVERY',
        notes: 'Sin cebolla por favor.',
        items: [
          { productId: products[1]?.id ?? products[0].id, quantity: 2, notes: 'Punto medio' },
          { productId: products[3]?.id ?? products[0].id, quantity: 1, notes: 'Poco azúcar' }
        ]
      });

      const createdInProgress = await this.createOrder(activeBusinessId, {
        customerName: 'Andrés Torres',
        customerWhatsapp: '+573112345678',
        deliveryType: 'DELIVERY',
        notes: 'Cobrar por Nequi.',
        items: [
          { productId: products[0].id, quantity: 2 },
          { productId: products[3]?.id ?? products[0].id, quantity: 2 }
        ]
      });

      const createdReady = await this.createOrder(activeBusinessId, {
        customerName: 'Carlos Medina',
        customerWhatsapp: '+573201234567',
        deliveryType: 'PICKUP',
        notes: 'Recoge en 20 minutos.',
        items: [{ productId: products[2]?.id ?? products[0].id, quantity: 1 }]
      });

      const createdDelivered = await this.createOrder(activeBusinessId, {
        customerName: 'Diana Castillo',
        customerWhatsapp: '+573163216549',
        deliveryType: 'DELIVERY',
        notes: 'Entregar en portería.',
        items: [{ productId: products[0].id, quantity: 1 }]
      });

      await this.updateOrderStatus(activeBusinessId, createdPending.id, 'PENDING');
      await this.updateOrderStatus(activeBusinessId, createdInProgress.id, 'IN_PROGRESS');
      await this.updateOrderStatus(activeBusinessId, createdReady.id, 'READY');
      await this.updateOrderStatus(activeBusinessId, createdDelivered.id, 'DELIVERED');
    }

    return { businesses, activeBusinessId };
  }

  private buildOptions(businessId?: string): { headers: HttpHeaders } {
    let headers = new HttpHeaders();
    const token = this.getToken();

    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    if (businessId) {
      headers = headers.set('X-Business-Id', businessId);
    }

    return { headers };
  }
}
