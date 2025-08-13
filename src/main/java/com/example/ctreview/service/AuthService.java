package com.example.ctreview.service;

import com.example.ctreview.entity.User;
import com.example.ctreview.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepo;

    public User getCurrentUser(HttpSession session) {
        Long id = (Long) session.getAttribute("uid");
        if (id == null) return null;
        return userRepo.findById(id).orElse(null);
    }
}
