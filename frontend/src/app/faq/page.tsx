import type { Metadata } from "next";
import Link from "next/link";
import { ORGANIZER, isDemoMode } from "@/lib/campaign-config";

export const metadata: Metadata = {
  title: "Perguntas Frequentes — Ação entre Amigos",
  description:
    "Tire suas dúvidas sobre a Ação entre Amigos. Regras, pagamento, prêmios, privacidade e muito mais.",
};

const faqs = [
  {
    question: "O que é esta Ação entre Amigos?",
    answer: `É uma iniciativa colaborativa organizada por ${ORGANIZER.name} para arrecadação de recursos entre amigos e conhecidos. Ao participar, você adquire números que concorrem a prêmios no valor total de R$ 25.300,00 — incluindo 5 prêmios principais, 110 prêmios-surpresa e bonificações de ranking. O sorteio é baseado no resultado da Loteria Federal de 20 de dezembro de 2026, utilizado apenas como fonte de aleatoriedade pública — não há qualquer vínculo com loterias regulamentadas. Sua participação é inteiramente voluntária.`,
  },
  {
    question: "Como funciona o pagamento?",
    answer: `O pagamento é feito exclusivamente via Pix. Ao escolher seu combo de números, o sistema gera um código Pix que deve ser pago dentro do prazo informado. Assim que o pagamento for compensado, seus números são atribuídos automaticamente e confirmados via WhatsApp. Caso o pagamento não seja compensado no prazo, a reserva é cancelada automaticamente e os números voltam a ficar disponíveis. Você não será cobrado duas vezes nem terá valores retidos indevidamente.`,
  },
  {
    question: "Posso cancelar minha participação e receber reembolso?",
    answer: `Sim. Você pode solicitar o cancelamento da sua participação e o reembolso integral dos valores pagos a qualquer momento antes do encerramento das participações. Para isso, entre em contato com o organizador informando seus dados cadastrais (nome e WhatsApp). O reembolso será processado via Pix em até 5 (cinco) dias úteis após a confirmação do cancelamento. Após o encerramento das participações, o cancelamento não será mais possível.`,
  },
  {
    question: "Como sei que o sorteio é justo?",
    answer: `A transparência é garantida por três mecanismos: (1) a fórmula de apuração é publicada antes do encerramento das participações e não pode ser alterada depois; (2) o resultado utiliza o número oficial da Loteria Federal, uma fonte pública e verificável por qualquer pessoa; (3) todos os números, pagamentos e resultados são registrados em snapshots auditáveis. Para os prêmios-surpresa, os números são gerados e selados com hash criptográfico (SHA-256) antes do início da campanha, comprovando que não houve manipulação.`,
  },
  {
    question: "O que acontece se eu ganhar?",
    answer: `Você será notificado via WhatsApp no número cadastrado. Prêmios em dinheiro (Pix) serão transferidos para a conta que você indicar em até 5 dias úteis. Prêmios em equipamentos (kits fotográficos, MacBook) serão combinados diretamente com você para entrega ou envio. Você tem até 30 dias após a notificação para se manifestar e fornecer os dados para recebimento. Recomendamos manter seu WhatsApp atualizado para garantir o recebimento da notificação.`,
  },
  {
    question: "Meus dados pessoais estão seguros?",
    answer: `Sim. Coletamos apenas nome, sobrenome e número de WhatsApp, utilizados exclusivamente para a operação desta ação e comunicação dos resultados. Seus dados não são compartilhados com terceiros sem seu consentimento e são armazenados com medidas de segurança adequadas. Você pode solicitar acesso, correção ou exclusão dos seus dados a qualquer momento, conforme a Lei Geral de Proteção de Dados (LGPD, Lei 13.709/2018). Nos rankings públicos, sua identidade é protegida: exibimos apenas suas iniciais e os últimos 4 dígitos do telefone.`,
  },
  {
    question: "E se a campanha for cancelada ou não atingir o mínimo?",
    answer: `Caso a campanha não atinja um número mínimo de participações que viabilize a entrega dos prêmios, o organizador poderá cancelá-la. Nesse caso, todos os participantes receberão o reembolso integral dos valores pagos em até 15 dias úteis via Pix. Se todos os números forem vendidos antes da data do sorteio, a campanha será encerrada antecipadamente e o sorteio acontecerá normalmente na data prevista.`,
  },
  {
    question: "Como posso consultar meus números?",
    answer: `Acesse a página "Meus números" no site e informe seus dados cadastrais (sobrenome e WhatsApp). Após a verificação, você poderá visualizar todos os seus números, seus status (confirmado ou pendente) e o histórico de participações. Por segurança, nenhuma informação é exibida sem a autenticação — seus dados só são acessíveis por você. Em caso de dificuldade, entre em contato com o organizador via WhatsApp.`,
  },
] as const;

