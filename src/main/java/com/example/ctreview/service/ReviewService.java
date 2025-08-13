package com.example.ctreview.service;

import com.example.ctreview.entity.*;
import com.example.ctreview.repository.ProblemRepository;
import com.example.ctreview.repository.ReviewLogRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.Clock;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReviewService {
    private final ProblemRepository problemRepo;
    private final ReviewLogRepository logRepo;
    private final ReviewPolicy reviewPolicy;
    private final Clock clock;

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    @Transactional
    public Problem createProblem(User user, Integer number, String name,
                                 ProblemCategory category, ProblemDifficulty difficulty) {
        log.debug("createProblem userId={} number={} name={}", user != null ? user.getId() : null, number, name);
        if (problemRepo.existsByNameAndUser(name.trim(), user)) throw new IllegalStateException("이미 존재하는 문제 이름");
        var p = new Problem();
        p.setUser(user);
        p.setNumber(number);
        p.setName(name.trim());
        p.setCategory(category);
        p.setDifficulty(difficulty);
        p.setReviewStep(1);
        p.setReviewCount(0);
        p.setStatus(ProblemStatus.ACTIVE);
        scheduleNextReview(p, now());
        return problemRepo.save(p);
    }
    public Problem getByNameOrThrow(User user, String name) {
        return problemRepo.findByNameAndUser(name.trim(), user)
                .orElseThrow(() -> new NoSuchElementException("문제 미존재(이름)"));
    }

    public Problem getByNumberOrThrow(User user, Integer number) {
        var list = problemRepo.findAllByNumberAndUser(number, user);
        if (list.isEmpty()) throw new NoSuchElementException("문제 미존재(번호)");
        if (list.size() > 1) throw new IllegalStateException("해당 번호가 여러 개입니다. 이름으로 지정해 주세요.");
        return list.get(0);
    }

    public List<Problem> listToday(User user) {
        log.debug("listToday userId={}", user != null ? user.getId() : null);
        return problemRepo.findByUserAndStatusAndNextReviewDateLessThanEqualOrderByReviewStepDesc(user, ProblemStatus.ACTIVE, now());
    }

    public List<Problem> listAllActiveOrderByDate(User user) {
        log.debug("listAllActiveOrderByDate userId={}", user != null ? user.getId() : null);
        return problemRepo.findByUserAndStatusOrderByNextReviewDateAsc(user, ProblemStatus.ACTIVE);
    }
    public Problem solve(User user, String name) {
        log.debug("solve userId={} name={}", user != null ? user.getId() : null, name);
        Problem p = getByNameOrThrow(user, name);
        // 하루 1회 중복 처리 방지 (Solve)
        if (logRepo.existsByProblemAndActionDateAndAction(p, today(), ReviewAction.SOLVE)) {
            throw new IllegalStateException("오늘은 이미 SOLVE 처리되었습니다.");
        }
        var beforeStep = p.getReviewStep();
        var beforeCount = p.getReviewCount();

        if (p.getReviewStep() >= 3) {
            p.graduate();
        } else {
            p.setReviewStep(p.getReviewStep() + 1);
            p.setReviewCount(0);
            scheduleNextReview(p, now());
        }

        writeLog(p, ReviewAction.SOLVE, beforeStep, beforeCount);
        return p;
    }

    public Problem fail(User user, String name) {
        log.debug("fail userId={} name={}", user != null ? user.getId() : null, name);
        Problem p = getByNameOrThrow(user, name);
        // 하루 1회 중복 처리 방지 (Fail)
        if (logRepo.existsByProblemAndActionDateAndAction(p, today(), ReviewAction.FAIL)) {
            throw new IllegalStateException("오늘은 이미 FAIL 처리되었습니다.");
        }
        var beforeStep = p.getReviewStep();
        var beforeCount = p.getReviewCount();

        // 실패는 졸업하지 않음: 같은 레벨 유지
        p.setReviewCount(p.getReviewCount() + 1);
        scheduleNextReviewOnFail(p, now());

        writeLog(p, ReviewAction.FAIL, beforeStep, beforeCount);
        return p;
    }

    private void writeLog(Problem p, ReviewAction action, int beforeStep, int beforeCount) {
        logRepo.save(ReviewLog.builder()
                .problem(p)
                .action(action)
                .actionDate(today())
                .beforeStep(beforeStep)
                .beforeReviewCount(beforeCount)
                .afterStep(p.getReviewStep())
                .afterReviewCount(p.getReviewCount())
                .build());
    }

    public Optional<Problem> findOptionalByNumber(User user, int number) {
        return problemRepo.findByNumberAndUser(number, user);
    }
    @Transactional
    public void deleteByName(User user, String name) {
        log.debug("deleteByName userId={} name={}", user != null ? user.getId() : null, name);
        Problem problem = problemRepo.findByNameAndUser(name, user)
                .orElseThrow(() -> new NoSuchElementException("해당 이름의 문제가 없습니다."));
        problemRepo.delete(problem);
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

    @Transactional
    public Problem graduate(User user, String name) {
        log.debug("graduate userId={} name={}", user != null ? user.getId() : null, name);
        Problem p = getByNameOrThrow(user, name);

        // 이미 졸업한 경우, 아무 작업도 하지 않고 반환 (선택적 방어 코드)
        if (p.getStatus() == ProblemStatus.GRADUATED) {
            return p;
        }

        var beforeStep = p.getReviewStep();
        var beforeCount = p.getReviewCount();

        p.graduate();

        writeLog(p, ReviewAction.SOLVE, beforeStep, beforeCount);
        return p;
    }

}
