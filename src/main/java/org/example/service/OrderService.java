package org.example.service;

import org.example.enums.OrderStatus;
import org.example.model.Order;
import org.example.model.OrderDetail;

import java.util.List;

public interface OrderService {
   int checkout();
   List<Order> history();
   Order find(int id);
   List<OrderDetail> details(int orderId);
   void pay(int orderId, boolean successful);
   void cancel(int orderId);
   void advance(int orderId, OrderStatus next);
}