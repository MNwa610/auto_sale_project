package carmarket.backend.error;

import org.springframework.http.HttpStatus;

/** Ожидаемая ошибка предметного API; сообщения не содержат SQL и внутренних данных. */
public class ApiFailure extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    public ApiFailure(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public static ApiFailure notFound() { return new ApiFailure(HttpStatus.NOT_FOUND, "NOT_FOUND", "Запись не найдена."); }
    public static ApiFailure invalid(String message) { return new ApiFailure(HttpStatus.BAD_REQUEST, "INVALID_DATA", message); }
    public static ApiFailure conflict(String code, String message) { return new ApiFailure(HttpStatus.CONFLICT, code, message); }
}