function FAQItem({
  question,
  answer,
}: {
  question: string;
  answer: string;
}) {
  return (
    <details className="group rounded-2xl border border-black/[0.06] bg-white transition-shadow hover:shadow-sm">
      <summary className="flex cursor-pointer items-center justify-between gap-4 px-6 py-5 text-left font-[family-name:var(--font-heading)] text-base font-semibold text-[var(--color-midnight)] md:text-lg [&::-webkit-details-marker]:hidden">
        {question}
        <svg
          viewBox="0 0 20 20"
          fill="none"
          className="h-5 w-5 shrink-0 text-[var(--color-midnight)]/30 transition-transform group-open:rotate-180"
          aria-hidden="true"
        >
          <path
            d="M5 7.5l5 5 5-5"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </summary>
      <div className="px-6 pb-5 text-sm leading-relaxed text-[var(--color-midnight)]/55 md:text-base">
        {answer}
      </div>
    </details>
  );
}

export default function FAQPage() {
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
            Perguntas frequentes
          </h1>
          <p className="text-sm text-[var(--color-midnight)]/40 mb-10 md:mb-14">
            Tire suas dúvidas sobre a Ação entre Amigos.
          </p>

          {demoMode && (
            <div className="mb-8 rounded-xl border border-[var(--color-iris)]/20 bg-[var(--color-iris)]/[0.06] px-5 py-4">
              <p className="text-sm font-semibold text-[var(--color-iris)]">
                Modo demonstração
              </p>
              <p className="mt-1 text-xs text-[var(--color-ink-soft)]">
                As respostas abaixo são provisórias e podem ser atualizadas
                antes da ativação da campanha.
              </p>
            </div>
          )}

          <div className="space-y-3">
            {faqs.map((faq) => (
              <FAQItem
                key={faq.question}
                question={faq.question}
                answer={faq.answer}
              />
            ))}
          </div>

          <div className="mt-14 text-center">
            <p className="text-sm text-[var(--color-midnight)]/40 mb-2">
              Ainda tem dúvidas?
            </p>
            <p className="text-sm text-[var(--color-midnight)]/55">
              Entre em contato com o organizador via WhatsApp.
            </p>
          </div>

          <div className="mt-10 flex flex-col items-center gap-4 sm:flex-row sm:justify-center">
            <Link
              href="/regulamento"
              className="text-sm font-medium text-[var(--color-iris)] hover:underline"
            >
              Ler o regulamento completo
            </Link>
            <span className="hidden text-[var(--color-midnight)]/20 sm:inline">
              ·
            </span>
            <a
              href="/participar"
              className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-[var(--color-ember)] to-[#FF8A65] px-8 py-4 font-bold text-base text-white animate-cta-beacon hover:scale-[1.05] active:scale-[0.97] transition-transform"
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
              href="/regulamento"
              className="hover:text-white/60 transition-colors"
            >
              Regulamento
            </Link>
          </nav>
        </div>
      </footer>
    </div>
  );
}
