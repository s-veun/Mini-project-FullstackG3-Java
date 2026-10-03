package org.example.service;

import org.example.dao.OrderDao;
import org.example.model.CartItem;
import java.util.List;

public class OrderService {
    private final OrderDao orderDao = new OrderDao();

    public boolean checkout(int customerId, double totalAmount, List<CartItem> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty.");
        }
        return orderDao.createOrderWithTransaction(customerId, totalAmount, cartItems);
    }
}