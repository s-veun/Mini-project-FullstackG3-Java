package org.example.service;

import org.example.model.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryService {
    boolean createCategory(String name, String description);
    List<Category> getAllCategories();
    Optional<Category> getCategoryById(int id);
    boolean updateCategory(int id, String name, String description);
    boolean deleteCategory(int id);
}