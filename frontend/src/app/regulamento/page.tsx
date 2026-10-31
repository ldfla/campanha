import type { Metadata } from "next";
import Link from "next/link";
import { ORGANIZER, DRAWING_DATE, isDemoMode } from "@/lib/campaign-config";

export const metadata: Metadata = {
  title: "Regulamento — Ação entre Amigos",
  description:
    "Regulamento completo da Ação entre Amigos. Regras, prêmios, sorteio e condições de participação.",
};

const sections = [
  {
    title: "1. Da natureza e objetivo",
    content: `Esta é uma Ação entre Amigos organizada por ${ORGANIZER.name} (CNPJ ${ORGANIZER.cnpjMasked}), de caráter colaborativo e privado, destinada a arrecadação de recursos entre conhecidos para financiamento de atividades do organizador.

Esta ação não constitui loteria, promoção comercial, título de capitalização ou qualquer modalidade regulamentada pela legislação de loterias federais. Trata-se de iniciativa voluntária entre amigos e conhecidos, promovida por recomendação pessoal.

A participação é inteiramente voluntária. Os valores arrecadados destinam-se ao custeio dos prêmios oferecidos e despesas operacionais da ação.`,
  },
  {
    title: "2. Dos prêmios",
    content: `São oferecidos 5 (cinco) prêmios principais e 10 (dez) prêmios-surpresa, totalizando R$ 18.500,00 (dezoito mil e quinhentos reais) em premiações:

• 1º Prêmio — Kit Fotográfico Canon: Camera Canon D70, Lente Canon Profissional 24-70 1:4 e Flash 430EX (estimado em R$ 10.000,00)
• 2º Prêmio — MacBook Air M1 13" 8GB (estimado em R$ 4.000,00)
• 3º Prêmio — Kit Fotográfico Canon T1i Rebel (estimado em R$ 2.000,00)
• 4º Prêmio — R$ 1.000,00 (mil reais) via Pix
• 5º Prêmio — R$ 500,00 (quinhentos reais) via Pix
• 10 Prêmios-Surpresa — R$ 100,00 cada, para números sorteados aleatoriamente (total R$ 1.000,00)

Os prêmios em espécie (equipamentos) serão entregues conforme disponibilidade e nas condições informadas pelo organizador. Os valores estimados são referências de mercado e podem sofrer variação.`,
  },
  {
    title: "3. Dos números e participação",
    content: `Os números disponíveis vão de 00000 a 99999, totalizando 100.000 (cem mil) posições. Cada número custa R$ 0,65 (sessenta e cinco centavos). O participante pode adquirir quantos números desejar, dentro dos combos disponíveis.

A atribuição dos números é aleatória e será feita pelo sistema após confirmação do pagamento. Para participar, é necessário informar: nome, sobrenome e número de WhatsApp (usado como identificador pessoal).

A participação só é efetivada após confirmação do pagamento via Pix. Números reservados sem pagamento confirmado serão liberados automaticamente.`,
  },
  {
    title: "4. Do pagamento",
    content: `O pagamento é realizado exclusivamente via Pix. Após a geração do código Pix, o participante deverá efetuar o pagamento dentro do prazo informado. A confirmação é automática e os números são atribuídos imediatamente após a compensação.

Não são aceitos outros meios de pagamento. Em caso de não compensação dentro do prazo, a reserva de números é cancelada automaticamente e os números retornam ao estoque disponível.`,
  },
  {
    title: "5. Do sorteio e apuração",
    content: `O sorteio será realizado com base no resultado da Loteria Federal do dia 20 de dezembro de 2026. A utilização do resultado da Loteria Federal serve exclusivamente como fonte de aleatoriedade pública e verificável, não configurando qualquer vínculo, associação ou subordinação desta ação às loterias regulamentadas no Brasil.

A fórmula de apuração será publicada antes do encerramento das participações e não poderá ser alterada após a publicação. O resultado é determinístico e auditável a partir do número oficial da Loteria Federal.

Em caso de cancelamento ou adiamento da extração da Loteria Federal na data prevista, será utilizada a próxima extração disponível.`,
  },
  {
    title: "6. Dos prêmios-surpresa",
    content: `Os 10 números dos prêmios-surpresa são gerados aleatoriamente pelo sistema na inicialização da campanha e registrados em snapshot auditável com hash criptográfico (SHA-256). Os números são revelados somente após o encerramento da ação, garantindo que não houve manipulação posterior.

A prova criptográfica (commitment) é publicada antes do início das participações, permitindo verificação independente da integridade dos números.`,
  },
  {
    title: "7. Da entrega dos prêmios",
    content: `Os ganhadores serão notificados via WhatsApp no número cadastrado. Prêmios em dinheiro serão transferidos via Pix para conta indicada pelo ganhador em até 5 (cinco) dias úteis após a apuração. Prêmios em espécie serão combinados diretamente com o ganhador para entrega ou envio.

O ganhador tem até 30 (trinta) dias corridos após a notificação para se manifestar e fornecer os dados necessários para recebimento. Após este prazo, o organizador poderá destinar o prêmio conforme melhor conveniência.`,
  },
  {
    title: "8. Das bonificações por engajamento",
    content: `São oferecidas bonificações para participantes que contribuírem com a divulgação:

Divulgadores (por link mais ativo em redes sociais):
• 1º lugar: R$ 2.000,00 | 2º lugar: R$ 1.000,00 | 3º lugar: R$ 500,00

Participantes que mais adquirirem números:
• 1º lugar: R$ 2.000,00 | 2º lugar: R$ 1.000,00 | 3º lugar: R$ 750,00

Participantes que trouxerem mais amigos com participações concluídas:
• 1º lugar: R$ 1.000,00 | 2º lugar: R$ 750,00 | 3º lugar: R$ 500,00

Os rankings são atualizados em tempo real e identificados pelas iniciais do nome e últimos 4 dígitos do telefone, preservando a privacidade dos participantes.`,
  },
  {
    title: "9. Dos direitos dos participantes",
    content: `Todo participante tem direito a:

• Receber confirmação imediata dos números atribuídos após a compensação do pagamento.
• Consultar seus números a qualquer momento, mediante autenticação com seus dados cadastrais.
• Verificar a integridade do sorteio por meio da fórmula pública e dos snapshots auditáveis.
• Solicitar o cancelamento da participação e reembolso integral antes do encerramento das participações, mediante solicitação ao organizador.
• Ter seus dados pessoais tratados conforme a LGPD (Lei 13.709/2018), com finalidade exclusiva para a operação desta ação.
• Solicitar a exclusão de seus dados pessoais após o encerramento da ação e entrega dos prêmios.

O organizador se compromete a responder solicitações dos participantes em até 5 (cinco) dias úteis.`,
  },
  {
    title: "10. Da transparência e auditabilidade",
    content: `Toda a operação é registrada em log de auditoria. Os snapshots de números, pagamentos e resultados são verificáveis. O código de apuração, a fórmula do sorteio e os compromissos criptográficos dos prêmios-surpresa são publicados antes do encerramento das participações.

O organizador garante transparência total no processo, disponibilizando relatórios e evidências da apuração para verificação por qualquer participante.`,
  },
  {
    title: "11. Do encerramento e esgotamento",
    content: `A campanha será encerrada na data do sorteio (20 de dezembro de 2026) ou quando todos os números forem vendidos, o que ocorrer primeiro. No caso de esgotamento dos números, todas as funcionalidades de compra serão desativadas e os participantes serão notificados.

Caso a campanha não atinja um número mínimo de participações que viabilize a entrega dos prêmios, o organizador poderá cancelar a ação, mediante devolução integral dos valores pagos por todos os participantes, no prazo de até 15 (quinze) dias úteis.`,
  },
  {
    title: "12. Da privacidade e proteção de dados",
    content: `Os dados pessoais dos participantes (nome, sobrenome e WhatsApp) são coletados exclusivamente para fins desta ação e comunicação dos resultados, com base no consentimento do participante e no legítimo interesse do organizador.

O organizador se compromete a:
• Não compartilhar dados pessoais com terceiros sem consentimento.
• Armazenar dados com medidas de segurança adequadas.
• Permitir que participantes solicitem acesso, correção ou exclusão de seus dados.
• Manter os dados apenas pelo tempo necessário para a operação da ação e cumprimento de obrigações legais.

Em caso de incidente de segurança, o organizador notificará os participantes afetados conforme exigido pela LGPD.`,
  },
  {
    title: "13. Das disposições gerais",
    content: `A participação implica aceitação integral deste regulamento. Casos omissos serão resolvidos pelo organizador com base nos princípios de boa-fé e transparência.

Esta ação é promovida exclusivamente entre amigos e conhecidos do organizador, por meio de recomendação pessoal. O organizador não garante nem promete rentabilidade, retorno financeiro ou qualquer benefício além dos prêmios aqui descritos.

Organizador: ${ORGANIZER.name}
CNPJ: ${ORGANIZER.cnpjMasked}`,
  },
] as const;

