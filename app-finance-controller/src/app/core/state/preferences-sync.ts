import { Injectable, effect, inject } from '@angular/core';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { ToastService } from '../../shared/ui/toast.service';
import { CurrencyStore } from './currency.store';
import { Preferences, PreferencesApiService } from './preferences-api.service';
import { SessionStore } from './session.store';
import { ThemeStore } from './theme.store';

const SAVE_ERROR = 'Não foi possível salvar suas preferências. Elas continuam neste dispositivo.';

/**
 * Sincroniza moeda e tema com o servidor, por utilizador (P-19). Depois de entrar, as preferências do servidor
 * prevalecem sobre as do dispositivo; cada troca de moeda ou tema (inclusive a troca automática ao salvar um
 * lançamento noutra moeda) é gravada no servidor. Falha ao gravar avisa mas não bloqueia o uso: o `localStorage` segue
 * como cópia local para antes do login. Sem sessão não há chamada e nada é gravado.
 */
@Injectable({ providedIn: 'root' })
export class PreferencesSync {
  private readonly api = inject(PreferencesApiService);
  private readonly session = inject(SessionStore);
  private readonly currency = inject(CurrencyStore);
  private readonly theme = inject(ThemeStore);
  private readonly toast = inject(ToastService);

  /** O que o servidor tem (ou acabamos de lhe mandar); `null` enquanto não se carregou nesta sessão. */
  private synced: Preferences | null = null;

  constructor() {
    effect(() => {
      const current: Preferences = { currency: this.currency.selectedCurrency(), theme: this.theme.themeMode() };
      if (!this.session.isAuthenticated()) {
        this.synced = null;
        return;
      }
      if (this.synced === null || (this.synced.currency === current.currency && this.synced.theme === current.theme)) {
        return;
      }
      this.synced = current;
      this.api
        .savePreferences(current)
        .pipe(catchError(() => of(null)))
        .subscribe((saved) => {
          if (saved === null) {
            this.toast.error(SAVE_ERROR);
          }
        });
    });
  }

  /** Lê as preferências do servidor e as aplica; nunca lança (falha mantém as do dispositivo). */
  load(): Observable<void> {
    return this.api.getPreferences().pipe(
      tap((preferences) => {
        this.synced = preferences;
        this.currency.select(preferences.currency);
        this.theme.setMode(preferences.theme);
      }),
      map(() => undefined),
      catchError(() => {
        // sem o que o servidor tem, o que o utilizador mudar ainda é gravado a partir do estado atual
        this.synced = { currency: this.currency.selectedCurrency(), theme: this.theme.themeMode() };
        return of(undefined);
      }),
    );
  }
}
