package school.hei.haapi.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import school.hei.haapi.endpoint.rest.mapper.DelayPenaltyMapper;
import school.hei.haapi.endpoint.rest.model.CreateDelayPenaltyChange;
import school.hei.haapi.model.DelayPenalty;
import school.hei.haapi.model.Fee;
import school.hei.haapi.repository.DelayPenaltyRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DelayPenaltyService {
    private final DelayPenaltyRepository repository;
    private final DelayPenaltyMapper delayPenaltyMapper;
    public DelayPenalty getDelayPenalty() {
        return repository.findAll().get(0);
    }

    public DelayPenalty createDelayPenaltyChange(CreateDelayPenaltyChange createDelayPenaltyChange) {
        return delayPenaltyMapper.toDomain(createDelayPenaltyChange);
    }
}
