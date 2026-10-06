package org.example.service.impl;

import org.example.dao.CategoryDao;
import org.example.dao.ProductDao;
import org.example.dao.UserDao;
import org.example.config.Jdbc;
import org.example.enums.Role;
import org.example.exception.AppException;
import org.example.model.Product;
import org.example.model.User;
import org.example.service.ProductService;
import org.example.utils.SessionManager;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.util.ArrayList;

public final class ProductServiceImpl implements ProductService {
    private final ProductDao products;
    private final CategoryDao categories;
    private final UserDao users;
    private final SessionManager session;
    private final Jdbc jdbc;

    public ProductServiceImpl(ProductDao products, CategoryDao categories, UserDao users, SessionManager session, Jdbc jdbc) {
        this.products = products;
        this.categories = categories;
        this.users = users;
        this.session = session;
        this.jdbc = jdbc;
    }

    @Override
    public List<Product> getCatalog() {
        return products.findActive();
    }

    @Override
    public List<Product> searchCatalog(String term, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
        if (categoryId != null && categories.findById(categoryId).isEmpty())
            throw new IllegalArgumentException("Category does not exist.");
        if (minPrice != null && minPrice.signum() < 0 || maxPrice != null && maxPrice.signum() < 0)
            throw new IllegalArgumentException("Price filters cannot be negative.");
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)
            throw new IllegalArgumentException("Minimum price cannot exceed maximum price.");
        return products.search(term, categoryId, minPrice, maxPrice);
    }

    @Override
    public List<Product> getMyProducts() {
        User user = session.require(Role.SELLER, Role.ADMIN);
        return user.getRole().equalsIgnoreCase(Role.ADMIN.name()) ? products.findAll() : products.findBySeller(user.getUserId());
    }

    @Override
    public Optional<Product> find(int productId) {
        User user = session.require(Role.SELLER, Role.ADMIN);
        Optional<Product> product = products.findById(productId);
        product.ifPresent(value -> requireOwner(user, value));
        return product;
    }

    @Override
    public boolean addProduct(int categoryId, String name, String description, double price, int stock) {
        User seller = session.require(Role.SELLER);
        return add(seller.getUserId(), categoryId, name, description, price, stock);
    }

    @Override
    public boolean addProductForSeller(int sellerId, int categoryId, String name, String description, double price, int stock) {
        session.require(Role.ADMIN);
        User seller = users.findById(sellerId).orElseThrow(() -> new AppException("Seller account not found."));
        if (!seller.getRole().equalsIgnoreCase(Role.SELLER.name()))
            throw new IllegalArgumentException("Selected account is not a seller.");
        return add(sellerId, categoryId, name, description, price, stock);
    }

    private boolean add(int sellerId, int categoryId, String name, String description, double price, int stock) {
        validate(categoryId, name, price, stock);
        return products.save(Product.builder().sellerId(sellerId).categoryId(categoryId)
                .productName(name.trim()).description(description).price(price).stockQuantity(stock)
                .active(true).build());
    }

    @Override
    public boolean updateProduct(int productId, int categoryId, String name, String description, double price, int stock) {
        User user = session.require(Role.SELLER, Role.ADMIN);
        validate(categoryId, name, price, stock);
        Product existing = products.findById(productId).orElseThrow(() -> new AppException("Product not found."));
        requireOwner(user, existing);
        existing.setCategoryId(categoryId);
        existing.setProductName(name.trim());
        existing.setDescription(description);
        existing.setPrice(price);
        existing.setStockQuantity(stock);
        return products.update(existing);
    }

    @Override
    public boolean setProductActive(int productId, boolean active) {
        User user = session.require(Role.SELLER, Role.ADMIN);
        Product product = products.findById(productId).orElseThrow(() -> new AppException("Product not found."));
        requireOwner(user, product);
        return products.setActive(productId, active);
    }

    @Override
    public int importProducts(Integer requestedSellerId, List<ProductImport> imports) {
        User user = session.require(Role.SELLER, Role.ADMIN);
        if (imports == null || imports.isEmpty() || imports.size() > 1000)
            throw new IllegalArgumentException("Import must contain between 1 and 1000 products.");
        int sellerId = user.getRole().equalsIgnoreCase(Role.SELLER.name()) ? user.getUserId() : requestedSellerId == null ? 0 : requestedSellerId;
        if (sellerId <= 0) throw new IllegalArgumentException("An admin must specify a seller account ID.");
        return jdbc.transaction(connection -> {
            User seller = users.findById(connection, sellerId)
                    .orElseThrow(() -> new AppException("Seller account not found."));
            if (!seller.getRole().equalsIgnoreCase(Role.SELLER.name()))
                throw new IllegalArgumentException("Selected account is not a seller.");
            List<Product> validated = new ArrayList<>(imports.size());
            for (ProductImport item : imports) {
                validateImport(item);
                if (categories.findById(connection, item.categoryId()).isEmpty())
                    throw new IllegalArgumentException("Category " + item.categoryId() + " does not exist.");
                validated.add(Product.builder().sellerId(sellerId).categoryId(item.categoryId())
                        .productName(item.name().trim()).description(item.description())
                        .price(item.price().doubleValue()).stockQuantity(item.stock()).active(true).build());
            }
            for (Product product : validated) products.save(connection, product);
            return validated.size();
        });
    }

    private void validateImport(ProductImport item) {
        if (item == null || item.name() == null || item.name().isBlank() || item.name().length() > 200)
            throw new IllegalArgumentException("Every imported product needs a name of 1-200 characters.");
        if (item.price() == null || item.price().scale() > 2 || item.price().signum() <= 0
                || item.price().compareTo(new BigDecimal("9999999999.99")) > 0)
            throw new IllegalArgumentException("Each price must be greater than zero and have at most two decimals.");
        if (item.stock() < 0) throw new IllegalArgumentException("Stock cannot be negative.");
        if (item.description() != null && item.description().length() > 10000)
            throw new IllegalArgumentException("Product description cannot exceed 10000 characters.");
    }

    private void validate(int categoryId, String name, double price, int stock) {
        if (name == null || name.isBlank() || name.length() > 200)
            throw new IllegalArgumentException("Product name must contain 1-200 characters.");
        if (!Double.isFinite(price) || price <= 0 || price > 9_999_999_999.99)
            throw new IllegalArgumentException("Price must be greater than zero and fit the supported currency range.");
        if (stock < 0) throw new IllegalArgumentException("Stock cannot be negative.");
        if (categories.findById(categoryId).isEmpty()) throw new IllegalArgumentException("Category does not exist.");
    }

    private void requireOwner(User user, Product product) {
        if (user.getRole().equalsIgnoreCase(Role.SELLER.name()) && user.getUserId() != product.getSellerId())
            throw new AppException("You can only manage your own products.");
    }
}
