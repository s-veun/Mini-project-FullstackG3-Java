package org.example.controller;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.dao.ReportDao;
import org.example.enums.Role;
import org.example.exception.AppException;
import org.example.model.User;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public final class AdminController {
    private final ReportDao reports;
    private final CategoryController categories;
    private final ProductController products;
    private final OrderController orders;
    private final ProfileController profile;
    private final SessionManager session;
    private final Scanner scanner;

    public AdminController(ReportDao reports, CategoryController categories, ProductController products,
                           OrderController orders, ProfileController profile, SessionManager session, Scanner scanner) {
        this.reports = reports;
        this.categories = categories;
        this.products = products;
        this.orders = orders;
        this.profile = profile;
        this.session = session;
        this.scanner = scanner;
    }

    public void runAdminMenu() {
        session.require(Role.ADMIN);
        while (session.isLoggedIn()) {
            System.out.println("\n=== ADMIN DASHBOARD ===\n1. Browse catalog\n2. Manage categories\n3. Manage products\n" +
                    "4. Orders\n5. Sales report\n6. Profile\n0. Logout");
            System.out.print("Choice: ");
            try {
                switch (scanner.nextLine().trim()) {
                    case "1" -> products.showCatalog();
                    case "2" -> categories.handleCategoryMenu();
                    case "3" -> products.handleProductMenu();
                    case "4" -> orders.adminOrdersMenu();
                    case "5" -> reportMenu();
                    case "6" -> profile.manageProfile();
                    case "0" -> { session.logout(); return; }
                    default -> System.out.println("Choose 1-6 or 0.");
                }
            } catch (RuntimeException e) {
                System.out.println("Action failed: " + e.getMessage());
            }
        }
    }

    public void runSellerReports() {
        session.require(Role.SELLER);
        reportMenu();
    }

    public void manageProfile() {
        session.require(Role.SELLER);
        profile.manageProfile();
    }

    private void reportMenu() {
        User user = session.require(Role.ADMIN, Role.SELLER);
        Integer sellerId = user.getRole().equalsIgnoreCase(Role.SELLER.name()) ? user.getUserId() : null;
        List<ReportDao.SalesRow> rows = reports.sales(sellerId);
        System.out.println("\n=== SALES REPORT ===");
        if (rows.isEmpty()) System.out.println("No completed sales yet.");
        rows.forEach(row -> System.out.printf("Order #%d | %s | %s | %s | qty %d | $%s%n",
                row.orderId(), row.orderDate(), row.sellerName(), row.productName(), row.quantity(), row.revenue()));
        System.out.println("Revenue total: $" + reports.totalRevenue(sellerId));
        System.out.println("Top products:");
        List<ReportDao.ProductPerformance> topProducts = reports.topProducts(sellerId, 10);
        if (topProducts.isEmpty()) System.out.println("No completed product sales.");
        for (ReportDao.ProductPerformance product : topProducts)
            System.out.printf("#%d | %s | %d sold | $%s revenue%n",
                    product.productId(), product.productName(), product.unitsSold(), product.revenue());
        System.out.println("Export as: 1. CSV  2. XLSX  0. Back");
        System.out.print("Choice: ");
        String choice = scanner.nextLine().trim();
        if (choice.equals("1")) exportCsv(rows, sellerId);
        else if (choice.equals("2")) exportXlsx(rows, sellerId);
        else if (!choice.equals("0")) System.out.println("Choose 1, 2 or 0.");
    }

    private void exportCsv(List<ReportDao.SalesRow> rows, Integer sellerId) {
        Path target = reportPath(sellerId, "csv");
        try {
            Files.createDirectories(target.getParent());
            try (var writer = Files.newBufferedWriter(target);
                 var printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                         .setHeader("Order ID", "Order Date", "Seller", "Product", "Quantity", "Unit Price", "Revenue")
                         .get())) {
                for (ReportDao.SalesRow row : rows)
                    printer.printRecord(row.orderId(), row.orderDate(), csvSafe(row.sellerName()), csvSafe(row.productName()),
                            row.quantity(), row.unitPrice(), row.revenue());
            }
            System.out.println("CSV report saved to " + target.toAbsolutePath());
        } catch (IOException e) {
            throw new AppException("Unable to export CSV report.", e);
        }
    }

    private void exportXlsx(List<ReportDao.SalesRow> rows, Integer sellerId) {
        Path target = reportPath(sellerId, "xlsx");
        try {
            Files.createDirectories(target.getParent());
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Sales");
                Row header = sheet.createRow(0);
                String[] headings = {"Order ID", "Order Date", "Seller", "Product", "Quantity", "Unit Price", "Revenue"};
                for (int i = 0; i < headings.length; i++) header.createCell(i).setCellValue(headings[i]);
                int index = 1;
                for (ReportDao.SalesRow data : rows) {
                    Row row = sheet.createRow(index++);
                    row.createCell(0).setCellValue(data.orderId());
                    row.createCell(1).setCellValue(data.orderDate().toString());
                    row.createCell(2).setCellValue(data.sellerName());
                    row.createCell(3).setCellValue(data.productName());
                    row.createCell(4).setCellValue(data.quantity());
                    row.createCell(5).setCellValue(data.unitPrice().doubleValue());
                    row.createCell(6).setCellValue(data.revenue().doubleValue());
                }
                for (int i = 0; i < headings.length; i++) sheet.autoSizeColumn(i);
                try (OutputStream output = Files.newOutputStream(target)) {
                    workbook.write(output);
                }
            }
            System.out.println("XLSX report saved to " + target.toAbsolutePath());
        } catch (IOException e) {
            throw new AppException("Unable to export XLSX report.", e);
        }
    }

    private Path reportPath(Integer sellerId, String extension) {
        String name = sellerId == null ? "sales-report" : "seller-" + sellerId + "-sales-report";
        return Path.of("reports", name + "." + extension);
    }

    private String csvSafe(String value) {
        if (value != null && !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0) return "'" + value;
        return value;
    }
}
