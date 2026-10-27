import { InjectionToken } from '@angular/core';

/** Prefixo da API; os caminhos do contrato OpenAPI são anexados a ele (ex.: `${base}/auth/login`). */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  factory: () => '/api/v1',
});
