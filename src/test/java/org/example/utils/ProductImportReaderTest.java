package org.example.utils;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductImportReaderTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void readsCsvProductsByHeaderName() throws Exception {
        Path file = temporaryDirectory.resolve("products.csv");
        Files.writeString(file, "product_name,category_id,description,price,stock_quantity\n" +
                "\"Desk, oak\",4,Wood desk,129.95,8\n");

        List<ProductService.ProductImport> result = ProductImportReader.read(file);

        assertEquals(1, result.size());
        assertEquals(4, result.getFirst().categoryId());
        assertEquals("Desk, oak", result.getFirst().name());
        assertEquals(new BigDecimal("129.95"), result.getFirst().price());
        assertEquals(8, result.getFirst().stock());
    }

    @Test
    void readsXlsxProducts() throws Exception {
        Path file = temporaryDirectory.resolve("products.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Products");
            var header = sheet.createRow(0);
            String[] headers = {"category_id", "product_name", "description", "price", "stock_quantity"};
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            var product = sheet.createRow(1);
            product.createCell(0).setCellValue(3);
            product.createCell(1).setCellValue("Lamp");
            product.createCell(2).setCellValue("Reading light");
            product.createCell(3).setCellValue(24.5);
            product.createCell(4).setCellValue(12);
            try (var output = Files.newOutputStream(file)) {
                workbook.write(output);
            }
        }

        List<ProductService.ProductImport> result = ProductImportReader.read(file);

        assertEquals(1, result.size());
        assertEquals(3, result.getFirst().categoryId());
        assertEquals("Lamp", result.getFirst().name());
        assertEquals(new BigDecimal("24.5"), result.getFirst().price());
        assertEquals(12, result.getFirst().stock());
    }

    @Test
    void rejectsImportsWithoutAllRequiredColumns() throws Exception {
        Path file = temporaryDirectory.resolve("missing-column.csv");
        Files.writeString(file, "category_id,product_name,price,stock_quantity\n1,Chair,10.00,2\n");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ProductImportReader.read(file));

        assertTrue(error.getMessage().contains("description"));
    }
}
