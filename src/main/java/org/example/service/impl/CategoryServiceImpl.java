package org.example.service.impl;

import org.example.dao.CategoryDao;
import org.example.model.Category;
import org.example.service.CategoryService;
import org.example.utils.InputValidator;

import java.util.List;
import java.util.Optional;

public class CategoryServiceImpl implements CategoryService {
    private final CategoryDao categoryDao = new CategoryDao();

    @Override
    public boolean createCategory(String name, String description) {
        if (!InputValidator.isValidString(name)) {
            throw new IllegalArgumentException("Category name cannot be empty.");
        }
        Category category = Category.builder()
                .categoryName(name)
                .description(description)
                .build();
        return categoryDao.save(category);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryDao.findAll();
    }

    @Override
    public Optional<Category> getCategoryById(int id) {
        return categoryDao.findById(id);
    }

    @Override
    public boolean updateCategory(int id, String name, String description) {
        if (!categoryDao.existsById(id)) {
            throw new IllegalArgumentException("Category with ID " + id + " does not exist.");
        }
        if (!InputValidator.isValidString(name)) {
            throw new IllegalArgumentException("Category name cannot be empty.");
        }
        Category category = Category.builder()
                .categoryId(id)
                .categoryName(name)
                .description(description)
                .build();
        return categoryDao.update(category);
    }

    @Override
    public boolean deleteCategory(int id) {
        if (!categoryDao.existsById(id)) {
            throw new IllegalArgumentException("Category with ID " + id + " does not exist.");
        }
        return categoryDao.delete(id);
    }
}