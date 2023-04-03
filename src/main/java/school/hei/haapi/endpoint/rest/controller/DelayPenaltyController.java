package school.hei.haapi.endpoint.rest.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import school.hei.haapi.endpoint.rest.mapper.DelayPenaltyMapper;
import school.hei.haapi.endpoint.rest.model.CreateDelayPenaltyChange;
import school.hei.haapi.endpoint.rest.model.DelayPenalty;
import school.hei.haapi.service.DelayPenaltyService;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toUnmodifiableList;


@AllArgsConstructor
@RestController
public class DelayPenaltyController {
    private final DelayPenaltyService delayPenaltyService;
    private final DelayPenaltyMapper delayPenaltyMapper;

    @GetMapping("/delay_penalty")
    public List<DelayPenalty> getDelayPenalty() {
        return delayPenaltyService.getDelayPenalty().stream()
                .map(delayPenaltyMapper::toRest)
                .collect(toUnmodifiableList());
    }

    @PutMapping("/delay_penalty_change")
    public List<school.hei.haapi.endpoint.rest.model.DelayPenalty> createDelayPenaltyChange(@RequestBody List<CreateDelayPenaltyChange> createDelayPenaltyChanges) {
        return delayPenaltyService.createDelayPenaltyChange(createDelayPenaltyChanges.stream()
                .map(delayPenaltyMapper::toDomain).collect(Collectors.toUnmodifiableList()))
                .stream().map(delayPenaltyMapper::toRest).collect(Collectors.toUnmodifiableList());

    }
}
