package com.example.ctreview.service;

import com.example.ctreview.entity.*;
import com.example.ctreview.repository.ProblemRepository;
import com.example.ctreview.repository.ReviewLogRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.time.Clock;

@Service
@RequiredArgsConstructor
@Transactional
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
    // ReviewService.java (가정)
    @Transactional
// 순서를 (번호, 이름, 카테고리, 레벨)로 수정
    public Problem createProblem(Integer number, String name, ProblemCategory category, ProblemDifficulty difficulty) {
        if (problemRepo.existsByName(name.trim())) throw new IllegalStateException("이미 존재하는 문제 이름");
        var p = new Problem();
        p.setNumber(number);
        p.setName(name.trim());
        p.setCategory(category);
        p.setDifficulty(difficulty);
        p.setReviewStep(1);
        p.setReviewCount(0);
        scheduleNextReview(p, now());
        return problemRepo.save(p);
    }
    public Problem getByNameOrThrow(String name) {
        return problemRepo.findByName(name.trim())
                .orElseThrow(() -> new NoSuchElementException("문제 미존재(이름)"));
    }

    public Problem getByNumberOrThrow(Integer number) {
        var list = problemRepo.findAllByNumber(number);
        if (list.isEmpty()) throw new NoSuchElementException("문제 미존재(번호)");
        if (list.size() > 1) throw new IllegalStateException("해당 번호가 여러 개입니다. 이름으로 지정해 주세요.");
        return list.get(0);
    }

    public List<Problem> listToday() {
        return problemRepo.findByStatusAndNextReviewDateLessThanEqualOrderByReviewStepDesc(ProblemStatus.ACTIVE, now());
    }

    public List<Problem> listAllActiveOrderByDate() {
        return problemRepo.findByStatusOrderByNextReviewDateAsc(ProblemStatus.ACTIVE);
    }
    public Problem solve(String name) {
        Problem p = getByNameOrThrow(name);
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

    public Problem fail(String name) { // 파라미터를 int number -> String name 으로 변경
        Problem p = getByNameOrThrow(name); // 문제 검색 로직을 getByNameOrThrow로 변경
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
    private Problem findByNumberOrThrow(int number) {
        return problemRepo.findByNumber(number)
                .orElseThrow(() -> new NoSuchElementException("해당 번호의 문제가 없습니다."));
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
    public Optional<Problem> findOptionalByNumber(int number) {
        return problemRepo.findByNumber(number);
    }
    @Transactional
    public void deleteByName(String Name) {
        Problem problem = problemRepo.findByName(Name)
                .orElseThrow(() -> new NoSuchElementException("해당 이름의 문제가 없습니다."));
        // 문제 삭제
        problemRepo.delete(problem);
    }

    private void scheduleNextReview(Problem p, LocalDateTime base) {
        int[] intervals = reviewPolicy.intervals(p.getReviewStep());
        if (intervals.length == 0) {
            p.graduate();
            return;
        }
        var unit = reviewPolicy.unit();
        if (p.getReviewCount() < intervals.length) {
            int amt = intervals[p.getReviewCount()];
            p.setNextReviewDate(base.plus(amt, unit));
            p.setStatus(ProblemStatus.ACTIVE);
        } else {
            int last = intervals[intervals.length - 1];
            p.setNextReviewDate(base.plus(last, unit));
            p.setStatus(ProblemStatus.ACTIVE);
        }
    }
    private void scheduleNextReviewOnFail(Problem p, LocalDateTime base) {
        int[] intervals = reviewPolicy.intervals(p.getReviewStep());
        var unit = reviewPolicy.unit();
        if (intervals.length == 0) {
            p.setNextReviewDate(base.plus(1, unit));
            p.setStatus(ProblemStatus.ACTIVE);
            return;
        }
        if (p.getReviewCount() < intervals.length) {
            int amt = intervals[p.getReviewCount()];
            p.setNextReviewDate(base.plus(amt, unit));
            p.setStatus(ProblemStatus.ACTIVE);
        } else {
            int last = intervals[intervals.length - 1];
            p.setNextReviewDate(base.plus(last * 2L, unit));
            p.setReviewCount(intervals.length);
            p.setStatus(ProblemStatus.ACTIVE);
        }
    }

    @Transactional
    public Problem graduate(String name) {
        Problem p = getByNameOrThrow(name);

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
