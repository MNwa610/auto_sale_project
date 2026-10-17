package carmarket.model;

import java.util.List;

public record ExportTable(String name, List<String> headers, List<List<Object>> rows) {
}
