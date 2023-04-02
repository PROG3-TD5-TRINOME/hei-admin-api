package school.hei.haapi.endpoint.rest.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.AllArgsConstructor;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import school.hei.haapi.endpoint.rest.mapper.FeeMapper;
import school.hei.haapi.endpoint.rest.model.CreateFee;
import school.hei.haapi.endpoint.rest.model.Fee;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.DelayPenalty;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.service.FeeService;

import static java.util.stream.Collectors.toUnmodifiableList;

@RestController
@AllArgsConstructor
public class FeeController {

  private final FeeService feeService;
  private final FeeMapper feeMapper;

  private final FeeService.ConfigurationService configurationService;

  @GetMapping("/students/{studentId}/fees/{feeId}")
  public Fee getFeeByStudentId(
      @PathVariable String studentId,
      @PathVariable String feeId) {
    return feeMapper.toRestFee(feeService.getByStudentIdAndFeeId(studentId, feeId));
  }

  @PostMapping("/students/{studentId}/fees")
  public List<Fee> createFees(
      @PathVariable String studentId, @RequestBody List<CreateFee> toCreate) {
    return feeService.saveAll(
            feeMapper.toDomainFee(studentId, toCreate)).stream()
        .map(feeMapper::toRestFee)
        .collect(toUnmodifiableList());
  }

  @GetMapping("/students/{studentId}/fees")
  public List<Fee> getFeesByStudentId(
          @PathVariable String studentId,
          @RequestParam PageFromOne page,
          @RequestParam("page_size") BoundedPageSize pageSize,
          @RequestParam(required = false) Fee.StatusEnum status) {
    List<school.hei.haapi.model.Fee> fees = feeService.getFeesByStudentId(studentId, page, pageSize, status);

    for (school.hei.haapi.model.Fee fee : fees) {
      if (fee.getRemainingAmount() > 0 && fee.getStatus() == Fee.StatusEnum.LATE) {
        LocalDate dueDate = LocalDate.parse(fee.getDueDatetime().toString());
        LocalDate currentDate = LocalDate.now();
        int gracePeriod = configurationService.getGracePeriod();
        double lateFeeRate = configurationService.getLateFeeRate();
        LocalDate gracePeriodEndDate = dueDate.plusDays(gracePeriod);
        LocalDate lateFeeStartDate = gracePeriodEndDate.plusDays(1);
        LocalDate lateFeeEndDate = lateFeeStartDate.plusDays(5); // assume 5 days late fee period

        if (currentDate.isAfter(lateFeeStartDate) && currentDate.isBefore(lateFeeEndDate)) {
          double lateFee = fee.getRemainingAmount() * lateFeeRate;
          fee.setTotalAmount((int) (fee.getTotalAmount() + lateFee));
        }
      }
    }

    return fees.stream()
            .map(feeMapper::toRestFee)
            .collect(toUnmodifiableList());
  }


  @GetMapping("/fees")
  public List<Fee> getFees(
      @RequestParam PageFromOne page,
      @RequestParam("page_size") BoundedPageSize pageSize,
      @RequestParam(required = false) Fee.StatusEnum status) {
    return feeService.getFees(page, pageSize, status).stream()
        .map(feeMapper::toRestFee)
        .collect(toUnmodifiableList());
  }
}
