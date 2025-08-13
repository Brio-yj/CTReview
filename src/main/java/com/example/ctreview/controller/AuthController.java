package com.example.ctreview.controller;

import com.example.ctreview.entity.User;
import com.example.ctreview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

record LoginRequest(String email, String password) {}
record UserDto(String email) { static UserDto from(User u){ return new UserDto(u.getEmail()); } }

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    @PostMapping("/register")
    public void register(@RequestBody LoginRequest req){
        if(userRepo.existsByEmail(req.email())) throw new IllegalStateException("이미 존재하는 이메일");
        User u = new User();
        u.setEmail(req.email());
        u.setPasswordHash(encoder.encode(req.password()));
        userRepo.save(u);
    }

    @PostMapping("/login")
    public UserDto login(@RequestBody LoginRequest req, HttpSession session){
        User u = userRepo.findByEmail(req.email())
                .orElseThrow(() -> new IllegalStateException("이메일 미존재"));
        if(!encoder.matches(req.password(), u.getPasswordHash()))
            throw new IllegalStateException("비밀번호 불일치");
        session.setAttribute("uid", u.getId());
        return UserDto.from(u);
    }

    @PostMapping("/logout")
    public void logout(HttpSession session){
        session.invalidate();
    }

    @GetMapping("/me")
    public UserDto me(HttpSession session){
        Long id = (Long) session.getAttribute("uid");
        if(id == null) return null;
        return userRepo.findById(id).map(UserDto::from).orElse(null);
    }
}
