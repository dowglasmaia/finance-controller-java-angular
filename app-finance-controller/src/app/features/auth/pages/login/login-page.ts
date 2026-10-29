import { ChangeDetectionStrategy, Component, ElementRef, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { isApiError } from '../../../../core/http/api-error';
import { applyServerErrors, emailFormat, focusFirstInvalid, maxLength, requiredText } from '../../../../shared/forms/validators';
import { ButtonDirective } from '../../../../shared/ui/button';
import { FORM_FIELD_IMPORTS } from '../../../../shared/ui/form-field';
import { AuthService } from '../../data-access/auth.service';
import { safeReturnUrl } from '../../guards/auth.guard';

const INVALID_CREDENTIALS_MESSAGE = 'E-mail ou senha inválidos.';
const TOO_MANY_ATTEMPTS_MESSAGE = 'Muitas tentativas de entrada. Aguarde alguns minutos e tente novamente.';
const GENERIC_ERROR_MESSAGE = 'Não foi possível entrar agora. Tente novamente.';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink, ButtonDirective, ...FORM_FIELD_IMPORTS],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="flex min-h-screen items-center justify-center bg-surface px-4 py-8">
      <section class="w-full max-w-md rounded-card border border-line bg-surface-raised p-6 shadow-card sm:p-8">
        <h1 class="text-2xl font-semibold text-content">Entrar</h1>
        <p class="mt-1 text-sm text-content-muted">Acesse o Finance Controller com o seu e-mail e senha.</p>

        <form class="mt-6 flex flex-col gap-4" [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <app-form-field label="E-mail" [required]="true" [control]="form.controls.email">
            <input
              appFieldControl
              type="email"
              formControlName="email"
              autocomplete="username"
              inputmode="email"
            />
          </app-form-field>

          <app-form-field label="Senha" [required]="true" [control]="form.controls.password">
            <input
              appFieldControl
              type="password"
              formControlName="password"
              autocomplete="current-password"
            />
          </app-form-field>

          @if (formError(); as message) {
            <p role="alert" class="rounded-control border border-expense bg-expense/10 px-3 py-2 text-sm font-medium text-expense">
              {{ message }}
            </p>
          }

          <button
            type="submit"
            [disabled]="submitting()"
            appButton
          >
            {{ submitting() ? 'Entrando…' : 'Entrar' }}
          </button>
        </form>

        <div class="mt-6 flex flex-col gap-2 text-sm">
          <a routerLink="/esqueci-senha" class="font-medium text-accent underline underline-offset-2">Esqueci minha senha</a>
          <p class="text-content-muted">
            Ainda não tem conta?
            <a routerLink="/cadastrar" class="font-medium text-accent underline">Cadastre-se</a>
          </p>
        </div>
      </section>
    </main>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /** Query param `returnUrl` (ligado por `withComponentInputBinding`). */
  readonly returnUrl = input<string | undefined>(undefined);

  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);

  protected readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [requiredText, emailFormat, maxLength(254)] }),
    password: new FormControl('', { nonNullable: true, validators: [requiredText, maxLength(128)] }),
  });

  protected submit(): void {
    if (this.submitting()) {
      return;
    }
    this.formError.set(null);
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      focusFirstInvalid(this.host.nativeElement);
      return;
    }
    this.submitting.set(true);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email.trim(), password).subscribe({
      next: () => {
        this.submitting.set(false);
        void this.router.navigateByUrl(safeReturnUrl(this.returnUrl()));
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.handleError(error);
      },
    });
  }

  private handleError(error: unknown): void {
    if (!isApiError(error)) {
      this.formError.set(GENERIC_ERROR_MESSAGE);
      return;
    }
    switch (error.code) {
      case 'INVALID_CREDENTIALS':
        this.formError.set(INVALID_CREDENTIALS_MESSAGE);
        break;
      case 'TOO_MANY_ATTEMPTS':
        this.formError.set(TOO_MANY_ATTEMPTS_MESSAGE);
        break;
      case 'VALIDATION_FAILED': {
        const unmatched = applyServerErrors(this.form, error.fieldErrors);
        this.formError.set(unmatched[0]?.message ?? null);
        setTimeout(() => focusFirstInvalid(this.host.nativeElement));
        break;
      }
      default:
        this.formError.set(error.message || GENERIC_ERROR_MESSAGE);
    }
  }
}
