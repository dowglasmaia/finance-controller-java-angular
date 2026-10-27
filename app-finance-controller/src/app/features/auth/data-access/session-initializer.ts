import { EnvironmentProviders, inject, provideAppInitializer } from '@angular/core';
import { AuthService } from './auth.service';

/** Restaura a sessão antes do primeiro roteamento. Registrar em `app.config.ts`. */
export function provideSessionInitializer(): EnvironmentProviders {
  return provideAppInitializer(async () => {
    await inject(AuthService).restoreSession();
  });
}
