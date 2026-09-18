package carmarket.service;

import carmarket.enums.UserRole;
import carmarket.model.User;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataExportServiceTest {

    @Test
    void exportToExcel_shouldCreateSheetsAndHeaders() throws Exception {
        Path file = Files.createTempFile("carmarket-export-", ".xlsx");

        try {
            new DataExportService().exportToExcel(
                    file,
                    List.of(new User("Иван Иванов", "ivan", "hash", UserRole.USER)),
                    List.of(),
                    List.of()
            );

            assertTrue(Files.size(file) > 0);
            try (Workbook workbook = WorkbookFactory.create(file.toFile())) {
                assertEquals(3, workbook.getNumberOfSheets());
                assertEquals("Users", workbook.getSheetAt(0).getSheetName());
                assertEquals("ID", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }
}