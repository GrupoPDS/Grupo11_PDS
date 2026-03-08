package br.ufu.pds.library.infrastructure.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.ufu.pds.library.core.domain.*;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookRepository bookRepository;
    @Mock private LoanRepository loanRepository;

    @InjectMocks private ReservationService reservationService;

    private User userA;
    private User userB;
    private Book book;

    @BeforeEach
    void setup() {
        userA = User.builder().id(1L).name("Alice").email("alice@ufu.br").role("STUDENT").build();
        userB = User.builder().id(2L).name("Bob").email("bob@ufu.br").role("STUDENT").build();

        book =
                Book.builder()
                        .id(10L)
                        .title("Design Patterns")
                        .author("GoF")
                        .isbn("978-0201633610")
                        .quantity(1)
                        .build();
    }

    // ====================================================
    // Testes de Criação de Reserva (reserve)
    // ====================================================

    @Nested
    @DisplayName("reserve()")
    class ReserveTests {

        @Test
        @DisplayName("Deve criar reserva PENDING quando livro sem exemplares disponíveis")
        void shouldCreatePendingReservation() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
            when(loanRepository.countByBookIdAndStatus(10L, LoanStatus.ACTIVE)).thenReturn(1L);
            when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(
                            eq(1L), eq(10L), anyList()))
                    .thenReturn(false);

            Reservation saved =
                    Reservation.builder()
                            .id(100L)
                            .user(userA)
                            .book(book)
                            .reservationDate(LocalDate.now())
                            .status(ReservationStatus.PENDING)
                            .build();
            when(reservationRepository.save(any(Reservation.class))).thenReturn(saved);
            when(reservationRepository.countPendingAhead(eq(10L), any(LocalDate.class), eq(100L)))
                    .thenReturn(0L);

            ReservationResponse response = reservationService.reserve(1L, 10L);

            assertNotNull(response);
            assertEquals(ReservationStatus.PENDING.name(), response.getStatus());
            assertEquals(1, response.getQueuePosition());
            assertEquals("Design Patterns", response.getBookTitle());
            verify(reservationRepository).save(any(Reservation.class));
        }

        @Test
        @DisplayName("Deve lançar BookAvailableException quando livro está disponível na estante")
        void shouldThrowWhenBookIsAvailable() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
            // Nenhum empréstimo ativo, nenhum AVAILABLE_FOR_PICKUP → livro disponível
            when(loanRepository.countByBookIdAndStatus(10L, LoanStatus.ACTIVE)).thenReturn(0L);

            assertThrows(BookAvailableException.class, () -> reservationService.reserve(1L, 10L));
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar IllegalStateException quando usuário já tem reserva ativa")
        void shouldThrowWhenUserAlreadyHasReservation() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
            when(loanRepository.countByBookIdAndStatus(10L, LoanStatus.ACTIVE)).thenReturn(1L);
            when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(
                            eq(1L), eq(10L), anyList()))
                    .thenReturn(true);

            assertThrows(IllegalStateException.class, () -> reservationService.reserve(1L, 10L));
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar UserNotFoundException quando usuário não existe")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> reservationService.reserve(99L, 10L));
        }

        @Test
        @DisplayName("Deve lançar BookNotFoundException quando livro não existe")
        void shouldThrowWhenBookNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
            when(bookRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(BookNotFoundException.class, () -> reservationService.reserve(1L, 99L));
        }
    }

    // ====================================================
    // Testes de Cancelamento (cancelReservation)
    // ====================================================

    @Nested
    @DisplayName("cancelReservation()")
    class CancelReservationTests {

        @Test
        @DisplayName("Deve cancelar reserva PENDING sem promover ninguém")
        void shouldCancelPendingReservation() {
            Reservation reservation =
                    Reservation.builder()
                            .id(100L)
                            .user(userA)
                            .book(book)
                            .reservationDate(LocalDate.now())
                            .status(ReservationStatus.PENDING)
                            .build();

            when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

            reservationService.cancelReservation(100L, 1L);

            assertEquals(ReservationStatus.CANCELED, reservation.getStatus());
            verify(reservationRepository).save(reservation);
            // Não deve promover ninguém porque era PENDING
            verify(reservationRepository, never())
                    .findFirstByBookIdAndStatusOrderByReservationDateAsc(
                            anyLong(), eq(ReservationStatus.PENDING));
        }

        @Test
        @DisplayName("Deve promover próximo da fila ao cancelar AVAILABLE_FOR_PICKUP")
        void shouldPromoteNextWhenCancellingAvailableForPickup() {
            Reservation availableRes =
                    Reservation.builder()
                            .id(100L)
                            .user(userA)
                            .book(book)
                            .reservationDate(LocalDate.now().minusDays(2))
                            .status(ReservationStatus.AVAILABLE_FOR_PICKUP)
                            .expiresAt(LocalDateTime.now().plusHours(24))
                            .build();

            Reservation pendingRes =
                    Reservation.builder()
                            .id(101L)
                            .user(userB)
                            .book(book)
                            .reservationDate(LocalDate.now().minusDays(1))
                            .status(ReservationStatus.PENDING)
                            .build();

            when(reservationRepository.findById(100L)).thenReturn(Optional.of(availableRes));
            when(reservationRepository.findFirstByBookIdAndStatusOrderByReservationDateAsc(
                            10L, ReservationStatus.PENDING))
                    .thenReturn(Optional.of(pendingRes));

            reservationService.cancelReservation(100L, 1L);

            // Reserva original cancelada
            assertEquals(ReservationStatus.CANCELED, availableRes.getStatus());

            // Próximo da fila promovido para AVAILABLE_FOR_PICKUP com expiresAt
            assertEquals(ReservationStatus.AVAILABLE_FOR_PICKUP, pendingRes.getStatus());
            assertNotNull(pendingRes.getExpiresAt());
            verify(reservationRepository, times(2)).save(any(Reservation.class));
        }

        @Test
        @DisplayName("Deve lançar IllegalStateException quando outro usuário tenta cancelar")
        void shouldThrowWhenWrongUserCancels() {
            Reservation reservation =
                    Reservation.builder()
                            .id(100L)
                            .user(userA)
                            .book(book)
                            .status(ReservationStatus.PENDING)
                            .build();

            when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

            assertThrows(
                    IllegalStateException.class,
                    () -> reservationService.cancelReservation(100L, 2L));
        }

        @Test
        @DisplayName("Deve lançar IllegalStateException ao cancelar reserva já FULFILLED")
        void shouldThrowWhenCancellingFulfilled() {
            Reservation reservation =
                    Reservation.builder()
                            .id(100L)
                            .user(userA)
                            .book(book)
                            .status(ReservationStatus.FULFILLED)
                            .build();

            when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

            assertThrows(
                    IllegalStateException.class,
                    () -> reservationService.cancelReservation(100L, 1L));
        }

        @Test
        @DisplayName("Deve lançar ReservationNotFoundException quando reserva não existe")
        void shouldThrowWhenReservationNotFound() {
            when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(
                    ReservationNotFoundException.class,
                    () -> reservationService.cancelReservation(999L, 1L));
        }
    }

    // ====================================================
    // Testes de Listagem com Posição (findByUserIdWithPosition)
    // ====================================================

    @Nested
    @DisplayName("findByUserIdWithPosition()")
    class FindByUserIdWithPositionTests {

        @Test
        @DisplayName("Deve retornar reservas com posição na fila para PENDING")
        void shouldReturnReservationsWithQueuePosition() {
            Reservation pending =
                    Reservation.builder()
                            .id(100L)
                            .user(userA)
                            .book(book)
                            .reservationDate(LocalDate.now())
                            .status(ReservationStatus.PENDING)
                            .build();

            Reservation fulfilled =
                    Reservation.builder()
                            .id(101L)
                            .user(userA)
                            .book(book)
                            .reservationDate(LocalDate.now().minusDays(10))
                            .status(ReservationStatus.FULFILLED)
                            .build();

            when(userRepository.existsById(1L)).thenReturn(true);
            when(reservationRepository.findByUserId(1L)).thenReturn(List.of(pending, fulfilled));
            when(reservationRepository.countPendingAhead(eq(10L), any(LocalDate.class), eq(100L)))
                    .thenReturn(2L);

            List<ReservationResponse> responses = reservationService.findByUserIdWithPosition(1L);

            assertEquals(2, responses.size());

            // A reserva PENDING deve ter posição 3 (2 à frente + 1)
            ReservationResponse pendingRes = responses.get(0);
            assertEquals(3, pendingRes.getQueuePosition());

            // A reserva FULFILLED deve ter posição 0
            ReservationResponse fulfilledRes = responses.get(1);
            assertEquals(0, fulfilledRes.getQueuePosition());
        }

        @Test
        @DisplayName("Deve lançar UserNotFoundException quando usuário não existe")
        void shouldThrowWhenUserNotFoundOnList() {
            when(userRepository.existsById(99L)).thenReturn(false);

            assertThrows(
                    UserNotFoundException.class,
                    () -> reservationService.findByUserIdWithPosition(99L));
        }
    }

    // ====================================================
    // Teste de Promoção da Fila (promoteNextInLine)
    // ====================================================

    @Nested
    @DisplayName("promoteNextInLine()")
    class PromoteNextInLineTests {

        @Test
        @DisplayName("Deve promover primeiro PENDING para AVAILABLE_FOR_PICKUP com expiresAt")
        void shouldPromoteFirstPendingWithExpiry() {
            Reservation pending =
                    Reservation.builder()
                            .id(200L)
                            .user(userB)
                            .book(book)
                            .reservationDate(LocalDate.now())
                            .status(ReservationStatus.PENDING)
                            .build();

            when(reservationRepository.findFirstByBookIdAndStatusOrderByReservationDateAsc(
                            10L, ReservationStatus.PENDING))
                    .thenReturn(Optional.of(pending));

            reservationService.promoteNextInLine(10L);

            assertEquals(ReservationStatus.AVAILABLE_FOR_PICKUP, pending.getStatus());
            assertNotNull(pending.getExpiresAt());
            assertTrue(pending.getExpiresAt().isAfter(LocalDateTime.now().plusHours(47)));
            verify(reservationRepository).save(pending);
        }

        @Test
        @DisplayName("Não deve fazer nada quando não há PENDING na fila")
        void shouldDoNothingWhenNoPending() {
            when(reservationRepository.findFirstByBookIdAndStatusOrderByReservationDateAsc(
                            10L, ReservationStatus.PENDING))
                    .thenReturn(Optional.empty());

            reservationService.promoteNextInLine(10L);

            verify(reservationRepository, never()).save(any());
        }
    }
}
