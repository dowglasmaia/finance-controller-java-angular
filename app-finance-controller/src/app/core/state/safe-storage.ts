/** Acesso a `localStorage` que nunca lança (modo privado, bloqueado ou indisponível). */
export function readStorage(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

export function writeStorage(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    // preferência fica só em memória
  }
}
