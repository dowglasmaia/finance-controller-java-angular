import { ChangeDetectionStrategy, Component, ElementRef, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { isApiError } from '../../../../core/http/api-error';
import { applyServerErrors, emailFormat, focusFirstInvalid, maxLength, requiredText } from '../../../../shared/forms/validators';
import { ButtonDirective } from '../../../../shared/ui/button';
import { FORM_FIELD_IMPORTS } from '../../../../shared/ui/form-field';
import { AuthApiService } from '../../data-access/auth-api.service';

/** A mesma confirmação para qualquer e-mail: a tela nunca revela se a conta existe. */
export const NEUTRAL_MESSAGE = 'Se o e-mail estiver cadastrado, enviaremos um link para redefinir a senha.';
const GENERIC_ERROR_MESSAGE = 'Não foi possível enviar o pedido agora. Tente novamente.';

/** `/esqueci-senha`: pede o link de recuperação por e-mail. Pública, sem sessão. */
@Component({
  selector: 'app-forgot-password-page',
  imports: [ReactiveFormsModule, RouterLink, ButtonDirective, ...FORM_FIELD_IMPORTS],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="flex min-h-screen items-center justify-center bg-surface px-4 py-8">
      <section class="w-full max-w-md rounded-card border border-line bg-surface-raised p-6 shadow-card sm:p-8">
        <h1 class="text-2xl font-semibold text-content">Esqueci minha senha</h1>
        <p class="mt-1 text-sm text-content-muted">Informe o e-mail da conta para receber um link de redefinição.</p>

        <div aria-live="polite">
          @if (sent()) {
            <p role="status" class="mt-6 rounded-control border border-line bg-surface-sunken px-3 py-3 text-sm text-content">
              {{ neutralMessage }}
            </p>
          }
        </div>

        @if (!sent()) {
          <form class="mt-6 flex flex-col gap-4" [formGroup]="form" (ngSubmit)="submit()" novalidate>
            <app-form-field label="E-mail" [required]="true" [control]="form.controls.email">
              <input appFieldControl type="email" formControlName="email" autocomplete="username" inputmode="email" />
            </app-form-field>

            @if (formError(); as message) {
              <p role="alert" class="rounded-control border border-expense bg-expense/10 px-3 py-2 text-sm font-medium text-expense">
                {{ message }}
              </p>
            }

            <button type="submit" appButton [disabled]="submitting()">{{ submitting() ? 'Enviando…' : 'Enviar link' }}</button>
          </form>
        }

        <p class="mt-6 text-sm text-content-muted">
          Lembrou a senha?
          <a routerLink="/entrar" class="font-medium text-accent underline">Entrar</a>
        </p>
      </section>
    </main>
  `,
})
export class ForgotPasswordPage {
  private readonly api = inject(AuthApiService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly neutralMessage = NEUTRAL_MESSAGE;
  protected readonly submitting = signal(false);
  protected readonly sent = signal(false);
  protected readonly formError = signal<string | null>(null);

  protected readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [requiredText, emailFormat, maxLength(254)] }),
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
    this.api.requestPasswordReset({ email: this.form.getRawValue().email.trim() }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.sent.set(true);
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
    if (error.code === 'VALIDATION_FAILED') {
      const unmatched = applyServerErrors(this.form, error.fieldErrors);
      this.formError.set(unmatched[0]?.message ?? null);
      setTimeout(() => focusFirstInvalid(this.host.nativeElement));
      return;
    }
    // limite de pedidos: a mensagem não muda, para a tela não denunciar nada
    if (error.status === 429 || error.code === 'TOO_MANY_ATTEMPTS') {
      this.sent.set(true);
      return;
    }
    this.formError.set(GENERIC_ERROR_MESSAGE);
  }
}
