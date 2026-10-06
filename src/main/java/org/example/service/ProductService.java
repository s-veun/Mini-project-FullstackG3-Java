package org.example.service;

import org.example.model.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductService {
    List<Product> getCatalog();
    List<Product> searchCatalog(String term, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice);
    List<Product> getMyProducts();
    Optional<Product> find(int productId);
    boolean addProduct(int categoryId, String name, String description, double price, int stock);
    boolean addProductForSeller(int sellerId, int categoryId, String name, String description, double price, int stock);
    boolean updateProduct(int productId, int categoryId, String name, String description, double price, int stock);
    boolean setProductActive(int productId, boolean active);
    int importProducts(Integer sellerId, List<ProductImport> products);

    record ProductImport(int categoryId, String name, String description, BigDecimal price, int stock) {
    }
}
