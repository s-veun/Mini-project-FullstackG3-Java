package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private int productId;
    private int sellerId;
    private int categoryId;
    private String productName;
    private String description;
    private double price;
    private int stockQuantity;
    private boolean active;
    private Timestamp createdAt;
}