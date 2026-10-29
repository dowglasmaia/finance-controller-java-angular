import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { isApiError } from '../../../../core/http/api-error';
import {
  applyServerErrors,
  focusFirstInvalid,
  matchesField,
  maxLength,
  minLengthPassword,
  requiredText,
  revalidateOnChange,
} from '../../../../shared/forms/validators';
import { ToastService } from '../../../../shared/ui/toast.service';
import { ButtonDirective } from '../../../../shared/ui/button';
import { FORM_FIELD_IMPORTS } from '../../../../shared/ui/form-field';
import { AuthApiService } from '../../data-access/auth-api.service';

export const INVALID_LINK_MESSAGE = 'Este link é inválido ou expirou. Peça um novo.';
export const PASSWORD_CHANGED_MESSAGE = 'Senha alterada. Entre com a nova senha.';
const GENERIC_ERROR_MESSAGE = 'Não foi possível redefinir a senha agora. Tente novamente.';

/**
 * `/redefinir-senha`: define a nova senha a partir do link do e-mail. Pública, sem sessão. O token é lido da URL e
 * removido da barra de endereço imediatamente; fica só em memória (um campo privado), nunca em storage, sinal exposto
 * ao template, log ou rota seguinte.
 */
@Component({
  selector: 'app-reset-password-page',
  imports: [ReactiveFormsModule, RouterLink, ButtonDirective, ...FORM_FIELD_IMPORTS],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="flex min-h-screen items-center justify-center bg-surface px-4 py-8">
      <section class="w-full max-w-md rounded-card border border-line bg-surface-raised p-6 shadow-card sm:p-8">
        <h1 class="text-2xl font-semibold text-content">Redefinir senha</h1>

        @if (linkInvalid()) {
          <div aria-live="polite">
            <p role="alert" class="mt-6 rounded-control border border-expense bg-expense/10 px-3 py-3 text-sm font-medium text-expense">
              {{ invalidMessage }}
            </p>
          </div>
          <a routerLink="/esqueci-senha" appButton class="mt-4 w-full">Pedir um novo link</a>
        } @else {
          <p class="mt-1 text-sm text-content-muted">Crie uma nova senha para a sua conta.</p>

          <form class="mt-6 flex flex-col gap-4" [formGroup]="form" (ngSubmit)="submit()" novalidate>
            <app-form-field label="Nova senha" [required]="true" help="Use de 8 a 128 caracteres." [control]="form.controls.password">
              <input appFieldControl type="password" formControlName="password" autocomplete="new-password" />
            </app-form-field>

            <app-form-field label="Confirmar nova senha" [required]="true" [control]="form.controls.confirmPassword">
              <input appFieldControl type="password" formControlName="confirmPassword" autocomplete="new-password" />
            </app-form-field>

            @if (formError(); as message) {
              <p role="alert" class="rounded-control border border-expense bg-expense/10 px-3 py-2 text-sm font-medium text-expense">
                {{ message }}
              </p>
            }

            <button type="submit" appButton [disabled]="submitting()">{{ submitting() ? 'Salvando…' : 'Redefinir senha' }}</button>
          </form>
        }

        <p class="mt-6 text-sm text-content-muted">
          <a routerLink="/entrar" class="font-medium text-accent underline">Voltar para entrar</a>
        </p>
      </section>
    </main>
  `,
})
export class ResetPasswordPage {
  private readonly api = inject(AuthApiService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly document = inject(DOCUMENT);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /** Só em memória; não é sinal para nunca ir parar num template. */
  private token: string | null;

  protected readonly invalidMessage = INVALID_LINK_MESSAGE;
  protected readonly linkInvalid = signal(false);
  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);

  protected readonly form = new FormGroup({
    password: new FormControl('', { nonNullable: true, validators: [requiredText, minLengthPassword(8), maxLength(128)] }),
    confirmPassword: new FormControl('', { nonNullable: true, validators: [requiredText, matchesField('password')] }),
  });

  constructor() {
    revalidateOnChange(this.form.controls.password, this.form.controls.confirmPassword, inject(DestroyRef));
    const raw = inject(ActivatedRoute).snapshot.queryParamMap.get('token');
    this.token = raw !== null && raw.trim() !== '' ? raw.trim() : null;
    this.removeTokenFromAddressBar();
    this.linkInvalid.set(this.token === null);
  }

  protected submit(): void {
    if (this.submitting() || this.token === null) {
      return;
    }
    this.formError.set(null);
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      focusFirstInvalid(this.host.nativeElement);
      return;
    }
    this.submitting.set(true);
    this.api.confirmPasswordReset({ token: this.token, newPassword: this.form.getRawValue().password }).subscribe({
      next: () => {
        this.token = null;
        this.submitting.set(false);
        this.toast.success(PASSWORD_CHANGED_MESSAGE);
        void this.router.navigateByUrl('/entrar');
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.handleError(error);
      },
    });
  }

  /** `history.replaceState` troca a URL sem recarregar nem guardar o token no histórico. */
  private removeTokenFromAddressBar(): void {
    const view = this.document.defaultView;
    if (view) {
      view.history.replaceState(view.history.state, '', view.location.pathname);
    }
  }

  private handleError(error: unknown): void {
    if (!isApiError(error)) {
      this.formError.set(GENERIC_ERROR_MESSAGE);
      return;
    }
    if (error.code === 'PASSWORD_RESET_TOKEN_INVALID') {
      this.token = null;
      this.linkInvalid.set(true);
      return;
    }
    if (error.code === 'VALIDATION_FAILED') {
      const unmatched = applyServerErrors(this.form, error.fieldErrors);
      this.formError.set(unmatched[0]?.message ?? 'A senha não atende à política: use de 8 a 128 caracteres e evite senhas comuns.');
      setTimeout(() => focusFirstInvalid(this.host.nativeElement));
      return;
    }
    this.formError.set(error.message || GENERIC_ERROR_MESSAGE);
  }
}
