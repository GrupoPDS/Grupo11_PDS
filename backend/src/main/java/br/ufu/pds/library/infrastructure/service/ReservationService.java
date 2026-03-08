package br.ufu.pds.library.infrastructure.service;

import br.ufu.pds.library.core.domain.Book;
import br.ufu.pds.library.core.domain.LoanStatus;
import br.ufu.pds.library.core.domain.Reservation;
import br.ufu.pds.library.core.domain.ReservationStatus;
import br.ufu.pds.library.core.domain.User;
import br.ufu.pds.library.core.exceptions.BookAvailableException;
import br.ufu.pds.library.core.exceptions.BookNotFoundException;
import br.ufu.pds.library.core.exceptions.ReservationNotFoundException;
import br.ufu.pds.library.core.exceptions.UserNotFoundException;
import br.ufu.pds.library.entrypoint.api.dto.ReservationResponse;
import br.ufu.pds.library.infrastructure.persistence.BookRepository;
import br.ufu.pds.library.infrastructure.persistence.LoanRepository;
import br.ufu.pds.library.infrastructure.persistence.ReservationRepository;
import br.ufu.pds.library.infrastructure.persistence.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final long PICKUP_EXPIRY_HOURS = 48;

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    @Transactional
    public ReservationResponse reserve(Long userId, Long bookId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));

        Book book =
                bookRepository
                        .findById(bookId)
                        .orElseThrow(() -> new BookNotFoundException(bookId));

        // Regra 1: Só reservar se estante vazia (ACTIVE + OVERDUE + AVAILABLE_FOR_PICKUP >=
        // quantity)
        long activeLoans = loanRepository.countByBookIdAndStatus(bookId, LoanStatus.ACTIVE);
        long overdueLoans = loanRepository.countByBookIdAndStatus(bookId, LoanStatus.OVERDUE);
        long availableForPickupCount =
                reservationRepository.countByBookIdAndStatus(
                        bookId, ReservationStatus.AVAILABLE_FOR_PICKUP);

        if (activeLoans + overdueLoans + availableForPickupCount < book.getQuantity()) {
            throw new BookAvailableException(bookId);
        }

        // Validação: Usuário já tem empréstimo ativo/atrasado deste livro?
        long userActiveLoans =
                loanRepository.countByUserIdAndBookIdAndStatusIn(
                        userId, bookId, Arrays.asList(LoanStatus.ACTIVE, LoanStatus.OVERDUE));
        if (userActiveLoans > 0) {
            throw new IllegalStateException("Você já possui um empréstimo ativo para este livro.");
        }

        // Validação: Usuário já está na fila?
        boolean alreadyReserved =
                reservationRepository.existsByUserIdAndBookIdAndStatusIn(
                        userId,
                        bookId,
                        Arrays.asList(
                                ReservationStatus.PENDING, ReservationStatus.AVAILABLE_FOR_PICKUP));
        if (alreadyReserved) {
            throw new IllegalStateException("Você já possui uma reserva ativa para este livro.");
        }

        Reservation reservation =
                Reservation.builder()
                        .user(user)
                        .book(book)
                        .reservationDate(LocalDate.now())
                        .status(ReservationStatus.PENDING)
                        .build();

        reservation = reservationRepository.save(reservation);

        int queuePosition = calculateQueuePosition(reservation);
        return ReservationResponse.fromEntity(reservation, queuePosition);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> findByUserIdWithPosition(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        List<Reservation> reservations = reservationRepository.findByUserId(userId);
        return reservations.stream()
                .map(
                        r -> {
                            int position = calculateQueuePosition(r);
                            return ReservationResponse.fromEntity(r, position);
                        })
                .toList();
    }

    @Transactional
    public void cancelReservation(Long id, Long userId) {
        Reservation reservation =
                reservationRepository
                        .findById(id)
                        .orElseThrow(() -> new ReservationNotFoundException(id));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Apenas o criador da reserva pode cancelá-la.");
        }

        if (reservation.getStatus() == ReservationStatus.FULFILLED
                || reservation.getStatus() == ReservationStatus.CANCELED) {
            throw new IllegalStateException("Não é possível cancelar uma reserva já finalizada.");
        }

        boolean wasAvailableForPickup = reservation.isAvailableForPickup();
        reservation.setStatus(ReservationStatus.CANCELED);
        reservationRepository.save(reservation);

        // Se o cancelado era AVAILABLE_FOR_PICKUP, promove o próximo da fila
        if (wasAvailableForPickup) {
            promoteNextInLine(reservation.getBook().getId());
        }
    }

    @Transactional(readOnly = true)
    public long countAvailableForPickup(Long userId) {
        return reservationRepository.countByUserIdAndStatus(
                userId, ReservationStatus.AVAILABLE_FOR_PICKUP);
    }

    /** Promove o primeiro PENDING da fila para AVAILABLE_FOR_PICKUP com prazo de 48h. */
    public void promoteNextInLine(Long bookId) {
        Optional<Reservation> nextInLine =
                reservationRepository.findFirstByBookIdAndStatusOrderByReservationDateAsc(
                        bookId, ReservationStatus.PENDING);

        if (nextInLine.isPresent()) {
            Reservation next = nextInLine.get();
            next.setStatus(ReservationStatus.AVAILABLE_FOR_PICKUP);
            next.setExpiresAt(LocalDateTime.now().plusHours(PICKUP_EXPIRY_HOURS));
            reservationRepository.save(next);
        }
    }

    /**
     * Calcula a posição na fila de uma reserva PENDING. Retorna 0 se a reserva não está PENDING.
     */
    private int calculateQueuePosition(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            return 0;
        }
        long ahead =
                reservationRepository.countPendingAhead(
                        reservation.getBook().getId(),
                        reservation.getReservationDate(),
                        reservation.getId());
        return (int) (ahead + 1);
    }
}
