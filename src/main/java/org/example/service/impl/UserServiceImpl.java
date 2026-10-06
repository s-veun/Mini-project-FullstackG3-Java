package org.example.service.impl;

import org.example.dao.UserDao;
import org.example.enums.Role;
import org.example.exception.AppException;
import org.example.model.User;
import org.example.service.UserService;
import org.example.utils.SessionManager;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

public final class UserServiceImpl implements UserService {
    private static final int ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserDao users;
    private final SessionManager session;

    public UserServiceImpl(UserDao users, SessionManager session) {
        this.users = users;
        this.session = session;
    }

    @Override
    public User register(String username, String password, String email, String requestedRole) {
        if (username == null || !username.matches("[A-Za-z0-9_.-]{3,40}"))
            throw new IllegalArgumentException("Username must be 3-40 characters using letters, digits, '.', '_' or '-'.");
        if (password == null || password.length() < 5 || password.length() > 256)
            throw new IllegalArgumentException("Password must be between 5 and 256 characters.");
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new IllegalArgumentException("Enter a valid email address.");
        Role role;
        try {
            role = Role.valueOf(requestedRole.trim().toUpperCase());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Role must be CUSTOMER or SELLER.", e);
        }
        if (role == Role.ADMIN) throw new IllegalArgumentException("Administrator accounts cannot be self-registered.");
        if (users.findByUsername(username).isPresent() || users.findByEmail(email).isPresent())
            throw new AppException("That username or email is already registered.");

        User user = User.builder().username(username.trim()).email(email.trim().toLowerCase())
                .password(hash(password)).role(role.name()).build();
        users.save(user);
        return users.findByUsername(username.trim()).orElseThrow(() -> new AppException("Registered user could not be loaded."));
    }

    @Override
    public Optional<User> login(String username, String password) {
        if (username == null || password == null) return Optional.empty();
        Optional<User> user = users.findByUsername(username.trim());
        if (user.isEmpty()) return Optional.empty();
        String storedPassword = user.get().getPassword();
        if (storedPassword != null && storedPassword.startsWith("pbkdf2$")) {
            if (!verify(password, storedPassword)) return Optional.empty();
        } else if (storedPassword != null && MessageDigest.isEqual(
                password.getBytes(StandardCharsets.UTF_8), storedPassword.getBytes(StandardCharsets.UTF_8))) {
            String upgradedHash = hash(password);
            if (!users.updatePassword(user.get().getUserId(), storedPassword, upgradedHash))
                return users.findByUsername(username.trim()).filter(u -> verify(password, u.getPassword()));
            user.get().setPassword(upgradedHash);
        } else {
            return Optional.empty();
        }
        return user;
    }

    @Override
    public void updateProfile(String email, String currentPassword, String newPassword) {
        User user = session.currentUser();
        String updatedEmail = email == null || email.isBlank() ? user.getEmail() : email.trim();
        if (!updatedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new IllegalArgumentException("Enter a valid email address.");
        if (currentPassword == null || !verify(currentPassword, user.getPassword()))
            throw new IllegalArgumentException("Current password is incorrect.");
        String passwordHash = user.getPassword();
        if (newPassword != null && !newPassword.isEmpty()) {
            if (newPassword.length() < 5 || newPassword.length() > 256)
                throw new IllegalArgumentException("New password must be between 5 and 256 characters.");
            passwordHash = hash(newPassword);
        }
        if (users.updateProfile(user.getUserId(), updatedEmail, passwordHash)) {
            user.setEmail(updatedEmail);
            user.setPassword(passwordHash);
        } else {
            throw new AppException("Profile was not updated.");
        }
    }

    private String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(password.toCharArray(), salt, ITERATIONS);
        return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$" +
                Base64.getEncoder().encodeToString(derived);
    }

    private boolean verify(String password, String encoded) {
        if (encoded == null || !encoded.startsWith("pbkdf2$")) return false;
        try {
            String[] parts = encoded.split("\\$", -1);
            if (parts.length != 4) return false;
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 100_000 || iterations > 1_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new AppException("Password hashing is unavailable.", e);
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(password, '\0');
        }
    }
}
