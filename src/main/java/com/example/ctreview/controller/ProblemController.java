package com.example.ctreview.controller;

import com.example.ctreview.dto.ActionResultDto;
import com.example.ctreview.dto.ProblemCreateRequest;
import com.example.ctreview.dto.ProblemDto;
import com.example.ctreview.entity.Problem;
import com.example.ctreview.entity.User;
import com.example.ctreview.service.AuthService;
import com.example.ctreview.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProblemController {

    private final ReviewService reviewService;
    private final AuthService authService;

    @PostMapping("/problems")

    public ProblemDto create(@Valid @RequestBody ProblemCreateRequest req) {
        return ProblemDto.from(reviewService.createProblem(req.number(), req.name(), req.category(), req.difficulty()));
    }

    @GetMapping("/reviews/today")
    public List<ProblemDto> today(HttpSession session) {
        User user = authService.getCurrentUser(session);
        return reviewService.listToday(user).stream().map(ProblemDto::from).toList();
    }

    @GetMapping("/problems/active")
    public List<ProblemDto> allActive(HttpSession session) {
        User user = authService.getCurrentUser(session);
        return reviewService.listAllActiveOrderByDate(user).stream().map(ProblemDto::from).toList();
    }

    @PostMapping("/problems/solve")
    public ActionResultDto solve(@RequestParam String name) {
        Problem p = reviewService.solve(name);
        return ActionResultDto.of("SOLVE 완료", ProblemDto.from(p));
    }

    @PostMapping("/problems/fail")
    public ActionResultDto fail(@RequestParam String name) {
        Problem p = reviewService.fail(name);
        return ActionResultDto.of("FAIL 처리", ProblemDto.from(p));
    }

    @PostMapping("/problems/graduate")
    public ActionResultDto graduate(@RequestParam String name) {
        Problem p = reviewService.graduate(name);
        return ActionResultDto.of("GRADUATE", ProblemDto.from(p));
    }

    @DeleteMapping("/problems")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(HttpSession session,
                       @RequestParam(required=false) Integer number,
                       @RequestParam(required=false) String name) {
        User user = authService.getCurrentUser(session);
        Problem p = (name != null && !name.isBlank())
                ? reviewService.getByNameOrThrow(user, name)
                : reviewService.getByNumberOrThrow(user, number);
        reviewService.deleteByName(user, p.getName());
    }
}
