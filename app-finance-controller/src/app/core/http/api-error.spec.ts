import { HttpErrorResponse } from '@angular/common/http';
import { INTERNAL_ERROR, NETWORK_ERROR, isApiError, toApiError } from './api-error';

describe('toApiError', () => {
  it('converte problem+json em ApiError com fieldErrors e title como mensagem', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: {
        code: 'VALIDATION_FAILED',
        title: 'Dados inválidos',
        detail: 'detalhe interno sensível',
        traceId: 'abc',
        errors: [{ field: 'email', message: 'E-mail inválido' }, { field: 1 }],
      },
    });
    const result = toApiError(error);
    expect(result).toEqual({
      status: 400,
      code: 'VALIDATION_FAILED',
      message: 'Dados inválidos',
      fieldErrors: [{ field: 'email', message: 'E-mail inválido' }],
    });
    expect(JSON.stringify(result)).not.toContain('detalhe interno');
    expect(JSON.stringify(result)).not.toContain('abc');
  });

  it('mapeia status 0 para NETWORK_ERROR', () => {
    const result = toApiError(new HttpErrorResponse({ status: 0 }));
    expect(result.code).toBe(NETWORK_ERROR);
    expect(result.message).toBe('Não foi possível conectar ao servidor.');
  });

  it('usa INTERNAL_ERROR quando não há code e não vaza o corpo', () => {
    const result = toApiError(new HttpErrorResponse({ status: 500, error: '<html>stack trace</html>' }));
    expect(result.code).toBe(INTERNAL_ERROR);
    expect(result.message).not.toContain('stack');
    expect(result.fieldErrors).toEqual([]);
  });

  it('isApiError reconhece apenas o formato correto', () => {
    expect(isApiError(toApiError(new HttpErrorResponse({ status: 0 })))).toBe(true);
    expect(isApiError(new HttpErrorResponse({ status: 0 }))).toBe(false);
    expect(isApiError(null)).toBe(false);
  });
});
