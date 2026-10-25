# Angular 21: decisões técnicas e referências

Consulta de fontes: 16/10/2026. O alvo é Angular 21, não necessariamente a versão mais recente. Verifique a minor/patch instalada antes de aplicar uma API; páginas em angular.dev podem mostrar versões posteriores.

## Compatibilidade e maturidade

| Tema | Orientação para o alvo 21 |
|---|---|
| Standalone | Preferir em código novo; não acrescentar NgModules apenas para organizar pastas. Preservar interoperabilidade existente. |
| Zoneless | Padrão para novas aplicações no 21. Confirmar bootstrap e bibliotecas no projeto existente; não remover Zone.js incidentalmente. |
| OnPush | Recomendação explícita para componentes novos; não afirmar que é o default do 21. |
| Signal Forms | Experimental no 21. Preferir Reactive Forms tipados quando não houver decisão de adotar experimental. |
| resource/httpResource | Experimentais no 21. Usar HttpClient + RxJS como alternativa estável. |
| Vitest | Integração estável e padrão nos novos projetos CLI 21. Não trocar o runner existente numa tarefa funcional. |
| Angular Aria | Developer Preview no lançamento do 21; não tratar como dependência obrigatória. |

Fontes versionadas: [roadmap v21](https://v21.angular.dev/roadmap), [httpResource v21](https://v21.angular.dev/api/common/http/HttpResourceFn), [forms v21](https://v21.angular.dev/guide/forms). Contexto de lançamento: [anúncio oficial do Angular 21](https://blog.angular.dev/announcing-angular-v21-57946c34f14b).

A tabela oficial consultada documenta para **21.0.x** Node `^20.19.0 || ^22.12.0 || ^24.0.0`, TypeScript `>=5.9.0 <6.0.0` e RxJS `^6.5.3 || ^7.4.0`. Isso é compatibilidade, não uma escolha automática de versão nem garantia de suporte de segurança do Node. Reconfirme a linha da versão concreta em [compatibilidade oficial](https://v21.angular.dev/reference/versions).

## Componentes e templates

Use inputs necessários tipados, outputs com payload preciso, membros `protected` para o template e dependências `private readonly` quando adequado. Inputs não devem ser mutados pelo filho; emita intenção para o proprietário do estado.

Prefira nomes que expressem o domínio e consistência com o projeto. Evite uma renomeação global só para remover ou adicionar sufixos. Classes/funções devem ter responsabilidades coesas, sem limites arbitrários de linhas.

Em `@for`, use identidade estável para coleções que mudam; `$index` serve para casos estáticos apropriados. Use HTML semântico e imports locais. Uma chamada de signal no template é normal; o problema são cálculos pesados repetidos e efeitos colaterais.

Fonte: [guia de estilo oficial](https://angular.dev/style-guide). Reconfirme detalhes de APIs nos pacotes do 21 quando a página corrente tiver evoluído.

## Signals, RxJS e ciclo de vida

- Derive estado com `computed`; reserve `effect` para sincronização com APIs externas ou efeitos reais. Evite copiar um signal para outro através de effects.
- Exponha sinais somente de leitura e métodos de intenção. `asReadonly()` e `readonly` não impedem mutações profundas; use atualizações imutáveis.
- Converta um fluxo com `toSignal` uma vez num contexto de injeção apropriado, não em getters ou chamadas repetidas. Planeie valor inicial e erros.
- Para subscrições imperativas de longa duração, use `takeUntilDestroyed`. Fora do contexto de injeção, passe `DestroyRef` capturado num contexto válido. `AsyncPipe` também gere subscrições.
- Escolha concorrência pela semântica: `switchMap` para leitura em que a última consulta prevalece; `concatMap` para sequência; `exhaustMap` para ignorar nova submissão durante a anterior; `mergeMap` para concorrência deliberada.
- Cancelar uma subscrição de escrita não garante desfazer uma alteração no servidor. Idempotência depende do contrato.
- Em zoneless, atualizar um campo comum num callback arbitrário não garante renderização. Use notificações suportadas, como signals lidos no template ou `markForCheck` quando necessário, sem espalhar `detectChanges`.

Fontes: [signals](https://angular.dev/guide/signals), [zoneless](https://angular.dev/guide/zoneless), [takeUntilDestroyed](https://angular.dev/api/core/rxjs-interop/takeUntilDestroyed), [switchMap](https://rxjs.dev/api/operators/switchMap). Nessas páginas correntes, use apenas APIs confirmadas para o alvo 21.

## Formulários e HTTP

Reactive Forms tipados permitem distinguir campos opcionais e nulos. `nonNullable` altera também a semântica de reset; use-o intencionalmente. `form.value` pode omitir controlos desabilitados; `getRawValue()` inclui-os e só deve ser usado quando isso corresponder ao comando.

Validação no cliente melhora UX; a API continua responsável pelas regras e autorização. Não recalcule regras financeiras oficiais no frontend sem uma necessidade especificada.

`HttpClient.get<T>()` afirma um tipo ao compilador; não valida a resposta. Nas fronteiras que exigem verificação, use `unknown` com decoder ou schema validado. Não adicione uma biblioteca de schemas para validar dois campos simples.

Fontes: [forms tipados](https://angular.dev/guide/forms/typed-forms), [FormBuilder v21](https://v21.angular.dev/api/forms/FormBuilder), [requisições HTTP](https://angular.dev/guide/http/making-requests).

## Testes no alvo 21

Use TestBed para integração Angular e testes puros para lógica independente. Em Vitest/zoneless não use `fakeAsync`/`tick` de Angular como se Zone.js estivesse disponível; aguarde estabilização e controle temporizadores com o runner compatível.

Fontes: [tick v21 e restrição de Zone.js](https://v21.angular.dev/api/core/testing/tick), [testes de navegação v21](https://v21.angular.dev/guide/routing/testing).

Comprove navegação e acessibilidade também em navegador real quando esses comportamentos forem relevantes; testes DOM simulados não garantem fidelidade visual.
