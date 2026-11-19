package carmarket.backend;

import carmarket.backend.error.AuthFailure;
import carmarket.backend.security.PasswordPolicy;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PasswordPolicyTest {
    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void rejectsWeakPasswordsAndUtf8OverflowWithoutReturningTheInput() {
        for (String password : new String[]{null, "short", "lowercaseonly42", "UPPERCASEONLY42",
                "NoDigitsInThisPassword", "Aa1" + "я".repeat(35), "Aa1" + "x".repeat(70)}) {
            assertThatThrownBy(() -> policy.validate(password)).isInstanceOf(AuthFailure.class)
                    .hasMessageContaining("72 байта");
        }
    }

    @Test
    void acceptsExactly72BytesAndUnicodeWithinTheLimit() {
        assertThatCode(() -> policy.validate("Aa1" + "x".repeat(69))).doesNotThrowAnyException();
        assertThatCode(() -> policy.validate("НовыйПароль12345!")).doesNotThrowAnyException();
        assertThat(PasswordPolicy.normalizeLogin("  AdMiN  ")).isEqualTo("admin");
    }
}
