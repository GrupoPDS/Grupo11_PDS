package br.ufu.pds.library.entrypoint.api.controller;

import br.ufu.pds.library.core.domain.User;
import br.ufu.pds.library.entrypoint.api.dto.ReservationRequest;
import br.ufu.pds.library.entrypoint.api.dto.ReservationResponse;
import br.ufu.pds.library.infrastructure.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Endpoints para gerenciamento da fila de espera")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(
            summary = "Entrar na fila de espera de um livro",
            description =
                    "Cria uma reserva PENDING para o livro caso não haja exemplares na estante")
    @ApiResponse(responseCode = "201", description = "Reserva criada com sucesso")
    @ApiResponse(
            responseCode = "400",
            description = "Usuário já possui reserva Ativa/Pendente para o livro")
    @ApiResponse(
            responseCode = "409",
            description = "Livro disponível na estante, não é necessário reservar")
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request,
            @AuthenticationPrincipal User principal) {

        ReservationResponse response =
                reservationService.reserve(principal.getId(), request.bookId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my/notifications")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Contagem de notificações de reservas disponíveis para retirada")
    @ApiResponse(responseCode = "200", description = "Número de reservas prontas para retirada")
    public ResponseEntity<java.util.Map<String, Object>> getNotifications(
            @AuthenticationPrincipal User principal) {
        long count = reservationService.countAvailableForPickup(principal.getId());
        return ResponseEntity.ok(java.util.Map.of("availableForPickup", count));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN') or #userId == principal.id")
    @Operation(
            summary = "Listar reservas do usuário",
            description = "Retorna todas as reservas de um usuário com posição na fila")
    public ResponseEntity<List<ReservationResponse>> listByUser(@PathVariable Long userId) {
        List<ReservationResponse> reservations =
                reservationService.findByUserIdWithPosition(userId);
        return ResponseEntity.ok(reservations);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Cancelar reserva",
            description =
                    "Cancela uma reserva. Apenas o próprio usuário pode cancelar sua reserva. Se a reserva era AVAILABLE_FOR_PICKUP, o próximo da fila é promovido.")
    @ApiResponse(responseCode = "204", description = "Cancelado com sucesso")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long id, @AuthenticationPrincipal User principal) {

        reservationService.cancelReservation(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
