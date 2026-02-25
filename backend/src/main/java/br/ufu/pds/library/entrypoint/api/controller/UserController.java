package br.ufu.pds.library.entrypoint.api.controller;

import br.ufu.pds.library.entrypoint.api.dto.CreateUserRequest;
import br.ufu.pds.library.entrypoint.api.dto.UserResponse;
import br.ufu.pds.library.infrastructure.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Operações relacionadas a usuários")
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Cadastrar um novo usuário",
            description = "Cria um novo usuário no sistema")
    @ApiResponse(
            responseCode = "201",
            description = "Usuário cadastrado com sucesso",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @ApiResponse(responseCode = "409", description = "Email já existe no sistema")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse saved = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(
            summary = "Listar todos os usuários",
            description = "Retorna uma lista de todos os usuários ativos")
    @ApiResponse(
            responseCode = "200",
            description = "Lista retornada com sucesso",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    public ResponseEntity<List<UserResponse>> listAll() {
        List<UserResponse> users = userService.findAll();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(summary = "Obter usuário por ID", description = "Retorna um usuário pelo seu ID")
    @ApiResponse(
            responseCode = "200",
            description = "Usuário retornado com sucesso",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        UserResponse user = userService.getById(id);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Atualizar usuário",
            description = "Atualiza os dados de um usuário existente")
    @ApiResponse(
            responseCode = "200",
            description = "Usuário atualizado com sucesso",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @ApiResponse(responseCode = "409", description = "Email já existe no sistema")
    public ResponseEntity<UserResponse> update(
            @PathVariable Long id, @Valid @RequestBody CreateUserRequest request) {
        UserResponse saved = userService.update(id, request);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativar usuário", description = "Desativa (soft delete) um usuário pelo ID")
    @ApiResponse(responseCode = "204", description = "Usuário desativado com sucesso")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
