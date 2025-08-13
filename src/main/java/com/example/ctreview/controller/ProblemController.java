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
        Problem p = (user != null)
                ? reviewService.createProblem(user, req.number(), req.name(), req.category(), req.difficulty())
                : sessionReviewService.create(session, req.number(), req.name(), req.category(), req.difficulty());
        return ProblemDto.from(p);
    }

    @GetMapping("/reviews/today")
    public List<ProblemDto> today(HttpSession session) {
        User user = authService.getCurrentUser(session);
        log.debug("List today problems userId={}", user != null ? user.getId() : null);
        var list = (user != null)
                ? reviewService.listToday(user)
                : sessionReviewService.listToday(session);
        return list.stream().map(ProblemDto::from).toList();
    }

    @GetMapping("/problems/active")
    public List<ProblemDto> allActive(HttpSession session) {
        User user = authService.getCurrentUser(session);
        log.debug("List active problems userId={}", user != null ? user.getId() : null);
        var list = (user != null)
                ? reviewService.listAllActiveOrderByDate(user)
                : sessionReviewService.listAll(session);
        return list.stream().map(ProblemDto::from).toList();
    }

    @PostMapping("/problems/solve")
    public ActionResultDto solve(HttpSession session, @RequestParam String name) {
        User user = authService.getCurrentUser(session);
        log.debug("Solve problem userId={} name={}", user != null ? user.getId() : null, name);
        Problem p = (user != null) ? reviewService.solve(user, name) : sessionReviewService.solve(session, name);
        return ActionResultDto.of("SOLVE 완료", ProblemDto.from(p));
    }

    @PostMapping("/problems/fail")
    public ActionResultDto fail(HttpSession session, @RequestParam String name) {
        User user = authService.getCurrentUser(session);
        log.debug("Fail problem userId={} name={}", user != null ? user.getId() : null, name);
        Problem p = (user != null) ? reviewService.fail(user, name) : sessionReviewService.fail(session, name);
        return ActionResultDto.of("FAIL 처리", ProblemDto.from(p));
    }

    @PostMapping("/problems/graduate")
    public ActionResultDto graduate(HttpSession session, @RequestParam String name) {
        User user = authService.getCurrentUser(session);
        log.debug("Graduate problem userId={} name={}", user != null ? user.getId() : null, name);
        Problem p = (user != null) ? reviewService.graduate(user, name) : sessionReviewService.graduate(session, name);
        return ActionResultDto.of("GRADUATE", ProblemDto.from(p));
    }

    @DeleteMapping("/problems")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(HttpSession session,
                       @RequestParam(required=false) Integer number,
                       @RequestParam(required=false) String name) {
        User user = authService.getCurrentUser(session);
        log.debug("Delete problem userId={} number={} name={}", user != null ? user.getId() : null, number, name);
        if (user != null) {
            Problem p = (name != null && !name.isBlank())
                    ? reviewService.getByNameOrThrow(user, name)
                    : reviewService.getByNumberOrThrow(user, number);
            reviewService.deleteByName(user, p.getName());
        } else if (name != null && !name.isBlank()) {
            sessionReviewService.delete(session, name);
        }
    }
}
