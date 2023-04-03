package school.hei.haapi.endpoint.rest.validator;

import org.springframework.stereotype.Component;
import school.hei.haapi.endpoint.rest.model.CreateDelayPenaltyChange;
import school.hei.haapi.model.exception.BadRequestException;

import java.util.function.Consumer;

@Component
public class CreateDelayPenaltyValidator implements Consumer<CreateDelayPenaltyChange> {
  @Override public void accept(CreateDelayPenaltyChange createDelayPenaltyChange) {
    if (createDelayPenaltyChange.getInterestPercent() != null && createDelayPenaltyChange.getInterestPercent() < 0) {
      throw new BadRequestException("InterestPercent can't be below 0");
    }
    else if (createDelayPenaltyChange.getGraceDelay() != null && createDelayPenaltyChange.getGraceDelay() < 0) {
      throw new BadRequestException("GraceDelay can't be below 0");
    }
    else if (createDelayPenaltyChange.getApplicabilityDelayAfterGrace() != null && createDelayPenaltyChange.getApplicabilityDelayAfterGrace() < 0) {
      throw new BadRequestException("ApplicabilityDelayAfterGrace can't be null or below 0");
    }
  }
}