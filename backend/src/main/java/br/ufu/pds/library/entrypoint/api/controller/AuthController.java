package br.ufu.pds.library.entrypoint.api.controller;

import br.ufu.pds.library.core.domain.User;
import br.ufu.pds.library.entrypoint.api.dto.*;
import br.ufu.pds.library.infrastructure.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autenticação e autorização")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Criar conta", description = "Registra um novo usuário com role STUDENT")
    @ApiResponse(responseCode = "201", description = "Conta criada com sucesso")
    @ApiResponse(responseCode = "409", description = "Email já cadastrado")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar", description = "Login com email e senha")
    @ApiResponse(responseCode = "200", description = "Login com sucesso")
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Renovar token",
            description = "Gera novo access token usando refresh token")
    @ApiResponse(responseCode = "200", description = "Token renovado")
    @ApiResponse(responseCode = "401", description = "Refresh token inválido ou expirado")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResponse response = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoga o refresh token")
    @ApiResponse(responseCode = "204", description = "Logout com sucesso")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(
            summary = "Dados do usuário logado",
            description = "Retorna o perfil do usuário autenticado")
    @ApiResponse(responseCode = "200", description = "Dados do perfil")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }
}
