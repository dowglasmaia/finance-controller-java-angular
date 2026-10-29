package com.dowglasmaia.financecontroller.report.api;

import com.dowglasmaia.financecontroller.generated.reports.api.ReportsApi;
import com.dowglasmaia.financecontroller.generated.reports.model.Currency;
import com.dowglasmaia.financecontroller.generated.reports.model.Report;
import com.dowglasmaia.financecontroller.generated.reports.model.ReportPeriod;
import com.dowglasmaia.financecontroller.report.application.usecase.GetReportUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Só mapeia e delega; as contas vivem no caso de uso. */
@RestController
public class ReportsController implements ReportsApi {

    private final GetReportUseCase getReport;

    public ReportsController(GetReportUseCase getReport) {
        this.getReport = getReport;
    }

    @Override
    public ResponseEntity<Report> getReport(ReportPeriod period, LocalDate anchorDate, Currency currency,
                                            Integer rankingSize) {
        var result = getReport.execute(
            com.dowglasmaia.financecontroller.report.domain.ReportPeriod.valueOf(period.getValue()), anchorDate,
            com.dowglasmaia.financecontroller.shared.money.Currency.valueOf(currency.getValue()), rankingSize);
        return ResponseEntity.ok(ReportsApiMapper.toResponse(result));
    }
}
