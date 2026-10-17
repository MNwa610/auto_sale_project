package carmarket.service;

import carmarket.enums.UserRole;
import carmarket.enums.CarStatus;
import carmarket.enums.RequestStatus;
import carmarket.enums.SortField;
import carmarket.exception.BusinessException;
import carmarket.model.Car;
import carmarket.model.PurchaseRequest;
import carmarket.model.TableQuery;
import carmarket.model.User;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataExportServiceTest {
    @TempDir Path directory;

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

    @Test
    void exportRequests_shouldKeepFilteredSubsetAndOrderInBothFormats() throws Exception {
        InMemoryPurchaseRequestRepository repository = new InMemoryPurchaseRequestRepository();
        PurchaseRequest first = request(1L, "Toyota", "Camry", "Осмотр; \"вечером\"\nпосле 18:00", 50000);
        PurchaseRequest hidden = request(2L, "BMW", "X5", "НЕ ДОЛЖНО ПОПАСТЬ В ФАЙЛ", 100000);
        PurchaseRequest third = request(3L, "Toyota", "RAV4", "Позвоните", 70000);
        repository.seed(first);
        repository.seed(hidden);
        repository.seed(third);
        PurchaseRequestService queries = new PurchaseRequestService(repository,
                new InMemoryUserRepository(), new InMemoryCarRepository());
        List<PurchaseRequest> visible = queries.findForTable(
                new TableQuery("Toyota", RequestStatus.APPROVED, "500000", "", SortField.MILEAGE, false));
        DataExportService service = new DataExportService();
        Path csv = directory.resolve("заявки.csv");
        Path xlsx = directory.resolve("заявки.xlsx");
        service.exportRequests(csv, visible);
        service.exportRequests(xlsx, visible);
        String text = Files.readString(csv, StandardCharsets.UTF_8);
        assertThat(text).startsWith("\uFEFF№;Пользователь;Автомобиль;").contains("Одобрена", "01.01.2026 10:00")
                .doesNotContain("BMW", "НЕ ДОЛЖНО");
        assertThat(text.indexOf("\r\n3;")).isLessThan(text.indexOf("\r\n1;"));
        try (Workbook workbook = WorkbookFactory.create(xlsx.toFile())) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("Заявки");
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
            assertThat(sheet.getRow(0).getLastCellNum()).isEqualTo((short) 8);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("3");
            assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("1");
            assertThat(sheet.getRow(1).getCell(3).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(sheet.getRow(1).getCell(3).getNumericCellValue()).isEqualTo(850000.25);
            assertThat(sheet.getRow(1).getCell(4).getNumericCellValue()).isEqualTo(70000);
            assertThat(sheet.getRow(2).getCell(6).getStringCellValue()).isEqualTo(first.getMessage());
        }
    }

    @Test
    void exportRequests_shouldKeepLongIdentifiersAndFormulaLikeTextAsStrings() throws Exception {
        PurchaseRequest request = request(Long.MAX_VALUE, "Toyota", "Camry", "=1+1", 45000);
        Path file = directory.resolve("идентификатор.xlsx");
        new DataExportService().exportRequests(file, List.of(request));
        try (Workbook workbook = WorkbookFactory.create(file.toFile())) {
            var row = workbook.getSheetAt(0).getRow(1);
            assertThat(row.getCell(0).getCellType()).isEqualTo(CellType.STRING);
            assertThat(row.getCell(0).getStringCellValue()).isEqualTo(Long.toString(Long.MAX_VALUE));
            assertThat(row.getCell(6).getCellType()).isEqualTo(CellType.STRING);
            assertThat(row.getCell(6).getStringCellValue()).isEqualTo("=1+1");
        }
    }

    @Test
    void exportUsers_shouldIncludeOnlyVisibleFieldsAndNeverPassword() throws Exception {
        User user = new User("Иван Иванов", "ivan", "НЕ ПОКАЗЫВАТЬ ПАРОЛЬ", UserRole.ADMIN);
        user.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        DataExportService service = new DataExportService();
        Path csv = directory.resolve("пользователи.csv");
        Path xlsx = directory.resolve("пользователи.xlsx");
        service.exportUsers(csv, List.of(user));
        service.exportUsers(xlsx, List.of(user));
        assertThat(Files.readString(csv, StandardCharsets.UTF_8))
                .isEqualTo("\uFEFFФИО;Логин;Роль;Дата регистрации\r\nИван Иванов;ivan;Администратор;01.01.2026 10:00\r\n");
        try (Workbook workbook = WorkbookFactory.create(xlsx.toFile())) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getLastCellNum()).isEqualTo((short) 4);
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("Администратор");
            for (var row : sheet) {
                for (var cell : row) {
                    assertThat(cell.getStringCellValue()).doesNotContain("НЕ ПОКАЗЫВАТЬ", "password");
                }
            }
        }
    }

    @Test
    void exportCars_shouldIncludeSellerVinAndNumericValues() throws Exception {
        Car car = new Car(1L, "Toyota", "Camry", 2020, 45000, new BigDecimal("850000.25"),
                null, "Седан", "Автомат", "Бензин", new BigDecimal("2.5"), "Описание", CarStatus.AVAILABLE);
        car.setSellerFullName("Продавец");
        Path xlsx = directory.resolve("автомобили.xlsx");
        Path csv = directory.resolve("автомобили.csv");
        new DataExportService().exportCars(xlsx, List.of(car));
        new DataExportService().exportCars(csv, List.of(car));
        assertThat(Files.readString(csv, StandardCharsets.UTF_8))
                .contains("Toyota;Camry;2020;45000;850000,25;Доступен;Продавец;\r\n");
        try (Workbook workbook = WorkbookFactory.create(xlsx.toFile())) {
            var row = workbook.getSheetAt(0).getRow(1);
            assertThat(row.getCell(2).getNumericCellValue()).isEqualTo(2020);
            assertThat(row.getCell(3).getNumericCellValue()).isEqualTo(45000);
            assertThat(row.getCell(4).getNumericCellValue()).isEqualTo(850000.25);
            assertThat(row.getCell(6).getStringCellValue()).isEqualTo("Продавец");
            assertThat(row.getCell(7).getStringCellValue()).isEmpty();
        }
    }

    @Test
    void exportRequests_shouldWriteHeadersOnlyForEmptyTable() throws Exception {
        Path xlsx = directory.resolve("пусто.xlsx");
        Path csv = directory.resolve("пусто.csv");
        new DataExportService().exportRequests(xlsx, List.of());
        new DataExportService().exportRequests(csv, List.of());
        try (Workbook workbook = WorkbookFactory.create(xlsx.toFile())) {
            assertThat(workbook.getSheetAt(0).getPhysicalNumberOfRows()).isEqualTo(1);
        }
        assertThat(Files.readAllLines(csv, StandardCharsets.UTF_8)).hasSize(1);
    }

    @Test
    void completeFileName_shouldUseFilterWhenMissingExtensionAndKeepExplicitFormat() throws Exception {
        DataExportService service = new DataExportService();
        Path selected = directory.resolve("заявки");
        Path completed = directory.resolve("заявки.csv");
        assertThat(service.completeFileName(selected, ".csv")).isEqualTo(completed);
        assertThat(service.completeFileName(directory.resolve("данные.XLSX"), ".csv"))
                .isEqualTo(directory.resolve("данные.XLSX"));
        assertThat(service.needsOverwriteConfirmation(selected, completed)).isFalse();
        Files.writeString(completed, "существующий файл");
        assertThat(service.needsOverwriteConfirmation(selected, completed)).isTrue();
        assertThat(service.needsOverwriteConfirmation(completed, completed)).isFalse();
        assertThat(Files.readString(completed)).isEqualTo("существующий файл");
    }

    @Test
    void export_shouldGiveFriendlyMessageForWriteFailureAndRejectUnknownFormat() throws Exception {
        Path folder = Files.createDirectory(directory.resolve("папка.xlsx"));
        assertThatThrownBy(() -> new DataExportService().exportRequests(folder, List.of()))
                .isInstanceOf(BusinessException.class).hasMessageStartingWith("Не удалось сохранить файл.")
                .hasMessageNotContaining("AccessDeniedException");
        Path unsupported = directory.resolve("заявки.txt");
        assertThatThrownBy(() -> new DataExportService().exportRequests(unsupported, List.of()))
                .isInstanceOf(BusinessException.class).hasMessageContaining(".csv или .xlsx");
        assertThat(Files.exists(unsupported)).isFalse();
    }

    private PurchaseRequest request(Long id, String brand, String model, String message, int mileage) {
        PurchaseRequest request = new PurchaseRequest(1L, 1L, message, RequestStatus.APPROVED);
        request.setId(id);
        request.setUserFullName("Покупатель");
        request.setCarBrand(brand);
        request.setCarModel(model);
        request.setCarPrice(new BigDecimal("850000.25"));
        request.setCarMileage(mileage);
        request.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        return request;
    }
}
