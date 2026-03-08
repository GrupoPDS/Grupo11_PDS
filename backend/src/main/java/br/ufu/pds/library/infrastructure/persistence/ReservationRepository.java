package br.ufu.pds.library.infrastructure.persistence;

import br.ufu.pds.library.core.domain.Reservation;
import br.ufu.pds.library.core.domain.ReservationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByBookIdOrderByReservationDateAsc(Long bookId);

    // Encontra as reservas pendentes de um livro (fila de espera)
    List<Reservation> findByBookIdAndStatusOrderByReservationDateAsc(
            Long bookId, ReservationStatus status);

    // Conta reservas ativas de um usuário para limite de reservas
    long countByUserIdAndStatusIn(Long userId, List<ReservationStatus> statuses);

    // Conta reservas por livro e status (usado no cálculo de disponibilidade)
    long countByBookIdAndStatus(Long bookId, ReservationStatus status);

    // Conta reservas de um usuário por status (usado para notificações)
    long countByUserIdAndStatus(Long userId, ReservationStatus status);

    // Busca reserva específica do usuário para um livro com determinado status
    Optional<Reservation> findByUserIdAndBookIdAndStatus(
            Long userId, Long bookId, ReservationStatus status);

    // Checa se o usuário já tem uma reserva na fila
    boolean existsByUserIdAndBookIdAndStatusIn(
            Long userId, Long bookId, List<ReservationStatus> statuses);

    // Pega o primeiro da fila que está esperando
    Optional<Reservation> findFirstByBookIdAndStatusOrderByReservationDateAsc(
            Long bookId, ReservationStatus status);

    // Conta quantas reservas PENDING existem para o livro com data anterior ou igual à dada
    @Query(
            "SELECT COUNT(r) FROM Reservation r WHERE r.book.id = :bookId "
                    + "AND r.status = 'PENDING' AND r.reservationDate <= :date AND r.id < :reservationId")
    long countPendingAhead(
            @Param("bookId") Long bookId,
            @Param("date") java.time.LocalDate date,
            @Param("reservationId") Long reservationId);
}
