# Exemplos: escolhas explícitas e código legível

Exemplos originais e didáticos para Angular 21; não foram compilados num workspace Angular nesta criação. Não representam endpoints nem requisitos aprovados do Finance Controller. Adapte convenções de texto e tokens à aplicação antes de reutilizar.

## 1. Validar uma fronteira em vez de silenciar o compilador

Evite confiar em `JSON.parse(text) as CategorySummary`. O cast não verifica o JSON. Para uma estrutura pequena, um decoder explícito pode ser suficiente:

```ts
export interface CategorySummary {
  readonly id: string;
  readonly name: string;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

export function decodeCategory(value: unknown): CategorySummary {
  if (!isRecord(value)) {
    throw new Error('Resposta de categoria inválida.');
  }

  const id = value['id'];
  const name = value['name'];

  if (
    typeof id !== 'string' || id.trim().length === 0 ||
    typeof name !== 'string' || name.trim().length === 0
  ) {
    throw new Error('Categoria sem identificador ou nome válido.');
  }

  return {id, name};
}
```

Este decoder só valida o contrato ilustrado; não normaliza silenciosamente nomes nem valida regras de unicidade. Com schemas complexos, avalie o validador existente no projeto antes de escrever uma biblioteca manual.

Fonte da distinção entre tipo e runtime: [HttpClient e tipos](https://angular.dev/guide/http/making-requests).

## 2. Estados impossíveis devem ser difíceis de representar

Evite booleans independentes `loading`, `hasError` e `loaded` que podem ficar simultaneamente ativos.

```ts
export type LoadState<T> =
  | {readonly kind: 'loading'}
  | {readonly kind: 'ready'; readonly data: T}
  | {readonly kind: 'error'; readonly message: string};
```

Resultado vazio é `ready` com uma coleção vazia; falha é `error`. Acrescente `idle` ou atualização com dados anteriores apenas se a UX realmente precisar. A escolha da união é uma recomendação de modelagem, não uma exigência Angular.

## 3. Componente de apresentação com contrato pequeno

O filho recebe dados e emite uma intenção. Não injeta HttpClient nem altera o input.

```ts
import {
  ChangeDetectionStrategy, Component, computed, input, output,
} from '@angular/core';
import type {CategorySummary} from './category-summary';

@Component({
  selector: 'app-category-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2>Categorias ({{ count() }})</h2>
    <ul>
      @for (category of categories(); track category.id) {
        <li>
          <button type="button" (click)="selected.emit(category.id)">
            {{ category.name }}
          </button>
        </li>
      } @empty {
        <li>Nenhuma categoria encontrada.</li>
      }
    </ul>
  `,
})
export class CategoryList {
  readonly categories = input.required<readonly CategorySummary[]>();
  readonly selected = output<string>();
  protected readonly count = computed(() => this.categories().length);
}
```

No Angular 21 o componente é standalone por padrão. A lista deve receber IDs únicos da sua fronteira. Textos são literais aqui para tornar o exemplo autocontido; na aplicação, siga a convenção de textos existente.

Evite sincronizar `count` com `effect(() => count.set(...))`: introduziria uma segunda fonte de estado sem necessidade.

## 4. Reactive Form tipado sem assertions de nulidade

O formulário emite um comando de UI; o exemplo não inventa integração com uma API.

```ts
import {
  ChangeDetectionStrategy, Component, inject, output,
} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';

export interface CategoryDraft {
  readonly name: string;
}

