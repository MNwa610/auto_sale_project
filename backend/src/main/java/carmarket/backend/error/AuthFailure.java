package carmarket.backend.error;

import org.springframework.http.HttpStatus;

/** Сообщения создаются сервером и не включают пароль, токены или исходный ввод. */
public class AuthFailure extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public AuthFailure(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
