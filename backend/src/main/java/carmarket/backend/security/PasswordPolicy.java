package carmarket.backend.security;

import carmarket.backend.error.AuthFailure;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class PasswordPolicy {
    public void validate(String password) {
        if (password == null || password.codePointCount(0, password.length()) < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72
                || password.codePoints().noneMatch(Character::isUpperCase)
                || password.codePoints().noneMatch(Character::isLowerCase)
                || password.codePoints().noneMatch(Character::isDigit)) {
            throw new AuthFailure(HttpStatus.BAD_REQUEST, "PASSWORD_POLICY",
                    "Пароль должен содержать от 12 символов, заглавную и строчную буквы, цифру; максимум 72 байта UTF-8.");
        }
    }

    public static String normalizeLogin(String login) {
        return login.strip().toLowerCase(Locale.ROOT);
    }
}
