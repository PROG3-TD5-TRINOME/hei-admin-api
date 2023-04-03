package school.hei.haapi.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import school.hei.haapi.endpoint.event.EventProducer;
import school.hei.haapi.endpoint.event.model.TypedLateFeeVerified;
import school.hei.haapi.endpoint.event.model.gen.LateFeeVerified;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.Fee;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.model.validator.FeeValidator;
import school.hei.haapi.repository.FeeRepository;

import static org.springframework.data.domain.Sort.Direction.DESC;
import static school.hei.haapi.endpoint.rest.model.Fee.StatusEnum.LATE;
import static school.hei.haapi.endpoint.rest.model.Fee.StatusEnum.PAID;

@Service
@AllArgsConstructor
@Slf4j
public class FeeService {

  private static final school.hei.haapi.endpoint.rest.model.Fee.StatusEnum DEFAULT_STATUS = LATE;
  private final FeeRepository feeRepository;
  private final FeeValidator feeValidator;

  private final EventProducer eventProducer;
  private final DelayPenaltyService delayPenaltyService;

  public Fee getById(String id) {
    return updateFeeStatus(feeRepository.getById(id));
  }

  public Fee getByStudentIdAndFeeId(String studentId, String feeId) {
    return updateFeeStatus(feeRepository.getByStudentIdAndId(studentId, feeId));
  }

  @Transactional
  public List<Fee> saveAll(List<Fee> fees) {
    feeValidator.accept(fees);
    return feeRepository.saveAll(fees);
  }

  public List<Fee> getFees(
      PageFromOne page, BoundedPageSize pageSize,
      school.hei.haapi.endpoint.rest.model.Fee.StatusEnum status) {
    Pageable pageable =
        PageRequest.of(page.getValue() - 1, pageSize.getValue(), Sort.by(DESC, "dueDatetime"));
    if (status != null) {
      return feeRepository.getFeesByStatus(status, pageable);
    }
    return feeRepository.getFeesByStatus(DEFAULT_STATUS, pageable);
  }

  public List<Fee> getFeesByStudentId(
      String studentId, PageFromOne page, BoundedPageSize pageSize,
      school.hei.haapi.endpoint.rest.model.Fee.StatusEnum status) {
    Pageable pageable = PageRequest.of(
        page.getValue() - 1,
        pageSize.getValue(),
        Sort.by(DESC, "dueDatetime"));
    int gracePeriod = delayPenaltyService.getDelayPenalty().get(0).getGraceDelay();
    double interestPercent = delayPenaltyService.getDelayPenalty().get(0).getInterestPercent();
    int applicabilityDelaysAfterGrace = delayPenaltyService.getDelayPenalty().get(0).getApplicabilityDelayAfterGrace();


    if (status != null) {
      List<school.hei.haapi.model.Fee> fees = feeRepository.getFeesByStudentIdAndStatus(studentId, status, pageable);
      for (school.hei.haapi.model.Fee fee : fees) {
        if (fee.getRemainingAmount() > 0 && fee.getStatus() == school.hei.haapi.endpoint.rest.model.Fee.StatusEnum.LATE) {
          LocalDate currentDate = LocalDate.now();
          ZonedDateTime zonedDateTime = fee.getDueDatetime().atZone(ZoneId.systemDefault());
          int dayOfMonth = zonedDateTime.getDayOfMonth();
          LocalDate applicableInterest = LocalDate.of(currentDate.getYear(),currentDate.getMonth(),(dayOfMonth+gracePeriod));
          LocalDate applicabilityDelayAfterGrace = LocalDate.of(applicableInterest.getYear(),applicableInterest.getMonth(),(applicableInterest.getDayOfMonth() + applicabilityDelaysAfterGrace));
          long daysBetween = ChronoUnit.DAYS.between(applicableInterest,applicabilityDelayAfterGrace);
          if(daysBetween>0){
            double lateFee = fee.getRemainingAmount() * interestPercent;
            fee.setRemainingAmount((int) (fee.getTotalAmount()+ lateFee));
          }
        }
      }
      return fees;
    }
    return feeRepository.getByStudentId(studentId, pageable);
  }

  private Fee updateFeeStatus(Fee initialFee) {
    if (initialFee.getRemainingAmount() == 0) {
      initialFee.setStatus(PAID);
    } else if (Instant.now().isAfter(initialFee.getDueDatetime())) {
      initialFee.setStatus(LATE);
     }
    return initialFee;
  }

  @Scheduled(cron = "0 0 * * * *")
  public void updateFeesStatusToLate() {
    List<Fee> unpaidFees = feeRepository.getUnpaidFees();
    unpaidFees.forEach(fee -> {
      updateFeeStatus(fee);
      log.info("Fee with id." + fee.getId() + " is going to be updated from UNPAID to LATE");
    });
    feeRepository.saveAll(unpaidFees);
  }

  private TypedLateFeeVerified toTypedEvent(Fee fee) {
    return new TypedLateFeeVerified(
        LateFeeVerified.builder()
            .type(fee.getType())
            .student(fee.getStudent())
            .comment(fee.getComment())
            .remainingAmount(fee.getRemainingAmount())
            .dueDatetime(fee.getDueDatetime())
            .build()
    );
  }

  /*
   * An email will be sent to user with late fees
   * every morning at 8am (UTC+3)
   * */
  @Scheduled(cron = "0 0 8 * * *")
  public void sendLateFeesEmail() {
    List<Fee> lateFees = feeRepository.getFeesByStatus(LATE);
    lateFees.forEach(
        fee -> {
          eventProducer.accept(List.of(toTypedEvent(fee)));
          log.info("Late Fee with id." + fee.getId() + " is sent to Queue");
        }
    );
  }

}
