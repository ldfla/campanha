package la.meleva.fundraiser.drawing;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Leitor da API pública da Caixa:
 * {@code https://servicebus2.caixa.gov.br/portaldeloterias/api/federal/{concurso}}.
 *
 * <p>Somente leitura e somente quando habilitado por configuração. Preserva o payload bruto e a URL
 * de origem para auditoria; nenhum número é inventado quando a fonte falha.</p>
 */
@Component
@ConditionalOnProperty(name = "fundraiser.federal.enabled", havingValue = "true")
public class CaixaFederalLotteryClient implements FederalLotteryClient {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RestClient rest;
    private final ObjectMapper json;
    private final String baseUrl;

    CaixaFederalLotteryClient(RestClient.Builder builder, ObjectMapper json,
                              @Value("${fundraiser.federal.base-url:https://servicebus2.caixa.gov.br/portaldeloterias/api/federal}") String baseUrl) {
        this.rest = builder.build();
        this.json = json;
        this.baseUrl = baseUrl;
    }

    @Override
    public FederalContest fetch(int contestNumber) {
        if (contestNumber < 1) throw ErrorCode.VALIDATION_ERROR.toException();
        String url = baseUrl + "/" + contestNumber;
        try {
            String body = rest.get().uri(url).retrieve().body(String.class);
            if (body == null || body.isBlank()) throw ErrorCode.FEDERAL_SOURCE_UNAVAILABLE.toException();
            JsonNode root = json.readTree(body);
            int numero = root.path("numero").asInt(contestNumber);
            LocalDate drawnOn = LocalDate.parse(root.path("dataApuracao").asString(), DATE);
            List<String> prizes = new ArrayList<>();
            for (JsonNode node : root.path("listaDezenas")) prizes.add(node.asString());
            if (prizes.isEmpty()) throw ErrorCode.FEDERAL_SOURCE_UNAVAILABLE.toException();
            return new FederalContest(numero, drawnOn, prizes, body, url);
        } catch (ErrorCode.BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ErrorCode.FEDERAL_SOURCE_UNAVAILABLE.toException();
        }
    }
}
