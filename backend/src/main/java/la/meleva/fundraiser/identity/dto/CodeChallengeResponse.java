package la.meleva.fundraiser.identity.dto;

/** Resposta uniforme de solicitação de código: não indica se o telefone está cadastrado. */
public record CodeChallengeResponse(String status, String detail) {
    public static CodeChallengeResponse sent() {
        return new CodeChallengeResponse("CODE_SENT",
                "Se o número puder receber mensagens, um código de verificação foi enviado.");
    }
}
