import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TemplateDto {
  eventType: string;
  label: string;
  body: string;
}

export interface PreviewRequest {
  businessType: string;
  eventType: string;
  variables: Record<string, string>;
}

@Injectable({ providedIn: 'root' })
export class TemplateService {
  private readonly http = inject(HttpClient);

  /** Get all templates for a specific business */
  getTemplates(businessId: string): Observable<TemplateDto[]> {
    return this.http.get<TemplateDto[]>(`/api/v1/businesses/${businessId}/settings/message-templates`);
  }

  /** Update a single template body */
  updateTemplate(businessId: string, eventType: string, body: string): Observable<TemplateDto> {
    return this.http.put<TemplateDto>(
      `/api/v1/businesses/${businessId}/settings/message-templates/${eventType}`,
      { body }
    );
  }

  /** Preview a template with variables (uses existing endpoint) */
  preview(req: PreviewRequest): Observable<{ preview: string }> {
    return this.http.post<{ preview: string }>('/api/v1/message-templates/preview', req);
  }
}
