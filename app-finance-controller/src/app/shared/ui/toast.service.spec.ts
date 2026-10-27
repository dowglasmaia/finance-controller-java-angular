import { TestBed } from '@angular/core/testing';
import { ToastService, TOAST_DURATION_MS } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    vi.useFakeTimers();
    service = TestBed.inject(ToastService);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('adiciona toasts por tipo', () => {
    service.success('ok');
    service.error('erro');
    service.warning('aviso');
    service.info('info');

    expect(service.toasts().map((toast) => toast.kind)).toEqual(['success', 'error', 'warning', 'info']);
    expect(service.toasts()[1]?.message).toBe('erro');
  });

  it('auto-dispensa após ~5 s', () => {
    service.success('ok');
    vi.advanceTimersByTime(TOAST_DURATION_MS - 1);
    expect(service.toasts()).toHaveLength(1);
    vi.advanceTimersByTime(1);
    expect(service.toasts()).toHaveLength(0);
  });

  it('dismiss remove só o toast indicado', () => {
    const first = service.info('um');
    service.info('dois');
    service.dismiss(first);

    expect(service.toasts().map((toast) => toast.message)).toEqual(['dois']);
  });
});