@Component({
  selector: 'app-category-editor',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form [formGroup]="form" (ngSubmit)="submit()">
      <label for="category-name">Nome</label>
      <input
        id="category-name"
        formControlName="name"
        aria-describedby="category-name-error"
        [attr.aria-invalid]="form.controls.name.touched &&
          (form.controls.name.invalid || isBlankName())"
      />
      <p id="category-name-error" aria-live="polite">
        @if (form.controls.name.touched &&
             (form.controls.name.invalid || isBlankName())) {
          Informe um nome.
        }
      </p>
      <button type="submit">Continuar</button>
    </form>
  `,
})
export class CategoryEditor {
  private readonly fb = inject(FormBuilder).nonNullable;

  readonly submitted = output<CategoryDraft>();
  protected readonly form = this.fb.group({
    name: ['', Validators.required],
  });

  protected isBlankName(): boolean {
    return this.form.controls.name.value.trim().length === 0;
  }

  protected submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.isBlankName()) return;

    this.submitted.emit({name: this.form.controls.name.value.trim()});
  }
}
```

A verificação de espaços complementa `required`; extrai-a para um validator reutilizável se aparecer noutros formulários. Não acrescente um `computed` sobre `FormControl.value` esperando reatividade automática: esse valor não é um signal. Se o formulário se repetir na mesma página, use IDs únicos por instância.

O exemplo não trata persistência assíncrona. Quando existir, acrescente estado de submissão, falha e prevenção de duplicados conforme a jornada aprovada. Validação do servidor continua necessária.

## 5. Compor uma pesquisa cancelável, preservando erros

Evite subscrições aninhadas ou um callback que permita a uma resposta antiga substituir uma consulta nova. Esta função recebe uma porta pequena e pura quanto à sua composição; o adaptador HTTP é responsabilidade do chamador.

```ts
import {
  catchError, debounceTime, distinctUntilChanged,
  map, of, startWith, switchMap,
} from 'rxjs';
import type {Observable} from 'rxjs';
import type {CategorySummary} from './category-summary';
import type {LoadState} from './load-state';

export interface CategorySearch {
  search(term: string): Observable<readonly CategorySummary[]>;
}

export function categorySearchState(
  terms: Observable<string>,
  reader: CategorySearch,
): Observable<LoadState<readonly CategorySummary[]>> {
  return terms.pipe(
    map(term => term.trim()),
    debounceTime(250),
    distinctUntilChanged(),
    switchMap(term =>
      reader.search(term).pipe(
        map((data): LoadState<readonly CategorySummary[]> => ({
          kind: 'ready', data,
        })),
        catchError(() =>
          of<LoadState<readonly CategorySummary[]>>({
            kind: 'error', message: 'Não foi possível carregar as categorias.',
          }),
        ),
        startWith<LoadState<readonly CategorySummary[]>>({kind: 'loading'}),
      ),
    ),
  );
}
```

O `catchError` interno permite pesquisar novamente após falha. A consulta anterior é cancelada quando um novo termo sai do debounce; durante os 250 ms, o fluxo anterior pode ainda emitir. Se a UX exigir cancelamento imediato a cada tecla, mova o atraso para dentro do fluxo cancelável e teste essa semântica.

Consuma o fluxo com AsyncPipe ou converta uma vez com `toSignal`; se houver subscrição imperativa, trate o ciclo de vida. Uma interface passada como argumento não precisa de token DI. Se a injetar, use um token real.

Este padrão é para leitura. Não aplique `switchMap` automaticamente a pagamentos, gravações ou importações.

## 6. Testar comportamento, não a forma do código

Exemplo de teste Vitest do decoder do primeiro exemplo:

```ts
import {describe, expect, it} from 'vitest';
import {decodeCategory} from './category-summary';

describe('decodeCategory', () => {
  it('rejeita dados externos sem nome utilizável', () => {
    expect(() => decodeCategory({id: 'c1', name: '   '})).toThrow();
    expect(() => decodeCategory({id: 'c1', name: 10})).toThrow();
  });

  it('extrai o contrato público de uma resposta válida', () => {
    expect(decodeCategory({id: 'c1', name: 'Casa', internal: true}))
      .toEqual({id: 'c1', name: 'Casa'});
  });
});
```

Para a pesquisa, os cenários relevantes incluem sucesso, vazio, falha seguida de nova consulta e resposta antiga após nova consulta. Para o formulário, teste campos inválidos e emissão do comando. Não transforme exemplos de teste em alegação de testes executados.
