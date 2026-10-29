import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { isApiError } from '../../../../core/http/api-error';
import {
  applyServerErrors,
  emailFormat,
  focusFirstInvalid,
  matchesField,
  maxLength,
  minLengthPassword,
  requiredText,
  revalidateOnChange,
} from '../../../../shared/forms/validators';
import { ButtonDirective } from '../../../../shared/ui/button';
import { FORM_FIELD_IMPORTS } from '../../../../shared/ui/form-field';
import { AuthService } from '../../data-access/auth.service';

const EMAIL_TAKEN_MESSAGE = 'Este e-mail não pode ser usado. Entre ou use outro e-mail.';
const TOO_MANY_ATTEMPTS_MESSAGE = 'Muitas tentativas. Aguarde alguns minutos e tente novamente.';
const GENERIC_ERROR_MESSAGE = 'Não foi possível concluir o cadastro agora. Tente novamente.';

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink, ButtonDirective, ...FORM_FIELD_IMPORTS],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="flex min-h-screen items-center justify-center bg-surface px-4 py-8">
      <section class="w-full max-w-md rounded-card border border-line bg-surface-raised p-6 shadow-card sm:p-8">
        <h1 class="text-2xl font-semibold text-content">Cadastrar</h1>
        <p class="mt-1 text-sm text-content-muted">Crie a sua conta para controlar as suas finanças.</p>

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

          <app-form-field
            label="Senha"
            help="Mínimo de 8 caracteres."
            [required]="true"
            [control]="form.controls.password"
          >
            <input
              appFieldControl
              type="password"
              formControlName="password"
              autocomplete="new-password"
            />
          </app-form-field>

          <app-form-field label="Confirmação da senha" [required]="true" [control]="form.controls.confirmPassword">
            <input
              appFieldControl
              type="password"
              formControlName="confirmPassword"
              autocomplete="new-password"
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
            {{ submitting() ? 'Cadastrando…' : 'Cadastrar' }}
          </button>
        </form>

        <p class="mt-6 text-sm text-content-muted">
          Já tem conta?
          <a routerLink="/entrar" class="font-medium text-accent underline">Entrar</a>
        </p>
      </section>
    </main>
  `,
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);

  protected readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [requiredText, emailFormat, maxLength(254)] }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [requiredText, minLengthPassword(8), maxLength(128)],
    }),
    confirmPassword: new FormControl('', {
      nonNullable: true,
      validators: [requiredText, matchesField('password')],
    }),
  });

  constructor() {
    revalidateOnChange(this.form.controls.password, this.form.controls.confirmPassword, inject(DestroyRef));
  }

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
    this.auth.register(email.trim(), password).subscribe({
      next: () => {
        this.submitting.set(false);
        void this.router.navigateByUrl('/');
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
      case 'EMAIL_ALREADY_REGISTERED':
        this.formError.set(EMAIL_TAKEN_MESSAGE);
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
