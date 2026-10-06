package org.example.model;

import java.sql.Timestamp;

public record Review(int reviewId, int productId, int customerId, int rating, String comment, Timestamp createdAt) {
}
