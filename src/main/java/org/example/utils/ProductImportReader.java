package org.example.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.poi.ss.usermodel.*;
import org.example.exception.AppException;
import org.example.service.ProductService;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ProductImportReader {
    private static final List<String> HEADERS =
            List.of("category_id", "product_name", "description", "price", "stock_quantity");

    private ProductImportReader() {
    }

    public static List<ProductService.ProductImport> read(Path path) {
        try {
            if (!Files.isRegularFile(path) || Files.size(path) > 25_000_000)
                throw new IllegalArgumentException("Select a regular import file no larger than 25 MB.");
            String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
            if (filename.endsWith(".csv")) return readCsv(path);
            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) return readExcel(path);
            throw new IllegalArgumentException("Import file must be CSV, XLSX or XLS.");
        } catch (IOException e) {
            throw new AppException("Unable to read product import file.", e);
        }
    }

    private static List<ProductService.ProductImport> readCsv(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                     .setTrim(true).get().parse(reader)) {
            Map<String, Integer> columns = normalizeHeaders(parser.getHeaderMap());
            List<ProductService.ProductImport> products = new ArrayList<>();
            for (var record : parser) {
                String[] values = new String[HEADERS.size()];
                for (int i = 0; i < HEADERS.size(); i++) values[i] = record.get(columns.get(HEADERS.get(i)));
                if (Arrays.stream(values).allMatch(String::isBlank)) continue;
                products.add(toProduct(values[0], values[1], values[2], values[3], values[4]));
                checkCount(products);
            }
            return products;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid CSV product import: " + e.getMessage(), e);
        }
    }

    private static List<ProductService.ProductImport> readExcel(Path path) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(path.toFile())) {
            if (workbook.getNumberOfSheets() == 0) throw new IllegalArgumentException("Excel file has no worksheets.");
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) throw new IllegalArgumentException("Excel worksheet is empty.");
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            Map<String, Integer> headerMap = new HashMap<>();
            for (Cell cell : header) {
                String value = formatter.formatCellValue(cell).trim().toLowerCase(Locale.ROOT);
                if (!value.isEmpty() && headerMap.put(value, cell.getColumnIndex()) != null)
                    throw new IllegalArgumentException("Duplicate column: " + value);
            }
            Map<String, Integer> columns = normalizeHeaders(headerMap);
            List<ProductService.ProductImport> products = new ArrayList<>();
            for (int rowIndex = header.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || row.getLastCellNum() < 0) continue;
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.FORMULA)
                        throw new IllegalArgumentException("Formula cells are not accepted in product imports.");
                }
                String[] values = new String[HEADERS.size()];
                for (int i = 0; i < HEADERS.size(); i++) {
                    Cell cell = row.getCell(columns.get(HEADERS.get(i)), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    values[i] = cell == null ? "" : formatter.formatCellValue(cell).trim();
                }
                if (Arrays.stream(values).allMatch(String::isBlank)) continue;
                products.add(toProduct(values[0], values[1], values[2], values[3], values[4]));
                checkCount(products);
            }
            return products;
        } catch (org.apache.poi.EncryptedDocumentException e) {
            throw new IllegalArgumentException("Excel file is invalid, encrypted or unsupported.", e);
        }
    }

    private static Map<String, Integer> normalizeHeaders(Map<String, Integer> input) {
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, Integer> entry : input.entrySet())
            result.put(entry.getKey().trim().toLowerCase(Locale.ROOT), entry.getValue());
        for (String required : HEADERS) {
            if (!result.containsKey(required)) throw new IllegalArgumentException("Missing required column: " + required);
        }
        return result;
    }

    private static ProductService.ProductImport toProduct(
            String categoryId, String name, String description, String price, String stock) {
        try {
            return new ProductService.ProductImport(Integer.parseInt(categoryId.trim()), name.trim(),
                    description.isBlank() ? null : description,
                    new BigDecimal(price.trim()), Integer.parseInt(stock.trim()));
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Each row needs numeric category_id, price and stock_quantity values.", e);
        }
    }

    private static void checkCount(List<?> products) {
        if (products.size() > 1000) throw new IllegalArgumentException("Import cannot exceed 1000 products.");
    }
}
