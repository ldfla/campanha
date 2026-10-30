package la.meleva.fundraiser.shared;

import java.net.URI;
import java.time.Instant;

public record ApiProblem(
        URI type,
        String title,
        int status,
        String detail,
        String code,
        String correlationId,
        Instant timestamp
) {

    public static ApiProblem of(ErrorCode errorCode, String detail, String correlationId) {
        return new ApiProblem(
                URI.create("https://fundraiser.meleva.la/problems/" + errorCode.code().toLowerCase().replace('_', '-')),
                errorCode.message(),
                errorCode.httpStatus(),
                detail,
                errorCode.code(),
                correlationId,
                Instant.now()
        );
    }

    public static ApiProblem of(ErrorCode errorCode, String correlationId) {
        return of(errorCode, errorCode.message(), correlationId);
    }
}
