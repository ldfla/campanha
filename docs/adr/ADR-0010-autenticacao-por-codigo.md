# ADR-0010 — Autenticação por código de verificação e MFA vinculado à sessão

**Status:** Aceito (DEMO) — revisão humana pendente  
**Data:** 2026-11-01  
**Origem:** Red Team P20 (`docs/security/red-team/P20-findings.md`, F01/F02/F06)

## Contexto
O identificador pessoal é o WhatsApp (AGENTS §1). O contrato anterior criava sessão em
`POST /auth/login {phoneNumber}` e em `POST /auth/register`, sem prova de posse do número: qualquer
pessoa que soubesse um telefone assumia a conta. O MFA administrativo aceitava o código em uma sessão
diferente da do primeiro fator, sem bloqueio, e o código DEMO existia em todos os perfis.

## Decisão
1. `POST /auth/register` e `POST /auth/login` respondem **202** `{"status":"CODE_SENT","detail":…}` —
   idêntico para telefone cadastrado, inexistente ou duplicado — e **não criam sessão**.
2. `POST /auth/login/verify {phoneNumber, code}` comprova a posse, marca o telefone como verificado,
   rotaciona o ID de sessão e cria a sessão. Papéis administrativos recebem 403 `AUTH_MFA_REQUIRED` e
   um estado `PendingMfa` (5 min) **na sessão do servidor**.
3. `POST /auth/mfa/verify` só aceita código na sessão com `PendingMfa` e para o mesmo telefone.
4. Bloqueio de 15 min após 5 falhas por telefone (código) e por usuário (MFA); envio de código limitado
   por telefone; rate limit por IP real (sem `X-Forwarded-For`).
5. Adaptadores DEMO (`MockContactVerificationProvider`, `DemoMfaCodeVerifier`) só existem nos perfis
   `default/local/test` e **recusam iniciar** com `app.live-operations-enabled=true`.

## Consequências
- **Quebra de contrato** para clientes que esperavam `UserResponse` em `/auth/login` e `/auth/register`:
  frontend atualizado (`code-login-form.tsx`, `use-auth.ts`); testes migrados para `AuthTestSupport`.
- Envio real de OTP (Twilio/WhatsApp) tem custo ainda não estimado (AGENTS §1) e depende de G-FINANCE/G-PSP.
- Passkeys continuam adiadas (ADR-0005); TOTP/WebAuthn real é pré-requisito de G-LAUNCH.
- Ao implantar, **invalidar todas as sessões existentes** (criadas sob o contrato inseguro).

## Testes
`AuthControllerTest` (17), `RbacTest`, `P20RedTeamRegressionTest`; re-teste local por `curl` registrado no
relatório P20.
