package br.ufu.pds.library.entrypoint.api.dto;

import br.ufu.pds.library.core.domain.Reservation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReservationResponse {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private LocalDate reservationDate;
    private String status;
    private Integer queuePosition;
    private LocalDateTime expiresAt;

    public static ReservationResponse fromEntity(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .bookId(reservation.getBook().getId())
                .bookTitle(reservation.getBook().getTitle())
                .reservationDate(reservation.getReservationDate())
                .status(reservation.getStatus().name())
                .expiresAt(reservation.getExpiresAt())
                .build();
    }

    public static ReservationResponse fromEntity(Reservation reservation, int queuePosition) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .bookId(reservation.getBook().getId())
                .bookTitle(reservation.getBook().getTitle())
                .reservationDate(reservation.getReservationDate())
                .status(reservation.getStatus().name())
                .queuePosition(queuePosition)
                .expiresAt(reservation.getExpiresAt())
                .build();
    }
}
