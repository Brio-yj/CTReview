package com.example.ctreview.service;

import com.example.ctreview.entity.*;
import com.example.ctreview.repository.ProblemRepository;
import com.example.ctreview.repository.ReviewLogRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
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
        return LocalDate.now(ZoneId.of("Asia/Seoul"));
    }
    // ReviewService.java (가정)
    @Transactional
// 순서를 (번호, 이름, 카테고리, 레벨)로 수정
    public Problem createProblem(Integer number, String name, ProblemCategory category, int level) {
        if (problemRepo.existsByName(name.trim())) throw new IllegalStateException("이미 존재하는 문제 이름");
        var p = new Problem();
        p.setNumber(number);
        p.setName(name.trim());
        p.setCategory(category);
        p.setCurrentLevel(level);
        p.setReviewCount(0);
        scheduleNextReview(p, LocalDate.now(clock));
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
        return problemRepo.findByNextReviewDateAndStatusOrderByCurrentLevelDesc(today(), ProblemStatus.ACTIVE);
    }

    public List<Problem> listAllActiveOrderByDate() {
        return problemRepo.findByStatusOrderByNextReviewDateAsc(ProblemStatus.ACTIVE);
    }
    public Problem solve(int number) {
        Problem p = findByNumberOrThrow(number);
        // 하루 1회 중복 처리 방지 (Solve)
        if (logRepo.existsByProblemAndActionDateAndAction(p, today(), ReviewAction.SOLVE)) {
            throw new IllegalStateException("오늘은 이미 SOLVE 처리되었습니다.");
        }
        var beforeLevel = p.getCurrentLevel();
        var beforeCount = p.getReviewCount();

        if (p.getCurrentLevel() == 1) {
            p.graduate();
        } else {
            p.setCurrentLevel(p.getCurrentLevel() - 1);
            p.setReviewCount(0);
            scheduleNextReview(p, today());
        }

        writeLog(p, ReviewAction.SOLVE, beforeLevel, beforeCount);
        return p;
    }

    public Problem fail(int number) {
        Problem p = findByNumberOrThrow(number);
        // 하루 1회 중복 처리 방지 (Fail)
        if (logRepo.existsByProblemAndActionDateAndAction(p, today(), ReviewAction.FAIL)) {
            throw new IllegalStateException("오늘은 이미 FAIL 처리되었습니다.");
        }
        var beforeLevel = p.getCurrentLevel();
        var beforeCount = p.getReviewCount();

        // 실패는 졸업하지 않음: 같은 레벨 유지
        p.setReviewCount(p.getReviewCount() + 1);
        scheduleNextReviewOnFail(p, today());

        writeLog(p, ReviewAction.FAIL, beforeLevel, beforeCount);
        return p;
    }

    public Problem forceGraduate(int number) {
        Problem p = findByNumberOrThrow(number);
        var beforeLevel = p.getCurrentLevel();
        var beforeCount = p.getReviewCount();
        p.graduate();
        writeLog(p, ReviewAction.SOLVE, beforeLevel, beforeCount);
        return p;
    }
    private Problem findByNumberOrThrow(int number) {
        return problemRepo.findByNumber(number)
                .orElseThrow(() -> new NoSuchElementException("해당 번호의 문제가 없습니다."));
    }

    private void writeLog(Problem p, ReviewAction action, int beforeLevel, int beforeCount) {
        logRepo.save(ReviewLog.builder()
                .problem(p)
                .action(action)
                .actionDate(today())
                .beforeLevel(beforeLevel)
                .beforeReviewCount(beforeCount)
                .afterLevel(p.getCurrentLevel())
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

    private void scheduleNextReview(Problem p, LocalDate base) {
        int[] intervals = reviewPolicy.intervals(p.getCurrentLevel());
        if (intervals.length == 0) {
            p.graduate();
            return;
        }
        if (p.getReviewCount() < intervals.length) {
            int days = intervals[p.getReviewCount()];
            p.setNextReviewDate(base.plusDays(days));
            p.setStatus(ProblemStatus.ACTIVE);
        } else {
            // 원본 콘솔과 달리: Solve 외에는 졸업하지 않음
            // Fail로 인해 회차가 간격을 초과해도 여기서 졸업 금지 → 마지막 간격 유지(다음 로직에서 보정)
            int last = intervals[intervals.length - 1];
            p.setNextReviewDate(base.plusDays(last));
            p.setStatus(ProblemStatus.ACTIVE);
        }
    }
    private void scheduleNextReviewOnFail(Problem p, LocalDate base) {
        int[] intervals = reviewPolicy.intervals(p.getCurrentLevel());
        if (intervals.length == 0) {
            // 이 케이스는 레벨 0이거나 정책 미설정 — 안전하게 오늘+1일로
            p.setNextReviewDate(base.plusDays(1));
            p.setStatus(ProblemStatus.ACTIVE);
            return;
        }
        if (p.getReviewCount() < intervals.length) {
            int days = intervals[p.getReviewCount()];
            p.setNextReviewDate(base.plusDays(days));
            p.setStatus(ProblemStatus.ACTIVE);
        } else {
            int last = intervals[intervals.length - 1];
            // 실패가 간격 끝을 초과했으므로 last*2
            p.setNextReviewDate(base.plusDays(last * 2L));
            // reviewCount를 intervals.length로 캡핑해 과도 증가 방지
            p.setReviewCount(intervals.length);
            p.setStatus(ProblemStatus.ACTIVE);
        }
    }

}
