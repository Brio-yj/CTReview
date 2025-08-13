package com.example.ctreview.controller;

import com.example.ctreview.dto.ProblemDto;
import com.example.ctreview.dto.ProblemSearchRequest;
import com.example.ctreview.entity.Problem;
import com.example.ctreview.entity.ProblemStatus;
import com.example.ctreview.entity.User;
import com.example.ctreview.repository.ProblemRepository;
import com.example.ctreview.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
@Slf4j
public class ProblemQueryController {

    private final ProblemRepository problemRepo;
    private final AuthService authService;

    @GetMapping
    public List<ProblemDto> search(ProblemSearchRequest req, HttpSession session) {
        User user = authService.getCurrentUser(session);
        log.debug("Search problems userId={} params={}", user != null ? user.getId() : null, req);
        Stream<Problem> stream = problemRepo.findByUser(user).stream();
        if (req.status() == null) {
            stream = stream.filter(p -> p.getStatus() == ProblemStatus.ACTIVE);
        }

        if (req.number() != null) stream = stream.filter(p -> p.getNumber() == req.number());
        if (req.q() != null && !req.q().isBlank()) {
            String ql = req.q().toLowerCase();
            stream = stream.filter(p -> p.getName() != null && p.getName().toLowerCase().contains(ql));
        }
        if (req.difficulty() != null) stream = stream.filter(p -> p.getDifficulty() == req.difficulty());
        if (req.status() != null) stream = stream.filter(p -> p.getStatus() == req.status());
        if (req.from() != null) stream = stream.filter(p -> p.getNextReviewDate() != null && !p.getNextReviewDate().isBefore(req.from().atStartOfDay()));
        if (req.to() != null) stream = stream.filter(p -> p.getNextReviewDate() != null && !p.getNextReviewDate().isAfter(req.to().atTime(LocalTime.MAX)));

        Comparator<Problem> comparator = Comparator.comparing(Problem::getNextReviewDate, Comparator.nullsLast(Comparator.naturalOrder()));
        if ("dateDesc".equalsIgnoreCase(req.sort())) comparator = comparator.reversed();
        if ("stepDesc".equalsIgnoreCase(req.sort())) comparator = Comparator.comparing(Problem::getReviewStep).reversed();
        if ("stepAsc".equalsIgnoreCase(req.sort())) comparator = Comparator.comparing(Problem::getReviewStep);

        return stream.sorted(comparator).map(ProblemDto::from).toList();
    }
}
