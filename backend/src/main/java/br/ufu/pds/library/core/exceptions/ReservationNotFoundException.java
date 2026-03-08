package br.ufu.pds.library.core.exceptions;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(Long id) {
        super("Reserva com ID " + id + " não encontrada");
    }
}
