package org.example.model;

import java.math.BigDecimal;

public record OrderDetail(
    int orderDetailId,
    int orderId,
    int productId,
    String productName,
    BigDecimal price,
    int quantity,
    BigDecimal subtotal,
    int sellerId
) {
    public OrderDetail(int orderDetailId, int orderId, int productId, String productName, BigDecimal price, int quantity) {
        this(
            orderDetailId,
            orderId,
            productId,
            productName,
            price,
            quantity,
            price.multiply(BigDecimal.valueOf(quantity)),
            0
        );
    }

    public OrderDetail(int orderDetailId, int orderId, int productId, String productName,
                       BigDecimal price, int quantity, BigDecimal subtotal) {
        this(orderDetailId, orderId, productId, productName, price, quantity, subtotal, 0);
    }
}