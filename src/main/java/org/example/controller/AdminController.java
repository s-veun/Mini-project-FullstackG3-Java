package org.example.controller;

import org.example.dao.ReportDao;

public class AdminController {
    private final ReportDao reportDao = new ReportDao();

    public void displayReports() {
        reportDao.printRevenueReport();
    }
}