package com.example.ctreview.service;

import com.example.ctreview.entity.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionReviewService {
    private final ReviewPolicy reviewPolicy;
    private final Clock clock;

    @SuppressWarnings("unchecked")
    private List<Problem> store(HttpSession session) {
        List<Problem> list = (List<Problem>) session.getAttribute("tempProblems");
        if (list == null) {
            list = new CopyOnWriteArrayList<>();
            session.setAttribute("tempProblems", list);
        }
        return list;
    }

    private LocalDateTime now() { return LocalDateTime.now(clock); }
    private LocalDate today() { return LocalDate.now(clock); }

    public Problem create(HttpSession session, Integer number, String name, ProblemCategory category, ProblemDifficulty difficulty) {
        log.debug("[session] create number={} name={}", number, name);
        List<Problem> list = store(session);
        if (list.stream().anyMatch(p -> p.getName().equals(name))) {
            throw new IllegalStateException("이미 존재하는 문제 이름");
        }
        Problem p = new Problem();
        p.setNumber(number);
        p.setName(name.trim());
        p.setCategory(category);
        p.setDifficulty(difficulty);
        p.setReviewStep(1);
        p.setReviewCount(0);
        scheduleNextReview(p, now());
        list.add(p);
        return p;
    }

    public List<Problem> listToday(HttpSession session) {
        log.debug("[session] listToday");
        return store(session).stream()
                .filter(p -> p.getStatus() == ProblemStatus.ACTIVE)
                .filter(p -> p.getNextReviewDate() != null && !p.getNextReviewDate().isAfter(now()))
                .sorted(Comparator.comparing(Problem::getReviewStep).reversed())
                .collect(Collectors.toList());
    }

    public List<Problem> listAll(HttpSession session) {
        return new ArrayList<>(store(session));
    }

    public Problem solve(HttpSession session, String name) {
        Problem p = getByName(session, name);
        log.debug("[session] solve name={}", name);
        if (p.getReviewStep() >= 3) {
            p.graduate();
        } else {
            p.setReviewStep(p.getReviewStep() + 1);
            p.setReviewCount(0);
            scheduleNextReview(p, now());
        }
        return p;
    }

    public Problem fail(HttpSession session, String name) {
        Problem p = getByName(session, name);
        log.debug("[session] fail name={}", name);
        p.setReviewCount(p.getReviewCount() + 1);
        scheduleNextReviewOnFail(p, now());
        return p;
    }

    public Problem graduate(HttpSession session, String name) {
        Problem p = getByName(session, name);
        log.debug("[session] graduate name={}", name);
        p.graduate();
        return p;
    }

    public void delete(HttpSession session, String name) {
        log.debug("[session] delete name={}", name);
        List<Problem> list = store(session);
        list.removeIf(p -> Objects.equals(p.getName(), name));
    }

    private Problem getByName(HttpSession session, String name) {
        return store(session).stream()
                .filter(p -> Objects.equals(p.getName(), name))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("문제 미존재(이름)"));
    }

    private void scheduleNextReview(Problem p, LocalDateTime base) {
        int[] intervals = reviewPolicy.intervals(p.getReviewStep());
        if (intervals.length == 0) {
            p.graduate();
            return;
        }
        var unit = reviewPolicy.unit();
        int index = Math.min(p.getReviewCount(), intervals.length - 1);
        int amt = intervals[index];
        activate(p, base.plus(amt, unit));
    }

    private void scheduleNextReviewOnFail(Problem p, LocalDateTime base) {
        int[] intervals = reviewPolicy.intervals(p.getReviewStep());
        var unit = reviewPolicy.unit();
        if (intervals.length == 0) {
            activate(p, base.plus(1, unit));
            return;
        }
        if (p.getReviewCount() < intervals.length) {
            int amt = intervals[p.getReviewCount()];
            activate(p, base.plus(amt, unit));
        } else {
            int last = intervals[intervals.length - 1];
            activate(p, base.plus(last * 2L, unit));
            p.setReviewCount(intervals.length);
        }
    }

    private void activate(Problem p, LocalDateTime next) {
        p.setNextReviewDate(next);
        p.setStatus(ProblemStatus.ACTIVE);
    }
}
