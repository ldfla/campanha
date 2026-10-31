package la.meleva.fundraiser.notification;

import java.util.Objects;

/** Textos estáticos versionados para uso futuro por um canal de entrega autorizado. */
public enum NotificationTemplate {
    ACCOUNT_REGISTERED(1, Category.ESSENTIAL, "Cadastro recebido",
            "Seu cadastro foi recebido. Acesse a plataforma pelo endereço que você já conhece para conferir os dados."),
    PARTICIPATION_VALID(1, Category.ESSENTIAL, "Participação registrada",
            "Sua participação foi registrada. Consulte os detalhes diretamente na plataforma."),
    ADMINISTRATIVE_CHANGE(1, Category.ESSENTIAL, "Atualização administrativa",
            "Há uma atualização administrativa disponível. Consulte os detalhes diretamente na plataforma."),
    RESULT_AVAILABLE(1, Category.ESSENTIAL, "Resultado disponível",
            "Há um resultado disponível para consulta na plataforma. Confira os detalhes e o regulamento aplicável."),
    DELIVERY_UPDATE(1, Category.ESSENTIAL, "Atualização de entrega",
            "Há uma atualização de entrega para consulta. Acesse a plataforma pelo endereço que você já conhece.");

    public enum Category { ESSENTIAL, MARKETING }

    private final int version;
    private final Category category;
    private final String subject;
    private final String body;

    NotificationTemplate(int version, Category category, String subject, String body) {
        this.version = version;
        this.category = Objects.requireNonNull(category);
        this.subject = Objects.requireNonNull(subject);
        this.body = Objects.requireNonNull(body);
    }

    public int version() { return version; }
    public Category category() { return category; }
    public String subject() { return subject; }
    public String body() { return body; }
}
