# ADR-0005 — Adiamento controlado de passkeys

**Status:** Aceito

## Decisão

O suporte WebAuthn/passkeys fica adiado para uma fase dedicada de credenciais e recuperação de conta. O schema já prevê `webauthn_credentials`, mas nenhum endpoint é exposto antes de definir RP ID, origens TLS permitidas, attestation, challenge store, recuperação segura e testes em navegadores compatíveis.

## Justificativa

Spring Security 7 suporta o fluxo WebAuthn, mas expor uma integração parcial sem origem de produção, HTTPS e política de recuperação aprovados cria risco de lockout e de associação incorreta de credenciais. A sessão com verificação DEMO de contato continua sendo o fallback temporário; administradores exigirão MFA antes de qualquer ação administrativa quando o fluxo for concluído.

## Consequência

`P04-03` é atendido por esta decisão documentada, sem alegar que passkeys estejam habilitadas em produção.
