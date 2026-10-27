import { HttpErrorResponse } from '@angular/common/http';

export interface FieldError {
  readonly field: string;
  readonly message: string;
}

export interface ApiError {
  readonly status: number;
  readonly code: string;
  readonly message: string;
  readonly fieldErrors: readonly FieldError[];
}

export const NETWORK_ERROR = 'NETWORK_ERROR';
export const INTERNAL_ERROR = 'INTERNAL_ERROR';

const NETWORK_MESSAGE = 'Não foi possível conectar ao servidor.';
const INTERNAL_MESSAGE = 'Ocorreu um erro inesperado. Tente novamente.';

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

export function isApiError(value: unknown): value is ApiError {
  return (
    isRecord(value) &&
    typeof value['status'] === 'number' &&
    typeof value['code'] === 'string' &&
    typeof value['message'] === 'string' &&
    Array.isArray(value['fieldErrors'])
  );
}

function parseFieldErrors(raw: unknown): FieldError[] {
  if (!Array.isArray(raw)) {
    return [];
  }
  const result: FieldError[] = [];
  for (const item of raw as unknown[]) {
    if (isRecord(item) && typeof item['field'] === 'string' && typeof item['message'] === 'string') {
      result.push({ field: item['field'], message: item['message'] });
    }
  }
  return result;
}

/** Converte a falha HTTP em `ApiError`. Nunca expõe `detail`, trace ou corpo bruto. */
export function toApiError(error: HttpErrorResponse): ApiError {
  if (error.status === 0) {
    return { status: 0, code: NETWORK_ERROR, message: NETWORK_MESSAGE, fieldErrors: [] };
  }
  const body: unknown = error.error;
  if (isRecord(body) && typeof body['code'] === 'string' && body['code'].length > 0) {
    const title = typeof body['title'] === 'string' && body['title'].length > 0 ? body['title'] : INTERNAL_MESSAGE;
    return {
      status: error.status,
      code: body['code'],
      message: title,
      fieldErrors: parseFieldErrors(body['errors']),
    };
  }
  return { status: error.status, code: INTERNAL_ERROR, message: INTERNAL_MESSAGE, fieldErrors: [] };
}
