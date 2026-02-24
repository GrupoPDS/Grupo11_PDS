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
@Tag(name = "Users", description = "Operações relacionadas a usuários")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(
            summary = "Cadastrar um novo usuário",
            description = "Cria um novo usuário no sistema")
    @ApiResponse(
            responseCode = "201",
            description = "Usuário cadastrado com sucesso",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @ApiResponse(responseCode = "409", description = "Email já existe no sistema")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User user =
                User.builder()
                        .name(request.getName())
                        .email(request.getEmail())
                        .password(request.getPassword())
                        .role(request.getRole())
                        .build();

        User saved = userService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromEntity(saved));
    }

    @GetMapping
    @Operation(
            summary = "Listar todos os usuários",
            description = "Retorna uma lista de todos os usuários")
    @ApiResponse(
            responseCode = "200",
            description = "Lista retornada com sucesso",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    public ResponseEntity<List<UserResponse>> listAll() {
        List<UserResponse> users =
                userService.findAll().stream().map(UserResponse::fromEntity).toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter usuário por ID", description = "Retorna um usuário pelo seu ID")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar usuário",
            description = "Atualiza os dados de um usuário existente")
    public ResponseEntity<UserResponse> update(
            @PathVariable Long id, @Valid @RequestBody CreateUserRequest request) {
        User updated =
                User.builder()
                        .name(request.getName())
                        .email(request.getEmail())
                        .password(request.getPassword())
                        .role(request.getRole())
                        .build();

        User saved = userService.update(id, updated);
        return ResponseEntity.ok(UserResponse.fromEntity(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover usuário", description = "Remove um usuário pelo ID")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
