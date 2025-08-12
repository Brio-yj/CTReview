package com.example.ctreview.controller;

import com.example.ctreview.dto.ActionResultDto;
import com.example.ctreview.dto.ProblemCreateRequest;
import com.example.ctreview.dto.ProblemDto;
import com.example.ctreview.entity.Problem;
import com.example.ctreview.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProblemController {

    private final ReviewService reviewService;

    @PostMapping("/problems")
    public ProblemDto create(@Valid @RequestBody ProblemCreateRequest req) {
        return ProblemDto.from(reviewService.createProblem(req.number(), req.name(), req.category(), req.level()));
    }

    @GetMapping("/reviews/today")
    public List<ProblemDto> today() {
        return reviewService.listToday().stream().map(ProblemDto::from).toList();
    }

    // 임의 복습(오늘이 아니어도 가능): Solve


    @GetMapping("/problems/active")
    public List<ProblemDto> allActive() {
        return reviewService.listAllActiveOrderByDate().stream().map(ProblemDto::from).toList();
    }
    // Solve/Fail: number 또는 name 중 하나
    @PostMapping("/problems/solve")
    public ActionResultDto solve(@RequestParam(required=false) Integer number,
                                    @RequestParam(required=false) String name) {
        Problem p = (name != null && !name.isBlank())
                ? reviewService.solve(reviewService.getByNameOrThrow(name).getNumber())
                : reviewService.solve(reviewService.getByNumberOrThrow(number).getNumber());
        return ActionResultDto.of("SOLVE 완료", ProblemDto.from(p));
    }

    @PostMapping("/problems/fail")
    public ActionResultDto fail(@RequestParam(required=false) Integer number,
                                @RequestParam(required=false) String name) {
        Problem p = (name != null && !name.isBlank())
                ? reviewService.fail(reviewService.getByNameOrThrow(name).getNumber())
                : reviewService.fail(reviewService.getByNumberOrThrow(number).getNumber());
        return ActionResultDto.of("FAIL 처리", ProblemDto.from(p));
    }

    @DeleteMapping("/problems")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam(required=false) Integer number,
                        @RequestParam(required=false) String name) {
        Problem p = (name != null && !name.isBlank())
                ? reviewService.getByNameOrThrow(name)
                : reviewService.getByNumberOrThrow(number);
        reviewService.deleteByName(name); // 혹은 svc.deleteById(p.getId())
    }
}
