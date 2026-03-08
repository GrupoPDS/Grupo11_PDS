package br.ufu.pds.library.infrastructure.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.ufu.pds.library.core.domain.*;
import br.ufu.pds.library.entrypoint.api.dto.OverdueItemResponse;
import br.ufu.pds.library.entrypoint.api.dto.OverdueReportSummaryResponse;
import br.ufu.pds.library.entrypoint.api.dto.PagedResponse;
import br.ufu.pds.library.infrastructure.persistence.LoanRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class OverdueReportServiceTest {

    @Mock private LoanRepository loanRepository;

    @InjectMocks private OverdueReportService overdueReportService;

    private User user1;
    private User user2;
    private Book book1;
    private Book book2;
    private Loan overdueLow; // 3 dias de atraso
    private Loan overdueMedium; // 10 dias de atraso
    private Loan overdueHigh; // 20 dias de atraso
    private Loan overdueCritical; // 45 dias de atraso

    @BeforeEach
    void setup() {
        user1 =
                User.builder()
                        .id(1L)
                        .name("Maria Silva")
                        .email("maria@ufu.br")
                        .role("STUDENT")
                        .build();
        user2 =
                User.builder()
                        .id(2L)
                        .name("João Santos")
                        .email("joao@ufu.br")
                        .role("STUDENT")
                        .build();

        book1 =
                Book.builder()
                        .id(1L)
                        .title("Clean Code")
                        .author("Robert Martin")
                        .isbn("978-0132350884")
                        .quantity(2)
                        .build();
        book2 =
                Book.builder()
                        .id(2L)
                        .title("Refactoring")
                        .author("Martin Fowler")
                        .isbn("978-0134757599")
                        .quantity(1)
                        .build();

        overdueLow =
                Loan.builder()
                        .id(1L)
                        .user(user1)
                        .book(book1)
                        .loanDate(LocalDate.now().minusDays(17))
                        .dueDate(LocalDate.now().minusDays(3))
                        .status(LoanStatus.OVERDUE)
                        .build();

        overdueMedium =
                Loan.builder()
                        .id(2L)
                        .user(user2)
                        .book(book2)
                        .loanDate(LocalDate.now().minusDays(24))
                        .dueDate(LocalDate.now().minusDays(10))
                        .status(LoanStatus.OVERDUE)
                        .build();

        overdueHigh =
                Loan.builder()
                        .id(3L)
                        .user(user1)
                        .book(book2)
                        .loanDate(LocalDate.now().minusDays(34))
                        .dueDate(LocalDate.now().minusDays(20))
                        .status(LoanStatus.OVERDUE)
                        .build();

        overdueCritical =
                Loan.builder()
                        .id(4L)
                        .user(user2)
                        .book(book1)
                        .loanDate(LocalDate.now().minusDays(59))
                        .dueDate(LocalDate.now().minusDays(45))
                        .status(LoanStatus.OVERDUE)
                        .build();
    }

    // ── getOverdueLoans ──

    @Test
    void getOverdueLoans_success_returnsPaginatedResults() {
        List<Loan> loans = List.of(overdueLow, overdueMedium);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Loan> page = new PageImpl<>(loans, pageable, loans.size());

        when(loanRepository.findAllOverduePaged(any(Pageable.class))).thenReturn(page);

        PagedResponse<OverdueItemResponse> result =
                overdueReportService.getOverdueLoans(null, null, 0, 10);

        assertEquals(2, result.getContent().size());
        assertEquals(0, result.getPage());
        assertEquals(2, result.getTotalElements());
        assertFalse(result.isHasNext());
        verify(loanRepository).findAllOverduePaged(any(Pageable.class));
        verify(loanRepository, never()).searchOverdueLoans(anyString());
    }

    @Test
    void getOverdueLoans_withSearch_filtersResults() {
        when(loanRepository.searchOverdueLoans("Maria"))
                .thenReturn(List.of(overdueLow, overdueHigh));

        PagedResponse<OverdueItemResponse> result =
                overdueReportService.getOverdueLoans("Maria", null, 0, 10);

        assertEquals(2, result.getContent().size());
        result.getContent().forEach(item -> assertEquals("Maria Silva", item.getUserName()));
        verify(loanRepository).searchOverdueLoans("Maria");
    }

    @Test
    void getOverdueLoans_emptyResult_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Loan> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(loanRepository.findAllOverduePaged(any(Pageable.class))).thenReturn(emptyPage);

        PagedResponse<OverdueItemResponse> result =
                overdueReportService.getOverdueLoans(null, null, 0, 10);

        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getOverdueLoans_paginationMetadata_isCorrect() {
        List<Loan> loans = List.of(overdueLow, overdueMedium);
        Pageable pageable = PageRequest.of(1, 2);
        Page<Loan> page = new PageImpl<>(loans, pageable, 6);

        when(loanRepository.findAllOverduePaged(any(Pageable.class))).thenReturn(page);

        PagedResponse<OverdueItemResponse> result =
                overdueReportService.getOverdueLoans(null, null, 1, 2);

        assertEquals(2, result.getContent().size());
        assertEquals(1, result.getPage());
        assertEquals(2, result.getSize());
        assertEquals(6, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertTrue(result.isHasNext());
    }

    // ── getOverdueSummary ──

    @Test
    void getOverdueSummary_success_calculatesCorrectly() {
        when(loanRepository.countByStatus(LoanStatus.OVERDUE)).thenReturn(4L);
        when(loanRepository.countByStatus(LoanStatus.ACTIVE)).thenReturn(6L);
        when(loanRepository.findAllOverdueWithDetails())
                .thenReturn(List.of(overdueLow, overdueMedium, overdueHigh, overdueCritical));

        OverdueReportSummaryResponse result = overdueReportService.getOverdueSummary();

        assertEquals(4, result.getTotalOverdue());
        assertEquals(10, result.getTotalActiveLoans()); // 4 overdue + 6 active
        assertEquals(40.0, result.getOverduePercentage(), 0.1); // 4/10 * 100
        assertTrue(result.getAverageDaysOverdue() > 0);
        assertEquals(1, result.getLowSeverity()); // 3 dias
        assertEquals(1, result.getMediumSeverity()); // 10 dias
        assertEquals(1, result.getHighSeverity()); // 20 dias
        assertEquals(1, result.getCriticalSeverity()); // 45 dias
        assertNotNull(result.getGeneratedAt());
    }

    @Test
    void getOverdueSummary_noOverdueLoans_returnsZeros() {
        when(loanRepository.countByStatus(LoanStatus.OVERDUE)).thenReturn(0L);
        when(loanRepository.countByStatus(LoanStatus.ACTIVE)).thenReturn(5L);

        OverdueReportSummaryResponse result = overdueReportService.getOverdueSummary();

        assertEquals(0, result.getTotalOverdue());
        assertEquals(5, result.getTotalActiveLoans());
        assertEquals(0.0, result.getOverduePercentage());
        assertEquals(0.0, result.getAverageDaysOverdue());
        assertEquals(0, result.getLowSeverity());
        assertEquals(0, result.getMediumSeverity());
        assertEquals(0, result.getHighSeverity());
        assertEquals(0, result.getCriticalSeverity());
    }

    @Test
    void getOverdueSummary_noActiveLoans_percentageIsZero() {
        when(loanRepository.countByStatus(LoanStatus.OVERDUE)).thenReturn(0L);
        when(loanRepository.countByStatus(LoanStatus.ACTIVE)).thenReturn(0L);

        OverdueReportSummaryResponse result = overdueReportService.getOverdueSummary();

        assertEquals(0.0, result.getOverduePercentage());
        assertEquals(0, result.getTotalActiveLoans());
    }

    // ── OverdueItemResponse.from (unit) ──

    @Test
    void overdueItemResponse_from_lowSeverity() {
        OverdueItemResponse response = OverdueItemResponse.from(overdueLow);

        assertEquals(1L, response.getLoanId());
        assertEquals("Maria Silva", response.getUserName());
        assertEquals("maria@ufu.br", response.getUserEmail());
        assertEquals("Clean Code", response.getBookTitle());
        assertEquals("978-0132350884", response.getBookIsbn());
        assertEquals(3, response.getDaysOverdue());
        assertEquals("LOW", response.getSeverity());
    }

    @Test
    void overdueItemResponse_from_mediumSeverity() {
        OverdueItemResponse response = OverdueItemResponse.from(overdueMedium);

        assertEquals(10, response.getDaysOverdue());
        assertEquals("MEDIUM", response.getSeverity());
    }

    @Test
    void overdueItemResponse_from_highSeverity() {
        OverdueItemResponse response = OverdueItemResponse.from(overdueHigh);

        assertEquals(20, response.getDaysOverdue());
        assertEquals("HIGH", response.getSeverity());
    }

    @Test
    void overdueItemResponse_from_criticalSeverity() {
        OverdueItemResponse response = OverdueItemResponse.from(overdueCritical);

        assertEquals(45, response.getDaysOverdue());
        assertEquals("CRITICAL", response.getSeverity());
    }

    @Test
    void getOverdueLoans_searchWithBlankString_usesPagedQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Loan> page = new PageImpl<>(List.of(overdueLow), pageable, 1);

        when(loanRepository.findAllOverduePaged(any(Pageable.class))).thenReturn(page);

        PagedResponse<OverdueItemResponse> result =
                overdueReportService.getOverdueLoans("   ", null, 0, 10);

        assertEquals(1, result.getContent().size());
        verify(loanRepository).findAllOverduePaged(any(Pageable.class));
        verify(loanRepository, never()).searchOverdueLoans(anyString());
    }

    // ── exportOverdueCsv ──

    @Test
    void exportOverdueCsv_success_generatesCorrectCsv() {
        when(loanRepository.findAllOverdueWithDetails())
                .thenReturn(List.of(overdueLow, overdueCritical));

        byte[] csv = overdueReportService.exportOverdueCsv();
        String csvContent = new String(csv, StandardCharsets.UTF_8);

        // Remove BOM
        if (csvContent.charAt(0) == '\uFEFF') {
            csvContent = csvContent.substring(1);
        }

        assertTrue(
                csvContent.startsWith(
                        "Leitor,Email,Telefone,Livro,ISBN,Data Empréstimo,Prazo,Dias Atraso,Severidade"));
        assertTrue(csvContent.contains("Maria Silva"));
        assertTrue(csvContent.contains("João Santos"));
        assertTrue(csvContent.contains("CRITICAL"));
        assertTrue(csvContent.contains("LOW"));

        // CSV deve estar ordenado por daysOverdue DESC (CRITICAL primeiro)
        int criticalIdx = csvContent.indexOf("CRITICAL");
        int lowIdx = csvContent.indexOf("LOW");
        assertTrue(
                criticalIdx < lowIdx,
                "CRITICAL deve aparecer antes de LOW (ordenado por dias DESC)");
    }

    @Test
    void exportOverdueCsv_empty_generatesHeadersOnly() {
        when(loanRepository.findAllOverdueWithDetails()).thenReturn(List.of());

        byte[] csv = overdueReportService.exportOverdueCsv();
        String csvContent = new String(csv, StandardCharsets.UTF_8);

        // Remove BOM
        if (csvContent.charAt(0) == '\uFEFF') {
            csvContent = csvContent.substring(1);
        }

        String[] lines = csvContent.trim().split("\n");
        assertEquals(1, lines.length, "Deve conter apenas a linha de cabeçalho");
        assertTrue(lines[0].startsWith("Leitor,Email"));
    }

    @Test
    void exportOverdueCsv_hasUtf8Bom() {
        when(loanRepository.findAllOverdueWithDetails()).thenReturn(List.of());

        byte[] csv = overdueReportService.exportOverdueCsv();
        String csvContent = new String(csv, StandardCharsets.UTF_8);

        assertEquals('\uFEFF', csvContent.charAt(0), "CSV deve iniciar com BOM UTF-8");
    }

    // ── getOverdueLoans com minDaysOverdue ──

    @Test
    void getOverdueLoans_withMinDays_filtersCorrectly() {
        // minDays=8 → só retorna empréstimos com 8+ dias de atraso
        when(loanRepository.findOverdueWithMinDays(any(LocalDate.class)))
                .thenReturn(List.of(overdueMedium, overdueHigh, overdueCritical));

        PagedResponse<OverdueItemResponse> result =
                overdueReportService.getOverdueLoans(null, 8, 0, 10);

        assertEquals(3, result.getContent().size());
        result.getContent()
                .forEach(
                        item ->
                                assertTrue(
                                        item.getDaysOverdue() >= 8,
                                        "Todos devem ter 8+ dias de atraso"));
        verify(loanRepository).findOverdueWithMinDays(any(LocalDate.class));
    }
}
