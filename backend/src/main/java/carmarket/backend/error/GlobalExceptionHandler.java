package carmarket.backend.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Единый ответ RFC 9457 без стека исключения и отклонённых значений полей. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.computeIfAbsent(error.getField(), key -> new java.util.ArrayList<>())
                        .add(error.getDefaultMessage() == null ? "Некорректное значение" : error.getDefaultMessage()));
        ProblemDetail problem = problem(status, "Проверьте заполнение полей запроса.");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest
                && servletRequest.getResponse() != null
                && servletRequest.getResponse().isCommitted()) {
            return null;
        }
        // Стандартные сообщения Spring могут содержать исходный ввод: заменяем их общим описанием.
        ProblemDetail detail = exception instanceof MethodArgumentNotValidException
                && body instanceof ProblemDetail validationProblem
                ? validationProblem : problem(status, description(status));
        if (request instanceof ServletWebRequest servletRequest) {
            detail.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
        }
        return super.handleExceptionInternal(exception, detail, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(
            Exception exception, HttpServletRequest request) {
        log.error("Необработанная ошибка REST API", exception);
        ProblemDetail detail = problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Не удалось выполнить запрос. Повторите попытку позже.");
        detail.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(detail);
    }

    private ProblemDetail problem(HttpStatusCode status, String description) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, description);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    private String description(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "Некорректный запрос.";
            case 404 -> "Запрошенный ресурс не найден.";
            case 405 -> "HTTP-метод не поддерживается для этого ресурса.";
            case 406 -> "Запрошенный формат ответа не поддерживается.";
            case 415 -> "Формат тела запроса не поддерживается.";
            default -> status.is5xxServerError()
                    ? "Не удалось выполнить запрос. Повторите попытку позже."
                    : "Запрос не может быть выполнен.";
        };
    }
}
