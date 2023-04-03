package school.hei.haapi.endpoint.rest.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.haapi.endpoint.rest.model.CreateDelayPenaltyChange;
import school.hei.haapi.endpoint.rest.model.DelayPenalty;
import school.hei.haapi.endpoint.rest.validator.CreateDelayPenaltyValidator;
import school.hei.haapi.model.exception.BadRequestException;
import school.hei.haapi.repository.DelayPenaltyRepository;
import school.hei.haapi.service.UserService;

@Component
@AllArgsConstructor
public class DelayPenaltyMapper {

    private final DelayPenaltyRepository delayPenaltyRepository;

    private final UserService userService;
    private final CreateDelayPenaltyValidator createDelayPenaltyValidator;
    public school.hei.haapi.model.DelayPenalty toDomain(CreateDelayPenaltyChange createDelayPenaltyChange) {
        createDelayPenaltyValidator.accept(createDelayPenaltyChange);
        school.hei.haapi.model.DelayPenalty current = delayPenaltyRepository.findAll().get(0);
        return school.hei.haapi.model.DelayPenalty.builder()
                .id(createDelayPenaltyChange.getStudentId() == null ? current.getId() : null)
                .interestPercent(createDelayPenaltyChange.getInterestPercent() == null ? current.getInterestPercent() : createDelayPenaltyChange.getInterestPercent())
                .interestTimerate(toDomainDelayPenaltyEnum(createDelayPenaltyChange.getInterestTimerate()))
                .graceDelay(createDelayPenaltyChange.getGraceDelay() == null ? 0 : createDelayPenaltyChange.getGraceDelay())
                .applicabilityDelayAfterGrace(createDelayPenaltyChange.getApplicabilityDelayAfterGrace() == null ? 0 : createDelayPenaltyChange.getApplicabilityDelayAfterGrace())
                .student(userService.getById(createDelayPenaltyChange.getStudentId()))
                .build();
    }

    public DelayPenalty toRest(school.hei.haapi.model.DelayPenalty domain) {
        return new DelayPenalty()
                .id(domain.getId())
                .interestPercent((int) domain.getInterestPercent())
                .interestTimerate(domain.getInterestTimerate())
                .graceDelay(domain.getGraceDelay())
                .applicabilityDelayAfterGrace(domain.getApplicabilityDelayAfterGrace())
                .creationDatetime(domain.getCreationDatetime());
    }

    private DelayPenalty.InterestTimerateEnum toDomainDelayPenaltyEnum(CreateDelayPenaltyChange.InterestTimerateEnum createDelayPenaltyInterestTimerateEnum) {
        if (createDelayPenaltyInterestTimerateEnum == CreateDelayPenaltyChange.InterestTimerateEnum.DAILY) {
            return DelayPenalty.InterestTimerateEnum.DAILY;
        } else if (createDelayPenaltyInterestTimerateEnum.getValue().equals("DAILY")) {
            return DelayPenalty.InterestTimerateEnum.DAILY;
        } else {
            throw new BadRequestException("Unexpected delay penalty interest timerate");
        }
    }
}
