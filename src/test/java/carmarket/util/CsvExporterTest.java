package carmarket.util;

import carmarket.model.ExportTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExporterTest {
    @TempDir Path directory;

    @Test
    void export_shouldWriteUtf8BomAndEscapeQuotesSeparatorAndNewlines() throws Exception {
        Path file = directory.resolve("данные.csv");
        ExportTable table = new ExportTable("Таблица", List.of("Название", "Значение"),
                List.of(Arrays.asList("Текст; \"цитата\"\r\nследующая строка", null)));
        new CsvExporter().export(file, table);
        assertThat(Arrays.copyOf(Files.readAllBytes(file), 3))
                .containsExactly((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo(
                "\uFEFFНазвание;Значение\r\n\"Текст; \"\"цитата\"\"\r\nследующая строка\";\r\n");
    }

    @Test
    void export_shouldFormatDecimalWithCommaAndEscapeHeaders() throws Exception {
        Path file = directory.resolve("цена.csv");
        new CsvExporter().export(file, new ExportTable("Таблица", List.of("Цена; ₽"),
                List.of(List.of(new BigDecimal("12345.67")))));
        assertThat(Files.readString(file, StandardCharsets.UTF_8))
                .isEqualTo("\uFEFF\"Цена; ₽\"\r\n12345,67\r\n");
    }

    @Test
    void export_shouldWriteHeaderForEmptyTable() throws Exception {
        Path file = directory.resolve("пусто.csv");
        new CsvExporter().export(file, new ExportTable("Таблица", List.of("ФИО", "Логин"), List.of()));
        assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo("\uFEFFФИО;Логин\r\n");
    }
}
