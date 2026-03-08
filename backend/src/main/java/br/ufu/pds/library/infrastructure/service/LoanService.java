package br.ufu.pds.library.infrastructure.service;

import br.ufu.pds.library.core.domain.*;
import br.ufu.pds.library.core.exceptions.BookNotAvailableException;
import br.ufu.pds.library.core.exceptions.BookNotFoundException;
import br.ufu.pds.library.core.exceptions.InvalidLoanStatusException;
import br.ufu.pds.library.core.exceptions.LoanNotFoundException;
import br.ufu.pds.library.core.exceptions.UserNotFoundException;
import br.ufu.pds.library.entrypoint.api.dto.LoanHistoryResponse;
import br.ufu.pds.library.entrypoint.api.dto.LoanHistorySummaryResponse;
import br.ufu.pds.library.entrypoint.api.dto.PagedResponse;
import br.ufu.pds.library.infrastructure.persistence.BookCopyRepository;
import br.ufu.pds.library.infrastructure.persistence.BookRepository;
import br.ufu.pds.library.infrastructure.persistence.LoanRepository;
import br.ufu.pds.library.infrastructure.persistence.ReservationRepository;
import br.ufu.pds.library.infrastructure.persistence.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public Loan save(Long userId, Long bookId, LocalDate dueDate) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));

        Book book =
                bookRepository
                        .findById(bookId)
                        .orElseThrow(() -> new BookNotFoundException(bookId));

        // Regra: não pode pegar o mesmo livro se já tem um empréstimo ativo/atrasado
        long userActiveForBook =
                loanRepository.countByUserIdAndBookIdAndStatusIn(
                        userId, bookId, List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE));
        if (userActiveForBook > 0) {
            throw new IllegalStateException(
                    "Você já possui um empréstimo ativo para este livro. Devolva-o antes de pegar outra cópia.");
        }

        long activeLoans = loanRepository.countByBookIdAndStatus(bookId, LoanStatus.ACTIVE);
        long overdueLoans = loanRepository.countByBookIdAndStatus(bookId, LoanStatus.OVERDUE);
        long availableForPickupCount =
                reservationRepository
                        .findByBookIdAndStatusOrderByReservationDateAsc(
                                bookId, ReservationStatus.AVAILABLE_FOR_PICKUP)
                        .size();

        if (activeLoans + overdueLoans + availableForPickupCount >= book.getQuantity()) {
            // Sem cópias livres → só pode emprestar se o usuário tem reserva AVAILABLE_FOR_PICKUP
            Optional<Reservation> myReservationOpt =
                    reservationRepository.findByUserIdAndBookIdAndStatus(
                            userId, bookId, ReservationStatus.AVAILABLE_FOR_PICKUP);

            if (myReservationOpt.isPresent()) {
                Reservation myRes = myReservationOpt.get();
                myRes.setStatus(ReservationStatus.FULFILLED);
                reservationRepository.save(myRes);
            } else {
                throw new BookNotAvailableException(bookId);
            }
        }

        Loan loan =
                Loan.builder()
                        .user(user)
                        .book(book)
                        .bookCopy(findAvailableCopy(bookId))
                        .loanDate(LocalDate.now())
                        .dueDate(dueDate)
                        .status(LoanStatus.ACTIVE)
                        .build();

        return loanRepository.save(loan);
    }

    /**
     * Busca um exemplar físico disponível do livro para atribuir ao empréstimo. Seleciona
     * aleatoriamente entre os disponíveis para distribuir o uso. Retorna null se não houver
     * exemplares cadastrados (compatibilidade retroativa).
     */
    private BookCopy findAvailableCopy(Long bookId) {
        List<BookCopy> available =
                bookCopyRepository.findAvailableCopies(
                        bookId, List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE));
        if (available.isEmpty()) return null;
        int randomIndex = ThreadLocalRandom.current().nextInt(available.size());
        return available.get(randomIndex);
    }

    @Transactional(readOnly = true)
    public List<Loan> findAll() {
        return loanRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Loan> searchActiveLoans(String query, String statusFilter) {
        final List<LoanStatus> statuses = resolveStatuses(statusFilter);

        // Filtro exclusivo de OVERDUE → usa queries dedicadas com JOIN FETCH + ORDER BY dueDate ASC
        if (statuses.size() == 1 && statuses.contains(LoanStatus.OVERDUE)) {
            if (query == null || query.isBlank()) {
                return loanRepository.findAllOverdueWithDetails();
            }
            return loanRepository.searchOverdueLoans(query.trim());
        }

        // Caso geral (ACTIVE + OVERDUE) — ordena por data de vencimento (mais urgentes primeiro)
        if (query == null || query.isBlank()) {
            return loanRepository.findByStatusInOrderByDueDateAsc(statuses);
        }
        return loanRepository.findLoansByEmailOrIsbnAndStatuses(statuses, query.trim());
    }

    private List<LoanStatus> resolveStatuses(String statusFilter) {
        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                return List.of(LoanStatus.valueOf(statusFilter.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // status inválido → retorna padrão
            }
        }
        return Arrays.asList(LoanStatus.ACTIVE, LoanStatus.OVERDUE);
    }

    @Transactional(readOnly = true)
    public Loan findById(Long id) {
        return loanRepository.findById(id).orElseThrow(() -> new LoanNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Loan> findByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        return loanRepository.findByUserId(userId);
    }

    @Transactional
    public Loan returnLoan(Long id) {
        Loan loan = loanRepository.findById(id).orElseThrow(() -> new LoanNotFoundException(id));

        if (!loan.isActive() && loan.getStatus() != LoanStatus.OVERDUE) {
            throw new InvalidLoanStatusException("Empréstimo com ID " + id + " já foi devolvido");
        }

        loan.setReturnDate(LocalDate.now());
        loan.setStatus(LoanStatus.RETURNED);

        loan = loanRepository.save(loan);

        // Regra 2: Devolução acorda o 1º da fila com prazo de 48h para retirada
        Optional<Reservation> nextInLine =
                reservationRepository.findFirstByBookIdAndStatusOrderByReservationDateAsc(
                        loan.getBook().getId(), ReservationStatus.PENDING);

        if (nextInLine.isPresent()) {
            Reservation res = nextInLine.get();
            res.setStatus(ReservationStatus.AVAILABLE_FOR_PICKUP);
            res.setExpiresAt(LocalDateTime.now().plusHours(48));
            reservationRepository.save(res);
        }

        return loan;
    }

    @Transactional
    public void delete(Long id) {
        if (!loanRepository.existsById(id)) {
            throw new LoanNotFoundException(id);
        }
        loanRepository.deleteById(id);
    }

    // =============================================
    // Histórico de Leituras (Backlog 7)
    // =============================================

    @Transactional(readOnly = true)
    public PagedResponse<LoanHistoryResponse> getLoanHistory(
            Long userId, List<String> statuses, int page, int size) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Loan> loanPage;

        if (statuses != null && !statuses.isEmpty()) {
            List<LoanStatus> statusList =
                    statuses.stream().map(s -> LoanStatus.valueOf(s.trim().toUpperCase())).toList();
            loanPage =
                    loanRepository.findByUserIdAndStatusInOrderByLoanDateDesc(
                            userId, statusList, pageable);
        } else {
            loanPage = loanRepository.findByUserIdOrderByLoanDateDesc(userId, pageable);
        }

        List<LoanHistoryResponse> content =
                loanPage.getContent().stream().map(LoanHistoryResponse::from).toList();

        return new PagedResponse<>(
                content,
                loanPage.getNumber(),
                loanPage.getSize(),
                loanPage.getTotalElements(),
                loanPage.getTotalPages(),
                loanPage.hasNext());
    }

    @Transactional(readOnly = true)
    public LoanHistorySummaryResponse getLoanSummary(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        long active = loanRepository.countByUserIdAndStatus(userId, LoanStatus.ACTIVE);
        long returned = loanRepository.countByUserIdAndStatus(userId, LoanStatus.RETURNED);
        long overdue = loanRepository.countByUserIdAndStatus(userId, LoanStatus.OVERDUE);
        long total = active + returned + overdue;

        double onTimeRate = 0.0;
        if (returned > 0) {
            long onTimeCount = loanRepository.countOnTimeReturns(userId);
            onTimeRate = (double) onTimeCount / returned;
        }

        return new LoanHistorySummaryResponse(total, active, returned, overdue, onTimeRate);
    }
}
