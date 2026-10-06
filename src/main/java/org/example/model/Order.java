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
public class Order {
    private int orderId;
    private int customerId;
    private double totalAmount;
    private String orderStatus;
    private Timestamp orderDate;
}