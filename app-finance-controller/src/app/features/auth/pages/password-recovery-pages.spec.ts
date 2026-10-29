import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { apiErrorInterceptor } from '../../../core/http/api-error.interceptor';
import { ToastService } from '../../../shared/ui/toast.service';
import { ForgotPasswordPage, NEUTRAL_MESSAGE } from './forgot-password/forgot-password-page';
import { INVALID_LINK_MESSAGE, PASSWORD_CHANGED_MESSAGE, ResetPasswordPage } from './reset-password/reset-password-page';

const TOKEN = 'tok-de-teste-0123456789';
const STRONG = 'N0va-Senha-Segura-2026';

function setup(token: string | null = null) {
  localStorage.clear();
  sessionStorage.clear();
  window.history.replaceState(null, '', token === null ? '/redefinir-senha' : `/redefinir-senha?token=${token}`);
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([]),
      provideHttpClient(withInterceptors([apiErrorInterceptor])),
      provideHttpClientTesting(),
      {
        provide: ActivatedRoute,
        useValue: { snapshot: { queryParamMap: convertToParamMap(token === null ? {} : { token }) } },
      },
    ],
  });
  return {
    http: TestBed.inject(HttpTestingController),
    navigate: vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true),
    toast: TestBed.inject(ToastService),
  };
}

function fill(root: HTMLElement, values: Record<string, string>): void {
  for (const [name, value] of Object.entries(values)) {
    const input = root.querySelector<HTMLInputElement>(`input[formcontrolname="${name}"]`);
    if (!input) throw new Error(`campo ${name} não encontrado`);
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }
}

const submit = (root: HTMLElement) => root.querySelector<HTMLFormElement>('form')?.dispatchEvent(new Event('submit'));
const text = (root: HTMLElement) => root.textContent?.replace(/\s+/g, ' ') ?? '';

describe('ForgotPasswordPage', () => {
  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('não chama a API com e-mail vazio ou inválido e mostra o erro no campo', async () => {
    const { http } = setup();
    const fixture = TestBed.createComponent(ForgotPasswordPage);
    const root = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();

    submit(root);
    await fixture.whenStable();
    expect(text(root)).toContain('Campo obrigatório');

    fill(root, { email: 'nao-e-email' });
    submit(root);
    await fixture.whenStable();
    expect(text(root)).toContain('Informe um e-mail válido');
    http.expectNone('/api/v1/auth/password-reset/request');
  });

  it('mostra sempre a mesma mensagem neutra, exista a conta ou não', async () => {
    const { http } = setup();
    const messages: string[] = [];
    for (const email of ['existe@example.com', 'nao-existe@example.com']) {
      const fixture = TestBed.createComponent(ForgotPasswordPage);
      const root = fixture.nativeElement as HTMLElement;
      await fixture.whenStable();
      fill(root, { email: `  ${email} ` });
      submit(root);
      await fixture.whenStable();
      const request = http.expectOne({ method: 'POST', url: '/api/v1/auth/password-reset/request' });
      expect(request.request.body).toEqual({ email });
      expect(request.request.withCredentials).toBe(false);
      request.flush({ message: NEUTRAL_MESSAGE }, { status: 202, statusText: 'Accepted' });
      await fixture.whenStable();
      messages.push(root.querySelector('[role="status"]')?.textContent?.trim() ?? '');
    }

    expect(messages[0]).toBe(NEUTRAL_MESSAGE);
    expect(messages[1]).toBe(messages[0]);
  });

  it('o limite de pedidos (429) mostra a mesma mensagem; erro de servidor pede para tentar de novo', async () => {
    const { http } = setup();
    const limited = TestBed.createComponent(ForgotPasswordPage);
    const limitedRoot = limited.nativeElement as HTMLElement;
    await limited.whenStable();
    fill(limitedRoot, { email: 'a@example.com' });
    submit(limitedRoot);
    await limited.whenStable();
    http.expectOne('/api/v1/auth/password-reset/request').flush(
      { status: 429, code: 'TOO_MANY_ATTEMPTS', title: 'x', detail: 'x' },
      { status: 429, statusText: 'Too Many Requests' },
    );
    await limited.whenStable();
    expect(limitedRoot.querySelector('[role="status"]')?.textContent?.trim()).toBe(NEUTRAL_MESSAGE);

    const broken = TestBed.createComponent(ForgotPasswordPage);
    const brokenRoot = broken.nativeElement as HTMLElement;
    await broken.whenStable();
    fill(brokenRoot, { email: 'a@example.com' });
    submit(brokenRoot);
    await broken.whenStable();
    http.expectOne('/api/v1/auth/password-reset/request').flush({}, { status: 500, statusText: 'x' });
    await broken.whenStable();
    expect(brokenRoot.querySelector('[role="alert"]')?.textContent).toContain('Não foi possível enviar o pedido agora');
    expect(brokenRoot.querySelector('[role="status"]')).toBeNull();
  });

  it('desabilita o botão durante o envio', async () => {
    const { http } = setup();
    const fixture = TestBed.createComponent(ForgotPasswordPage);
    const root = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
    fill(root, { email: 'a@example.com' });
    submit(root);
    await fixture.whenStable();

    expect(root.querySelector<HTMLButtonElement>('button[type="submit"]')?.disabled).toBe(true);
    http.expectOne('/api/v1/auth/password-reset/request').flush({ message: NEUTRAL_MESSAGE }, { status: 202, statusText: 'Accepted' });
  });
});

