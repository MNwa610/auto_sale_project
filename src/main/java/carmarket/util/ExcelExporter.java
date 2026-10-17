package carmarket.util;

import carmarket.model.ExportTable;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ExcelExporter {
    public void export(Path file, List<ExportTable> tables) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); OutputStream output = Files.newOutputStream(file)) {
            Font font = workbook.createFont();
            font.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(font);
            CellStyle decimalStyle = workbook.createCellStyle();
            decimalStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setWrapText(true);

            for (ExportTable table : tables) {
                Sheet sheet = workbook.createSheet(table.name());
                Row header = sheet.createRow(0);
                for (int i = 0; i < table.headers().size(); i++) {
                    Cell cell = header.createCell(i);
                    cell.setCellValue(table.headers().get(i));
                    cell.setCellStyle(headerStyle);
                }
                int rowIndex = 1;
                for (List<Object> values : table.rows()) {
                    Row row = sheet.createRow(rowIndex++);
                    for (int i = 0; i < values.size(); i++) {
                        Object value = values.get(i);
                        Cell cell = row.createCell(i);
                        if (value instanceof Number number) {
                            cell.setCellValue(number.doubleValue());
                            if (value instanceof BigDecimal) {
                                cell.setCellStyle(decimalStyle);
                            }
                        } else {
                            cell.setCellValue(value == null ? "" : value.toString());
                            cell.setCellStyle(textStyle);
                        }
                    }
                }
                sheet.createFreezePane(0, 1);
                for (int i = 0; i < table.headers().size(); i++) {
                    sheet.autoSizeColumn(i);
                    sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i), 60 * 256));
                }
            }
            workbook.write(output);
        }
    }
}
