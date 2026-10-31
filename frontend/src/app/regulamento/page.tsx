import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Regulamento — Ação entre Amigos",
  description: "Regulamento completo da Ação entre Amigos. Regras, prêmios, sorteio e condições de participação.",
};

const sections = [
  {
    title: "1. Da natureza da ação",
    content: `Esta é uma Ação entre Amigos de caráter privado, sem fins lucrativos regulamentados, organizada por pessoa física para arrecadação colaborativa. Não se trata de loteria, promoção comercial ou título de capitalização. A participação é voluntária e os valores arrecadados destinam-se ao custeio dos prêmios e despesas operacionais da ação.`,
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

Os prêmios em espécie (equipamentos) serão entregues conforme disponibilidade e nas condições descritas pelo organizador. Os valores estimados são referências e não constituem garantia de valor de mercado.`,
  },
  {
    title: "3. Dos números e participação",
    content: `Os números disponíveis vão de 00000 a 99999, totalizando 100.000 (cem mil) posições. Cada número custa R$ 0,56 (cinquenta e seis centavos). O participante pode adquirir quantos números desejar, sem limite. A atribuição dos números é aleatória e será feita pelo sistema após confirmação do pagamento. Para participar, é necessário informar: nome completo, sobrenome e número de WhatsApp (usado como identificador pessoal). A participação só é efetivada após confirmação do pagamento via Pix.`,
  },
  {
    title: "4. Do pagamento",
    content: `O pagamento é realizado exclusivamente via Pix. Após a geração do código Pix, o participante tem prazo para efetuar o pagamento. A confirmação é automática e os números são atribuídos imediatamente após a compensação. Não são aceitos outros meios de pagamento. Em caso de não compensação, a reserva de números é cancelada automaticamente.`,
  },
  {
    title: "5. Do sorteio",
    content: `O sorteio será realizado com base no resultado da Loteria Federal do dia 20 de dezembro de 2026. A apuração utilizará a fórmula previamente publicada e auditável para determinar os números vencedores a partir do resultado oficial. A fórmula de apuração será publicada antes do encerramento das vendas e não poderá ser alterada após a publicação. Em caso de cancelamento ou adiamento da extração da Loteria Federal na data prevista, será utilizada a próxima extração disponível.`,
  },
  {
    title: "6. Dos prêmios-surpresa",
    content: `Os 10 números dos prêmios-surpresa são gerados aleatoriamente pelo sistema na inicialização da campanha e registrados em snapshot auditável com hash SHA-256. Os números são revelados somente após o encerramento da ação, garantindo que não houve manipulação posterior. A prova criptográfica (commitment) é publicada antes do início das vendas.`,
  },
  {
    title: "7. Da entrega dos prêmios",
    content: `Os ganhadores serão notificados via WhatsApp no número cadastrado. Prêmios em dinheiro serão transferidos via Pix para conta indicada pelo ganhador em até 5 dias úteis após a apuração. Prêmios em espécie serão combinados diretamente com o ganhador para entrega ou envio. O ganhador tem até 30 dias para se manifestar, sob pena de perda do direito ao prêmio.`,
  },
  {
    title: "8. Das bonificações por indicação e engajamento",
    content: `São oferecidas bonificações para incentivar a divulgação:

Divulgadores/Influencers (por link mais ativo):
• 1º lugar: R$ 2.000,00 | 2º lugar: R$ 1.000,00 | 3º lugar: R$ 500,00

Participantes que mais comprarem números:
• 1º lugar: R$ 2.000,00 | 2º lugar: R$ 1.000,00 | 3º lugar: R$ 750,00

Participantes que mais trouxerem vendas entre amigos:
• 1º lugar: R$ 1.000,00 | 2º lugar: R$ 750,00 | 3º lugar: R$ 500,00

Os rankings são atualizados em tempo real e identificados pelas iniciais do nome e últimos 4 dígitos do telefone, preservando a privacidade.`,
  },
  {
    title: "9. Da transparência e auditabilidade",
    content: `Toda a operação é registrada em log de auditoria imutável. Os snapshots de números, pagamentos e resultados são verificáveis publicamente. O código de apuração, a fórmula do sorteio e os compromissos criptográficos dos prêmios-surpresa são publicados antes do encerramento das vendas.`,
  },
  {
    title: "10. Das disposições gerais",
    content: `O organizador reserva-se o direito de cancelar a ação caso não seja atingido um número mínimo de participações, mediante devolução integral dos valores pagos. A participação implica aceitação integral deste regulamento. Casos omissos serão resolvidos pelo organizador com base nos princípios de boa-fé e transparência. Os dados pessoais dos participantes são tratados conforme a LGPD (Lei 13.709/2018), sendo utilizados exclusivamente para fins da ação e comunicação dos resultados.`,
  },
] as const;

export default function RegulamentoPage() {
  return (
    <div className="min-h-dvh bg-[var(--color-cream)]">
      {/* Header */}
      <header className="bg-[var(--color-charcoal)] px-4 md:px-8 lg:px-16 py-5">
        <div className="max-w-3xl mx-auto flex items-center justify-between">
          <a href="/" className="font-[family-name:var(--font-heading)] text-base font-bold text-[var(--color-cream)] hover:text-[var(--color-lime)] transition-colors">
            &larr; Ação entre Amigos
          </a>
          <a
            href="/participar"
            className="text-sm font-semibold text-[var(--color-lime)] hover:text-[var(--color-lime)]/80 transition-colors"
          >
            Participar &rarr;
          </a>
        </div>
      </header>

      {/* Content */}
      <main className="px-4 md:px-8 lg:px-16 py-12 md:py-20">
        <div className="max-w-3xl mx-auto">
          <h1 className="font-[family-name:var(--font-heading)] text-[clamp(1.75rem,4vw,3rem)] font-bold text-[var(--color-charcoal)] mb-3">
            Regulamento
          </h1>
          <p className="text-sm text-[var(--color-charcoal)]/50 mb-12 md:mb-16">
            Leia atentamente as condições de participação na Ação entre Amigos.
          </p>

          <div className="space-y-10 md:space-y-14">
            {sections.map((section) => (
              <section key={section.title}>
                <h2 className="font-[family-name:var(--font-heading)] text-lg md:text-xl font-semibold text-[var(--color-charcoal)] mb-3">
                  {section.title}
                </h2>
                <div className="text-sm md:text-base text-[var(--color-charcoal)]/70 leading-relaxed whitespace-pre-line">
                  {section.content}
                </div>
              </section>
            ))}
          </div>

          {/* CTA */}
          <div className="mt-16 md:mt-20 pt-10 border-t border-[var(--color-charcoal)]/10 text-center">
            <p className="text-sm text-[var(--color-charcoal)]/50 mb-5">
              Ao participar, você aceita integralmente este regulamento.
            </p>
            <a
              href="/participar"
              className="inline-flex items-center gap-2 px-8 py-4 rounded-xl font-bold text-base bg-[var(--color-lime)] text-[var(--color-charcoal)] shadow-[0_4px_24px_rgba(198,242,78,0.3)] hover:brightness-105 active:scale-[0.98] transition-all"
            >
              Quero participar
              <span aria-hidden="true">&rarr;</span>
            </a>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-[var(--color-charcoal)] px-4 md:px-8 lg:px-16 py-8">
        <div className="max-w-3xl mx-auto flex items-center justify-between text-sm text-[var(--color-cream)]/40">
          <span className="font-[family-name:var(--font-heading)] text-base text-[var(--color-cream)]/60">
            Ação entre Amigos
          </span>
          <a href="/" className="hover:text-[var(--color-cream)]/70 transition-colors">Voltar ao início</a>
        </div>
      </footer>
    </div>
  );
}
