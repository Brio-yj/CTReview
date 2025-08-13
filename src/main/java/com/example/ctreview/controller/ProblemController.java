package com.example.ctreview.controller;

import com.example.ctreview.dto.ActionResultDto;
import com.example.ctreview.dto.ProblemCreateRequest;
import com.example.ctreview.dto.ProblemDto;
import com.example.ctreview.entity.Problem;
import com.example.ctreview.entity.User;
import com.example.ctreview.service.AuthService;
import com.example.ctreview.service.ReviewService;
import com.example.ctreview.service.SessionReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ProblemController {

    private final ReviewService reviewService;
    private final AuthService authService;
    private final SessionReviewService sessionReviewService;

    @PostMapping("/problems")
    public ProblemDto create(HttpSession session, @Valid @RequestBody ProblemCreateRequest req) {
        User user = authService.getCurrentUser(session);
        log.debug("Create problem userId={} number={} name={}", user != null ? user.getId() : null, req.number(), req.name());

        return ProblemDto.from(reviewService.createProblem(user, req.number(), req.name(), req.category(), req.difficulty()));

    }

    @GetMapping("/reviews/today")
    public List<ProblemDto> today(HttpSession session) {
        User user = authService.getCurrentUser(session);
        log.debug("List today problems userId={}", user != null ? user.getId() : null);

        return reviewService.listToday(user).stream().map(ProblemDto::from).toList();

    }

    @GetMapping("/problems/active")
    public List<ProblemDto> allActive(HttpSession session) {
        User user = authService.getCurrentUser(session);
        log.debug("List active problems userId={}", user != null ? user.getId() : null);

        return reviewService.listAllActiveOrderByDate(user).stream().map(ProblemDto::from).toList();

    }

    @PostMapping("/problems/solve")
    public ActionResultDto solve(HttpSession session, @RequestParam String name) {
        User user = authService.getCurrentUser(session);
        log.debug("Solve problem userId={} name={}", user != null ? user.getId() : null, name);

        Problem p = reviewService.solve(user, name);

        return ActionResultDto.of("SOLVE 완료", ProblemDto.from(p));
    }

    @PostMapping("/problems/fail")
    public ActionResultDto fail(HttpSession session, @RequestParam String name) {
        User user = authService.getCurrentUser(session);
        log.debug("Fail problem userId={} name={}", user != null ? user.getId() : null, name);

        Problem p = reviewService.fail(user, name);

        return ActionResultDto.of("FAIL 처리", ProblemDto.from(p));
    }

    @PostMapping("/problems/graduate")
    public ActionResultDto graduate(HttpSession session, @RequestParam String name) {
        User user = authService.getCurrentUser(session);
        log.debug("Graduate problem userId={} name={}", user != null ? user.getId() : null, name);

        Problem p = reviewService.graduate(user, name);

        return ActionResultDto.of("GRADUATE", ProblemDto.from(p));
    }

    @DeleteMapping("/problems")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(HttpSession session,
                       @RequestParam(required=false) Integer number,
                       @RequestParam(required=false) String name) {
        User user = authService.getCurrentUser(session);
        log.debug("Delete problem userId={} number={} name={}", user != null ? user.getId() : null, number, name);

        Problem p = (name != null && !name.isBlank())
                ? reviewService.getByNameOrThrow(user, name)
                : reviewService.getByNumberOrThrow(user, number);
        reviewService.deleteByName(user, p.getName());

    }
}
