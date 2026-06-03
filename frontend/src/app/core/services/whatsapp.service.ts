import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ChannelConfig {
  businessId: string;
  displayName: string;
  phoneNumber: string;
  phoneNumberId: string;
  wabaId: string;
  status: string;
  paymentMethods: string[];
  deliveryZones: string[];
}

export interface ChannelSaveRequest {
  displayName?: string;
  phoneNumber?: string;
  phoneNumberId?: string;
  wabaId?: string;
  paymentMethods?: string[];
  deliveryZones?: string[];
}

export interface QrResponse {
  base64: string | null;
  status: string;
}

export interface WhatsAppStatus {
  state: string; // 'open' | 'connecting' | 'close'
}

export interface BotIdentity {
  botName: string;
  botEmoji: string;
}

@Injectable({ providedIn: 'root' })
export class WhatsAppService {
  private readonly http = inject(HttpClient);

  getChannel(businessId: string): Observable<ChannelConfig> {
    return this.http.get<ChannelConfig>(`/api/v1/businesses/${businessId}/settings/whatsapp-channel`);
  }

  saveChannel(businessId: string, req: ChannelSaveRequest): Observable<ChannelConfig> {
    return this.http.put<ChannelConfig>(`/api/v1/businesses/${businessId}/settings/whatsapp-channel`, req);
  }

  confirmChannel(businessId: string): Observable<ChannelConfig> {
    return this.http.post<ChannelConfig>(`/api/v1/businesses/${businessId}/settings/whatsapp-channel/confirm`, {});
  }

  /** Gets a real WhatsApp QR code from Evolution API */
  getWhatsAppQr(businessId: string): Observable<QrResponse> {
    return this.http.get<QrResponse>(`/api/v1/businesses/${businessId}/settings/whatsapp-qr`);
  }

  /** Checks if the WhatsApp instance is connected */
  getWhatsAppStatus(businessId: string): Observable<WhatsAppStatus> {
    return this.http.get<WhatsAppStatus>(`/api/v1/businesses/${businessId}/settings/whatsapp-status`);
  }

  /** Deletes the WhatsApp instance (disconnect) */
  deleteWhatsAppInstance(businessId: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/businesses/${businessId}/settings/whatsapp-instance`);
  }

  /** Gets bot identity (mascot name + emoji) */
  getBotIdentity(businessId: string): Observable<BotIdentity> {
    return this.http.get<BotIdentity>(`/api/v1/businesses/${businessId}/settings/bot-identity`);
  }

  /** Saves bot identity */
  saveBotIdentity(businessId: string, identity: BotIdentity): Observable<BotIdentity> {
    return this.http.put<BotIdentity>(`/api/v1/businesses/${businessId}/settings/bot-identity`, identity);
  }
}
