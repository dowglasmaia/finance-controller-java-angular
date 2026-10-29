# 49-frontend-recuperacao-de-senha

> **ID:** FRONTEND-SPEC-16 · **Categoria:** Frontend (`features/auth`) · **Estado:** executada na F11 (2026-10-29); teclado completo, leitor de tela e 320 px pendentes (F12).

## Objetivo

Construir as páginas de recuperação de senha: pedir o link por e-mail e definir a nova senha a partir do link recebido.

## Escopo

- Inclui: páginas `/esqueci-senha` e `/redefinir-senha`, link na página de entrada, estados e mensagens.
- Não inclui: regras e envio do e-mail (BACKEND-SPEC-18).

## Contexto técnico local

### Páginas (públicas, sem sessão)

| Rota | Conteúdo | Comportamento |
|---|---|---|
| `/esqueci-senha` | e-mail\*, botão `Enviar link` | Sempre mostra a mesma confirmação neutra: "Se o e-mail estiver cadastrado, enviaremos um link para redefinir a senha." Botão desabilitado durante o envio; erro de formato no campo; limite de pedidos não muda a mensagem |
| `/redefinir-senha` | nova senha\*, confirmação\* | Lê o `token` da URL e o **remove da barra de endereço imediatamente** (`history.replaceState`); sem token → mensagem de link inválido com atalho para pedir outro; sucesso → mensagem "Senha alterada. Entre com a nova senha." e vai a `/entrar` |

- Na página `/entrar` (FRONTEND-SPEC-06) há o link `Esqueci minha senha`.
- Erro `PASSWORD_RESET_TOKEN_INVALID`: "Este link é inválido ou expirou. Peça um novo." com botão para `/esqueci-senha`. Erros de política da senha por campo.
- A página de redefinição não guarda o token em `localStorage`, `sessionStorage` nem registos; mantém-no apenas em memória durante o formulário.
- Cabeçalho `Referrer-Policy: no-referrer` na entrega do frontend (INFRA-SPEC-05), para o token não vazar por *referrer*.

### Operações consumidas

`requestPasswordReset`, `confirmPasswordReset`.

### Acessibilidade

Rótulos visíveis, erros ligados aos campos, foco no primeiro erro, mensagens anunciadas por região `aria-live`, navegação por teclado.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-18](../48-backend-recuperacao-de-senha/spec.md)
- [FRONTEND-SPEC-06](../29-frontend-autenticacao-e-protecao-de-rotas/spec.md)

## Regras e critérios de aceitação

- [x] Pedir o link com e-mail cadastrado e com e-mail inexistente mostra exatamente a mesma mensagem.
  > ✅ 2026-10-29 15:10 +00:00 — `password-recovery-pages.spec.ts` (a mensagem neutra é igual para os dois e também para o 429) e no navegador: `Se o e-mail estiver cadastrado, enviaremos um link para redefinir a senha.` nos dois casos; e-mail inválido mostra o erro no campo sem chamar a API.
- [x] Abrir o link remove o token da URL e o formulário continua utilizável.
  > ✅ 2026-10-29 15:10 +00:00 — `password-recovery-pages.spec.ts` (`window.location.search` vazio e o token fora do texto da página) e no navegador: ao abrir `…/redefinir-senha?token=…` a barra ficou `…/redefinir-senha` e o formulário continuou utilizável (`history.replaceState`; `<meta name="referrer" content="no-referrer">`).
- [x] Nova senha e confirmação diferentes impedem o envio com mensagem no campo.
  > ✅ 2026-10-29 15:10 +00:00 — `password-recovery-pages.spec.ts`: `As senhas não coincidem` no campo e `A senha deve ter pelo menos 8 caracteres`, sem chamar a API.
- [x] Link usado ou expirado mostra a mensagem de link inválido com atalho para pedir outro.
  > ✅ 2026-10-29 15:10 +00:00 — `password-recovery-pages.spec.ts` (`PASSWORD_RESET_TOKEN_INVALID` e link sem token → `Este link é inválido ou expirou. Peça um novo.` com o botão `Pedir um novo link` para `/esqueci-senha`, sem formulário) e no navegador (reuso do token → mesma mensagem da API).
- [x] Após redefinir, o utilizador é levado a `/entrar` e a senha antiga já não funciona.
  > ✅ 2026-10-29 15:10 +00:00 — `password-recovery-pages.spec.ts` (toast `Senha alterada. Entre com a nova senha.` e navegação a `/entrar`) e no navegador: a senha antiga deu 401 e a nova 200.
- [ ] Nenhum token ou senha é gravado em armazenamento do navegador, URL final ou consola.
  > ⏳ 2026-10-29 15:10 +00:00 — `password-recovery-pages.spec.ts`: o corpo do `POST` leva o token e a senha, a URL e os cabeçalhos não; `localStorage` e `sessionStorage` sem o token nem a senha; o token fica num campo privado (não é sinal nem aparece no template). Não há `console.log` na feature. Falta uma inspeção do console no navegador.

### Skills de implementação

- **Skills obrigatórias:** `angular-module-scaffold`, `angular-architect`, `angular-tailwind-ui-specialist`.
- **Caminhos:** `.claude/skills/angular-module-scaffold/SKILL.md`; `.claude/skills/angular-architect/SKILL.md`; `.claude/skills/angular-tailwind-ui-specialist/SKILL.md`.
- **Responsabilidade:** estender `features/auth` com as duas páginas e o `doc.md`; formulários tipados; apresentação acessível.
- **Momento de utilização:** depois do contrato atualizado e de FRONTEND-SPEC-06.
- **Dependências:** BACKEND-SPEC-18, FRONTEND-SPEC-06.
- **Limitações:** a skill de módulo exige selecionar as operações (`requestPasswordReset`, `confirmPasswordReset`).

## Tasks

### Front-end

- [x] Criar as páginas, o serviço de dados e atualizar o `doc.md` de `features/auth`.
  > ✅ 2026-10-29 15:10 +00:00 — `features/auth/pages/forgot-password` e `reset-password`, rotas públicas `esqueci-senha` e `redefinir-senha` (sem `guestGuard`, para o link funcionar com ou sem sessão), `AuthApiService.requestPasswordReset/confirmPasswordReset`, tipos regenerados em `identity.ts`, `Referrer-Policy: no-referrer` no `index.html` e no nginx; `doc.md` de `features/auth` atualizado.

### Validação

- [ ] Testar a mensagem neutra, a remoção do token da URL, o token inválido e a redefinição completa.
  > ⏳ 2026-10-29 15:10 +00:00 — `npm test`: 213 testes Vitest verdes (10 novos em `password-recovery-pages.spec.ts` e 6 em `preferences-sync.spec.ts`), `ng build` verde e percurso completo no navegador contra a API real e o Mailpit: pedido → e-mail → link → nova senha → entrar. Falta: tema claro, 320/768 px, teclado completo e leitor de tela (F12).

## Resultado esperado

- Recuperação de senha simples e sem vazamento de informação sobre contas.
- Preservada a regra de não guardar credenciais no navegador.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
