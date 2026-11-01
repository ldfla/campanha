# Checklist dos gates humanos

> Documento de apoio. **Nenhum item é marcado pelo agente.** Cada gate só é considerado cumprido com a
> evidência anexada e a assinatura do responsável. Falta de gate impede **produção real**, não DEMO.
> Situação consolidada em [`gates-status.md`](gates-status.md). Atualizado em 2026-11-01 (P20).

## G-LEGAL — modalidade, regulamento e promotor
| ID | Item | Responsável | Evidência esperada | Feito |
|---|---|---|---|---|
| G-LEGAL-01 | Parecer sobre a modalidade (ação entre amigos × título de capitalização, ADR-0007) | Advogado | Parecer assinado | ☐ |
| G-LEGAL-02 | Regulamento versionado com fórmula fechada da apuração e desempate (D-OPEN-008) | Advogado + organizador | PDF + hash publicado | ☐ |
| G-LEGAL-03 | Identificação do titular/organizador (D-OPEN-001) | Responsável legal | Documento anexado ao fluxo de autorização | ☐ |
| G-LEGAL-04 | Titularidade/nota dos prêmios físicos (D-OPEN-003) | Organizador | Notas fiscais / termo de doação | ☐ |
| G-LEGAL-05 | Data e número do concurso da Loteria Federal (D-OPEN-009) e regras de contingência | Organizador | Regulamento + fonte oficial | ☐ |

## G-PSP
| ID | Item | Responsável | Evidência esperada | Feito |
|---|---|---|---|---|
| G-PSP-01 | Contrato e homologação do PSP para a atividade e conta receptora (D-OPEN-010/014) | Financeiro | Contrato + carta de homologação | ☐ |
| G-PSP-02 | Webhook real autenticado, conciliação e estorno testados em sandbox do PSP | Engenharia + financeiro | Relatório de homologação | ☐ |

## G-FINANCE
| ID | Item | Responsável | Evidência esperada | Feito |
|---|---|---|---|---|
| G-FINANCE-01 | Tributação, retenções, destinação de recursos, custos de PSP (R$ 0,56/número) e de OTP (Twilio/WhatsApp) | Contador | Parecer contábil | ☐ |

## G-SECURITY
| ID | Item | Responsável | Evidência esperada | Feito |
|---|---|---|---|---|
| G-SECURITY-01 | Re-teste **independente** dos achados P20 (segregação ADR-0009), MFA real (TOTP/WebAuthn), OTP real, Termo de Aceitação de Risco assinado | Segurança + Diretoria | Relatório do revisor + termo | ☐ |

Pré-requisitos técnicos conhecidos: P20-F11 (atribuição aleatória), P20-F16 (CTA por capability), rate
limit/lockout compartilhado, invalidação de sessões na implantação (P20-F18).

## G-OPERATIONS
| ID | Item | Responsável | Evidência esperada | Feito |
|---|---|---|---|---|
| G-OPERATIONS-01 | Domínio + TLS (Cloudflare Full Strict), backup diário com restauração ensaiada e hash, alertas, atendimento | Operações | Registro do ensaio de restauração (P18-06) + painel de alertas | ☐ |

## G-LAUNCH
| ID | Item | Responsável | Evidência esperada | Feito |
|---|---|---|---|---|
| G-LAUNCH-01 | Habilitação explícita da campanha após todos os gates acima, com autorização anexada e dupla aprovação no produto (não por flag ou variável) | Responsável pela campanha | Registro de auditoria da habilitação | ☐ |
