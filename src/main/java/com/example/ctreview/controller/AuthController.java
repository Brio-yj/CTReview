package com.example.ctreview.controller;

import com.example.ctreview.entity.User;
import com.example.ctreview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

record LoginRequest(String email, String password) {}
record UserDto(String email) { static UserDto from(User u){ return new UserDto(u.getEmail()); } }

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    @PostMapping("/register")
    public void register(@RequestBody LoginRequest req){
        log.debug("Register attempt email={}", req.email());
        if(userRepo.existsByEmail(req.email())) throw new IllegalStateException("이미 존재하는 이메일");
        User u = new User();
        u.setEmail(req.email());
        u.setPasswordHash(encoder.encode(req.password()));
        userRepo.save(u);
        log.debug("Register success id={} email={}", u.getId(), u.getEmail());
    }

    @PostMapping("/login")
    public UserDto login(@RequestBody LoginRequest req, HttpSession session){
        log.debug("Login attempt email={}", req.email());
        User u = userRepo.findByEmail(req.email())
                .orElseThrow(() -> new IllegalStateException("이메일 미존재"));
        if(!encoder.matches(req.password(), u.getPasswordHash())) {
            log.debug("Login failed password mismatch for email={}", req.email());
            throw new IllegalStateException("비밀번호 불일치");
        }
        session.setAttribute("uid", u.getId());
        log.debug("Login success id={} email={}", u.getId(), u.getEmail());
        return UserDto.from(u);
    }

    @PostMapping("/logout")
    public void logout(HttpSession session){
        Long id = (Long) session.getAttribute("uid");
        log.debug("Logout uid={}", id);
        session.invalidate();
    }

    @GetMapping("/me")
    public UserDto me(HttpSession session){
        Long id = (Long) session.getAttribute("uid");
        log.debug("Me uid={}", id);
        if(id == null) return null;
        return userRepo.findById(id).map(UserDto::from).orElse(null);
    }
}
