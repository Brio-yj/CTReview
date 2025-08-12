package com.example.ctreview.controller;

import com.example.ctreview.dto.ProblemDto;
import com.example.ctreview.dto.ProblemSearchRequest;
import com.example.ctreview.entity.Problem;
import com.example.ctreview.entity.ProblemStatus;
import com.example.ctreview.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemQueryController {

    private final ProblemRepository problemRepo;

    @GetMapping
    public List<ProblemDto> search(ProblemSearchRequest req) {
        Stream<Problem> stream = problemRepo.findAll().stream();
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