describe('ResetPasswordPage', () => {
  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('lê o token e o remove da barra de endereço na hora; o formulário continua utilizável', async () => {
    setup(TOKEN);
    const fixture = TestBed.createComponent(ResetPasswordPage);
    const root = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();

    expect(window.location.search).toBe('');
    expect(window.location.href).not.toContain(TOKEN);
    expect(root.querySelector('form')).not.toBeNull();
    expect(text(root)).not.toContain(TOKEN);
  });

  it('sem token mostra o link inválido com atalho para pedir outro e nenhum formulário', async () => {
    setup(null);
    const fixture = TestBed.createComponent(ResetPasswordPage);
    const root = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();

    expect(text(root)).toContain(INVALID_LINK_MESSAGE);
    expect(root.querySelector('form')).toBeNull();
    expect(root.querySelector('a[href="/esqueci-senha"]')).not.toBeNull();
  });

  it('senhas diferentes impedem o envio com a mensagem no campo; senha curta também', async () => {
    const { http } = setup(TOKEN);
    const fixture = TestBed.createComponent(ResetPasswordPage);
    const root = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();

    fill(root, { password: STRONG, confirmPassword: 'outra-coisa-diferente' });
    submit(root);
    await fixture.whenStable();
    expect(text(root)).toContain('As senhas não coincidem');

    fill(root, { password: 'curta', confirmPassword: 'curta' });
    submit(root);
    await fixture.whenStable();
    expect(text(root)).toContain('A senha deve ter pelo menos 8 caracteres');
    http.expectNone('/api/v1/auth/password-reset/confirm');
  });

  it('redefine: manda token e senha só no corpo, avisa e vai para /entrar sem guardar nada no navegador', async () => {
    const { http, navigate, toast } = setup(TOKEN);
    const fixture = TestBed.createComponent(ResetPasswordPage);
    const root = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();

    fill(root, { password: STRONG, confirmPassword: STRONG });
    submit(root);
    await fixture.whenStable();

    const request = http.expectOne({ method: 'POST', url: '/api/v1/auth/password-reset/confirm' });
    expect(request.request.body).toEqual({ token: TOKEN, newPassword: STRONG });
    expect(request.request.url).not.toContain(TOKEN);
    expect(request.request.headers.keys().join(',').toLowerCase()).not.toContain('authorization');
    request.flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    expect(navigate).toHaveBeenCalledWith('/entrar');
    expect(toast.toasts().map((t) => t.message)).toContain(PASSWORD_CHANGED_MESSAGE);
    for (const store of [localStorage, sessionStorage]) {
      const dump = JSON.stringify({ ...store });
      expect(dump).not.toContain(TOKEN);
      expect(dump).not.toContain(STRONG);
    }
  });

  it('token inválido ou expirado mostra a mensagem de link inválido com atalho para pedir outro', async () => {
    const { http } = setup(TOKEN);
    const fixture = TestBed.createComponent(ResetPasswordPage);
    const root = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
    fill(root, { password: STRONG, confirmPassword: STRONG });
    submit(root);
    await fixture.whenStable();

    http.expectOne('/api/v1/auth/password-reset/confirm').flush(
      { status: 400, code: 'PASSWORD_RESET_TOKEN_INVALID', title: 'x', detail: 'x' },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();

    expect(text(root)).toContain(INVALID_LINK_MESSAGE);
    expect(root.querySelector('form')).toBeNull();
    expect(root.querySelector('a[href="/esqueci-senha"]')).not.toBeNull();
  });

  it('erro de política da senha vindo do servidor aparece e o formulário continua', async () => {
    const { http } = setup(TOKEN);
    const fixture = TestBed.createComponent(ResetPasswordPage);
    const root = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
    fill(root, { password: '12345678', confirmPassword: '12345678' });
    submit(root);
    await fixture.whenStable();

    http.expectOne('/api/v1/auth/password-reset/confirm').flush(
      { status: 400, code: 'VALIDATION_FAILED', title: 'x', detail: 'x' },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();

    expect(root.querySelector('[role="alert"]')?.textContent).toContain('A senha não atende à política');
    expect(root.querySelector('form')).not.toBeNull();
  });
});
