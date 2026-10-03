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
public class User {
    private int userId;
    private String username;
    private String password;
    private String email;
    private String role; // ADMIN, SELLER, CUSTOMER[cite: 3]
    private Timestamp createdAt;
}