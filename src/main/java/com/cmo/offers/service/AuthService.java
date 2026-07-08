package com.cmo.offers.service;

import java.sql.SQLException;
import java.util.Optional;

import com.cmo.offers.entity.UserEntity;
import com.cmo.offers.dao.UserDAO;

public class AuthService {

    private final UserDAO userDAO;
    
    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public Optional<UserEntity> login(String username, String password)
            throws SQLException {

        Optional<UserEntity> userOpt = userDAO.findByUsername(username);

        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        UserEntity user = userOpt.get();

        if (!user.isActive()) {
            return Optional.empty();
        }

        // Temporary until BCrypt is added
        if (user.getPasswordHash().equals(password)) {
            return Optional.of(user);
        }

        return Optional.empty();
    }
    
    public boolean register(String username, String password) throws SQLException {

        if (userDAO.findByUsername(username).isPresent()) {
            return false;
        }

        UserEntity user = new UserEntity();

        user.setUsername(username);
        user.setPasswordHash(password);   // plain text for now
        user.setRole("USER");
        user.setActive(true);

        userDAO.save(user);

        return true;
    }
}
