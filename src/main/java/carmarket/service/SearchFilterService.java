package carmarket.service;

import carmarket.exception.BusinessException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.StringJoiner;

public final class SearchFilterService {
    private SearchFilterService() {
    }

    public static boolean matches(String query, String... fields) {
        String text = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        StringJoiner searchableText = new StringJoiner(" ");
        for (String field : fields) {
            searchableText.add(field == null ? "" : field.toLowerCase(Locale.ROOT));
        }
        return searchableText.toString().contains(text);
    }

    public static BigDecimal parsePrice(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim().replace(',', '.');
        if (!text.matches("[+-]?\\d+(\\.\\d+)?")) {
            throw new BusinessException("Поле «" + field + "»: введите число, например 500000,50, или оставьте поле пустым.");
        }
        BigDecimal price = new BigDecimal(text);
        if (price.signum() < 0) {
            throw new BusinessException("Поле «" + field + "»: цена не может быть отрицательной.");
        }
        return price;
    }

    public static void validatePriceRange(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BusinessException("Поля «Цена от» и «Цена до»: нижняя граница не должна быть больше верхней.");
        }
    }

    public static boolean inPriceRange(BigDecimal price, BigDecimal min, BigDecimal max) {
        if (min == null && max == null) {
            return true;
        }
        return price != null && (min == null || price.compareTo(min) >= 0)
                && (max == null || price.compareTo(max) <= 0);
    }
}
