package org.example.service;

import org.example.dao.ProductDao;
import org.example.model.Product;
import org.example.utils.InputValidator;

import java.util.List;

public class ProductService {
    private final ProductDao productDao = new ProductDao();

    public boolean addProduct(int sellerId, int categoryId, String name, String desc, double price, int stock) {
        if (!org.example.utils.InputValidator.isValidString(name) || !org.example.utils.InputValidator.isValidPrice(price) || stock < 0) {
            throw new IllegalArgumentException("Invalid product details.");
        }
        Product product = Product.builder()
                .sellerId(sellerId)
                .categoryId(categoryId)
                .productName(name)
                .description(desc)
                .price(price)
                .stockQuantity(stock)
                .build();
        return productDao.save(product);
    }

    public List<Product> getAllProducts() {
        return productDao.findAll();
    }
}