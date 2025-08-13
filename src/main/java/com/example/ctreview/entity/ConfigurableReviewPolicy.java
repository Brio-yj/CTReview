package com.example.ctreview.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.temporal.ChronoUnit;
@Component
@Setter
@Getter
@ConfigurationProperties(prefix = "review")
class ReviewConfigProps {
    private Map<Integer, List<Integer>> steps = new HashMap<>();
}
@Component
@RequiredArgsConstructor
public class ConfigurableReviewPolicy implements ReviewPolicy {
    private final ReviewConfigProps props;

    @Override
    public int[] intervals(int step) {
        var list = props.getSteps().getOrDefault(step,List.of());
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public ChronoUnit unit() {
        return ChronoUnit.valueOf(props.getUnit().toUpperCase());
    }
}

