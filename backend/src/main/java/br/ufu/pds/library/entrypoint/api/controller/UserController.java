package br.ufu.pds.library.entrypoint.api.controller;

import br.ufu.pds.library.core.domain.User;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Operações relacionadas a usuários/leitores")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(
            summary = "Cadastrar um novo usuário",
            description = "Cria um novo leitor no sistema com nome, email e perfil.")
    @ApiResponse(
            responseCode = "201",
            description = "Usuário cadastrado com sucesso",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @ApiResponse(responseCode = "409", description = "Email já existe no sistema")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou incompletos")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User user =
                User.builder()
                        .name(request.getName())
                        .email(request.getEmail())
                        .phone(request.getPhone())
                        .role(request.getRole())
                        .build();

        User saved = userService.create(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromEntity(saved));
    }

    @GetMapping
    @Operation(
            summary = "Listar todos os usuários ativos",
            description = "Retorna apenas usuários com status ativo no sistema")
    @ApiResponse(
            responseCode = "200",
            description = "Lista de usuários retornada com sucesso",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    public ResponseEntity<List<UserResponse>> listAll() {
        List<UserResponse> users =
                userService.findAll().stream().map(UserResponse::fromEntity).toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID")
    @ApiResponse(
            responseCode = "200",
            description = "Usuário encontrado",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar dados de um usuário",
            description = "Atualiza nome, email, telefone e perfil de um usuário existente")
    @ApiResponse(
            responseCode = "200",
            description = "Usuário atualizado com sucesso",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @ApiResponse(responseCode = "409", description = "Email já existe em outro usuário")
    public ResponseEntity<UserResponse> update(
            @PathVariable Long id, @Valid @RequestBody CreateUserRequest request) {
        User updatedUser =
                User.builder()
                        .name(request.getName())
                        .email(request.getEmail())
                        .phone(request.getPhone())
                        .role(request.getRole())
                        .build();

        User saved = userService.update(id, updatedUser);
        return ResponseEntity.ok(UserResponse.fromEntity(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Desativar um usuário",
            description = "Realiza soft delete — marca o usuário como inativo sem remover do banco")
    @ApiResponse(responseCode = "204", description = "Usuário desativado com sucesso")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        userService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