export default function RegulamentoPage() {
  const demoMode = isDemoMode();

  return (
    <div className="min-h-dvh bg-[var(--color-snow)]">
      <header className="bg-[var(--color-midnight)] px-5 md:px-8 lg:px-16 py-5">
        <div className="max-w-3xl mx-auto flex items-center justify-between">
          <Link
            href="/"
            className="font-[family-name:var(--font-heading)] text-base font-bold text-white/80 hover:text-white transition-colors"
          >
            Ação entre Amigos
          </Link>
          <a
            href="/participar"
            className="text-sm font-semibold text-[var(--color-ember)] hover:text-[var(--color-ember)]/80 transition-colors"
          >
            Participar
          </a>
        </div>
      </header>

      <main className="px-5 md:px-8 lg:px-16 py-12 md:py-20">
        <div className="max-w-3xl mx-auto">
          <h1 className="font-[family-name:var(--font-heading)] text-[clamp(1.75rem,4vw,3rem)] font-bold text-[var(--color-midnight)] mb-3">
            Regulamento
          </h1>
          <p className="text-sm text-[var(--color-midnight)]/40 mb-4">
            Leia atentamente as condições de participação na Ação entre Amigos.
          </p>

          {demoMode && (
            <div className="mb-12 rounded-xl border border-[var(--color-iris)]/20 bg-[var(--color-iris)]/[0.06] px-5 py-4">
              <p className="text-sm font-semibold text-[var(--color-iris)]">
                Modo demonstração
              </p>
              <p className="mt-1 text-xs text-[var(--color-ink-soft)]">
                Este regulamento é provisório e será atualizado antes da
                ativação da campanha. Valores, prazos e condições podem sofrer
                alteração.
              </p>
            </div>
          )}

          <div className="space-y-10 md:space-y-14">
            {sections.map((section) => (
              <section key={section.title}>
                <h2 className="font-[family-name:var(--font-heading)] text-lg md:text-xl font-semibold text-[var(--color-midnight)] mb-3">
                  {section.title}
                </h2>
                <div className="text-sm md:text-base text-[var(--color-midnight)]/60 leading-relaxed whitespace-pre-line">
                  {section.content}
                </div>
              </section>
            ))}
          </div>

          <div className="mt-16 md:mt-20 pt-10 border-t border-[var(--color-midnight)]/8 text-center">
            <p className="text-sm text-[var(--color-midnight)]/40 mb-5">
              Ao participar, você aceita integralmente este regulamento.
            </p>
            <a
              href="/participar"
              className="inline-flex items-center gap-2 px-8 py-4 rounded-xl font-bold text-base bg-gradient-to-r from-[var(--color-ember)] to-[#FF8A65] text-white animate-cta-beacon hover:scale-[1.05] active:scale-[0.97] transition-transform"
            >
              Quero participar
            </a>
          </div>
        </div>
      </main>

      <footer className="bg-[var(--color-midnight)] px-5 md:px-8 lg:px-16 py-8">
        <div className="max-w-3xl mx-auto flex items-center justify-between text-sm text-white/30">
          <span className="font-[family-name:var(--font-heading)] text-base text-white/50">
            Ação entre Amigos
          </span>
          <nav className="flex gap-4 text-xs">
            <Link
              href="/"
              className="hover:text-white/60 transition-colors"
            >
              Voltar ao início
            </Link>
            <Link
              href="/faq"
              className="hover:text-white/60 transition-colors"
            >
              Perguntas frequentes
            </Link>
          </nav>
        </div>
      </footer>
    </div>
  );
}
