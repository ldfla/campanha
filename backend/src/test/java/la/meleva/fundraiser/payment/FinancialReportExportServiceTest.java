package la.meleva.fundraiser.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class FinancialReportExportServiceTest {
    @Test
    void neutralizesFormulasControlCharactersAndQuotes() {
        assertEquals("\"'=1+2\"", FinancialReportExportService.cell("=1+2"));
        assertEquals("\"'  @cmd\"", FinancialReportExportService.cell("  @cmd"));
        assertEquals("\"'\ttexto\"", FinancialReportExportService.cell("\ttexto"));
        assertEquals("\"'-400\"", FinancialReportExportService.cell("-400"));
        assertEquals("\"a\"\"b\"", FinancialReportExportService.cell("a\"b"));
    }
}
