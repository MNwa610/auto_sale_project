package carmarket.util;

import carmarket.model.ExportTable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CsvExporter {
    public void export(Path file, ExportTable table) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write('\uFEFF');
            writeRow(writer, table.headers());
            for (List<Object> row : table.rows()) {
                writeRow(writer, row);
            }
        }
    }

    private void writeRow(BufferedWriter writer, List<?> values) throws IOException {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.write(';');
            }
            writer.write(escape(values.get(i)));
        }
        writer.write("\r\n");
    }

    private String escape(Object value) {
        String text = value == null ? "" : value instanceof BigDecimal decimal
                ? decimal.toPlainString().replace('.', ',') : value.toString();
        if (text.contains(";") || text.contains("\"") || text.contains("\r") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
