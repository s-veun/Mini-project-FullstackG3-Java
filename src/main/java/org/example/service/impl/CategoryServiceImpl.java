package org.example.service.impl;

import org.example.dao.CategoryDao;
import org.example.enums.Role;
import org.example.model.Category;
import org.example.service.CategoryService;
import org.example.utils.SessionManager;

import java.util.List;
import java.util.Optional;

public final class CategoryServiceImpl implements CategoryService {
    private final CategoryDao categories;
    private final SessionManager session;

    public CategoryServiceImpl(CategoryDao categories, SessionManager session) {
        this.categories = categories;
        this.session = session;
    }

    @Override
    public boolean createCategory(String name, String description) {
        session.require(Role.ADMIN);
        validateName(name);
        return categories.save(Category.builder().categoryName(name.trim()).description(description).build());
    }

    @Override
    public List<Category> getAllCategories() {
        return categories.findAll();
    }

    @Override
    public Optional<Category> getCategoryById(int id) {
        return categories.findById(id);
    }

    @Override
    public boolean updateCategory(int id, String name, String description) {
        session.require(Role.ADMIN);
        validateName(name);
        if (categories.findById(id).isEmpty()) throw new IllegalArgumentException("Category does not exist.");
        return categories.update(Category.builder().categoryId(id).categoryName(name.trim()).description(description).build());
    }

    @Override
    public boolean deleteCategory(int id) {
        session.require(Role.ADMIN);
        if (categories.findById(id).isEmpty()) throw new IllegalArgumentException("Category does not exist.");
        return categories.delete(id);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 100)
            throw new IllegalArgumentException("Category name must contain 1-100 characters.");
    }
}
