import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { ApiError } from '../../../core/http/api-error';
import { AuthService } from '../data-access/auth.service';
import { LoginPage } from './login/login-page';
import { RegisterPage } from './register/register-page';

interface Credentials {
  email: string;
  password: string;
}

class FakeAuthService {
  readonly calls: Credentials[] = [];
  result: Observable<unknown> = of({});

  login(email: string, password: string): Observable<unknown> {
    this.calls.push({ email, password });
    return this.result;
  }

  register(email: string, password: string): Observable<unknown> {
    this.calls.push({ email, password });
    return this.result;
  }
}

function apiError(code: string, status: number, fieldErrors: ApiError['fieldErrors'] = []): ApiError {
  return { status, code, message: 'mensagem', fieldErrors };
}

function setup() {
  const auth = new FakeAuthService();
  TestBed.configureTestingModule({
    providers: [provideZonelessChangeDetection(), provideRouter([]), { provide: AuthService, useValue: auth }],
  });
  const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  return { auth, navigate };
}

function fill(root: HTMLElement, values: Record<string, string>): void {
  for (const [name, value] of Object.entries(values)) {
    const input = root.querySelector<HTMLInputElement>(`input[formcontrolname="${name}"]`);
    if (!input) {
      throw new Error(`campo ${name} não encontrado`);
    }
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }
}

function submit(root: HTMLElement): void {
  root.querySelector<HTMLFormElement>('form')?.dispatchEvent(new Event('submit'));
}

describe('LoginPage', () => {
  it('não chama a API com campos vazios e mostra as mensagens', async () => {
    const { auth } = setup();
    const fixture = TestBed.createComponent(LoginPage);
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    submit(root);
    await fixture.whenStable();

    expect(auth.calls).toHaveLength(0);
    expect(root.textContent).toContain('Campo obrigatório');
    expect(root.querySelector('input[aria-invalid="true"]')).not.toBeNull();
  });

  it('entra e navega para a rota pedida (apenas caminho interno)', async () => {
    const { auth, navigate } = setup();
    const fixture = TestBed.createComponent(LoginPage);
    fixture.componentRef.setInput('returnUrl', '/lancamentos');
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    fill(root, { email: ' Ana@Example.com ', password: 'SenhaForte#2026' });
    submit(root);
    await fixture.whenStable();

    expect(auth.calls).toEqual([{ email: 'Ana@Example.com', password: 'SenhaForte#2026' }]);
    expect(navigate).toHaveBeenCalledWith('/lancamentos');
  });

  it('ignora returnUrl externo', async () => {
    const { navigate } = setup();
    const fixture = TestBed.createComponent(LoginPage);
    fixture.componentRef.setInput('returnUrl', '//evil.example');
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    fill(root, { email: 'a@b.co', password: 'x' });
    submit(root);
    await fixture.whenStable();

    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('mostra mensagem genérica em INVALID_CREDENTIALS e permanece na página', async () => {
    const { auth, navigate } = setup();
    auth.result = throwError(() => apiError('INVALID_CREDENTIALS', 401));
    const fixture = TestBed.createComponent(LoginPage);
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    fill(root, { email: 'a@b.co', password: 'errada' });
    submit(root);
    await fixture.whenStable();

    expect(root.querySelector('[role="alert"]')?.textContent).toContain('E-mail ou senha inválidos.');
    expect(navigate).not.toHaveBeenCalled();
    expect(root.querySelector<HTMLButtonElement>('button[type="submit"]')?.disabled).toBe(false);
  });

  it('avisa a espera em TOO_MANY_ATTEMPTS', async () => {
    const { auth } = setup();
    auth.result = throwError(() => apiError('TOO_MANY_ATTEMPTS', 429));
    const fixture = TestBed.createComponent(LoginPage);
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    fill(root, { email: 'a@b.co', password: 'x' });
    submit(root);
    await fixture.whenStable();

    expect(root.querySelector('[role="alert"]')?.textContent).toContain('Aguarde');
  });
});

describe('RegisterPage', () => {
  it('bloqueia senha curta e confirmação diferente sem chamar a API', async () => {
    const { auth } = setup();
    const fixture = TestBed.createComponent(RegisterPage);
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    fill(root, { email: 'a@b.co', password: 'curta', confirmPassword: 'outra' });
    submit(root);
    await fixture.whenStable();

    expect(auth.calls).toHaveLength(0);
    expect(root.textContent).toContain('A senha deve ter pelo menos 8 caracteres');
    expect(root.textContent).toContain('As senhas não coincidem');
  });

  it('cadastra e navega para o dashboard', async () => {
    const { auth, navigate } = setup();
    const fixture = TestBed.createComponent(RegisterPage);
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    fill(root, { email: 'a@b.co', password: 'SenhaForte#2026', confirmPassword: 'SenhaForte#2026' });
    submit(root);
    await fixture.whenStable();

    expect(auth.calls).toEqual([{ email: 'a@b.co', password: 'SenhaForte#2026' }]);
    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('mostra erros do servidor no campo e preserva os dados digitados', async () => {
    const { auth } = setup();
    auth.result = throwError(() =>
      apiError('VALIDATION_FAILED', 400, [{ field: 'email', message: 'E-mail inválido no servidor' }]),
    );
    const fixture = TestBed.createComponent(RegisterPage);
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    fill(root, { email: 'a@b.co', password: 'SenhaForte#2026', confirmPassword: 'SenhaForte#2026' });
    submit(root);
    await fixture.whenStable();

    expect(root.textContent).toContain('E-mail inválido no servidor');
    expect(root.querySelector<HTMLInputElement>('input[formcontrolname="email"]')?.value).toBe('a@b.co');
  });

  it('trata EMAIL_ALREADY_REGISTERED sem revelar detalhes', async () => {
    const { auth } = setup();
    auth.result = throwError(() => apiError('EMAIL_ALREADY_REGISTERED', 409));
    const fixture = TestBed.createComponent(RegisterPage);
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    fill(root, { email: 'a@b.co', password: 'SenhaForte#2026', confirmPassword: 'SenhaForte#2026' });
    submit(root);
    await fixture.whenStable();

    expect(root.querySelector('[role="alert"]')?.textContent).toContain('Este e-mail não pode ser usado');
  });
});
