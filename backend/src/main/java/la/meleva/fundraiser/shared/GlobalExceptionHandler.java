package la.meleva.fundraiser.shared;

import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(la.meleva.fundraiser.campaign.CampaignOperationBlockedException.class)
    public ResponseEntity<ApiProblem> handleCampaignBlocked(
            la.meleva.fundraiser.campaign.CampaignOperationBlockedException ex) {
        String correlationId = UUID.randomUUID().toString();
        var problem = ApiProblem.of(ErrorCode.CAMPAIGN_OPERATION_BLOCKED, ex.getMessage(), correlationId);
        return ResponseEntity.status(403).body(problem);
    }

    @ExceptionHandler(ErrorCode.BusinessException.class)
    public ResponseEntity<ApiProblem> handleBusiness(ErrorCode.BusinessException ex) {
        var code = ex.errorCode();
        String correlationId = UUID.randomUUID().toString();
        var problem = ApiProblem.of(code, correlationId);
        return ResponseEntity.status(code.httpStatus()).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiProblem> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));

        String correlationId = UUID.randomUUID().toString();
        var problem = ApiProblem.of(ErrorCode.VALIDATION_ERROR, detail, correlationId);
        return ResponseEntity.badRequest().body(problem);
    }
}
