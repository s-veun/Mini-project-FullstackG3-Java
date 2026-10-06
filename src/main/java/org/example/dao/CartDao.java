package org.example.dao;

import org.example.exception.AppException;
import org.example.model.CartItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CartDao {

    // រកមើលទំនិញទាំងអស់ក្នុង shopping_cart របស់ customer (តម្រៀបតាម product_id ASC)
    public List<CartItem> find(Connection c, int customerId) {
        String sql = "SELECT cart_id, customer_id, product_id, quantity FROM shopping_cart WHERE customer_id = ? ORDER BY product_id ASC";
        List<CartItem> items = new ArrayList<>();
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new CartItem(
                            rs.getInt("cart_id"),
                            rs.getInt("customer_id"),
                            rs.getInt("product_id"),
                            rs.getInt("quantity")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new AppException("Failed to retrieve cart items: " + e.getMessage(), e);
        }
        return items;
    }

    // បន្ថែម ឬធ្វើបច្ចុប្បន្នភាពចំនួនទំនិញក្នុង shopping_cart (ប្រើ UNIQUE constraint លើ customer_id, product_id)
    public void save(Connection c, int customerId, int productId, int quantity) {
        String sql = "INSERT INTO shopping_cart (customer_id, product_id, quantity) VALUES (?, ?, ?) " +
                "ON CONFLICT (customer_id, product_id) DO UPDATE SET quantity = EXCLUDED.quantity";
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            stmt.setInt(2, productId);
            stmt.setInt(3, quantity);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new AppException("Failed to save cart item: " + e.getMessage(), e);
        }
    }

    // លុបទំនិញជាក់លាក់ចេញពី shopping_cart
    public void remove(Connection c, int customerId, int productId) {
        String sql = "DELETE FROM shopping_cart WHERE customer_id = ? AND product_id = ?";
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            stmt.setInt(2, productId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new AppException("Failed to remove cart item: " + e.getMessage(), e);
        }
    }

    // សម្អាតទំនិញទាំងអស់ចេញពី shopping_cart ក្រោយពេល Checkout រួច
    public void clear(Connection c, int customerId) {
        String sql = "DELETE FROM shopping_cart WHERE customer_id = ?";
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new AppException("Failed to clear cart: " + e.getMessage(), e);
        }
    }
}