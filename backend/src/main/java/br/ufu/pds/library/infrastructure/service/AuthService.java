package br.ufu.pds.library.infrastructure.service;

import br.ufu.pds.library.core.domain.RefreshToken;
import br.ufu.pds.library.core.domain.User;
import br.ufu.pds.library.core.exceptions.DuplicateEmailException;
import br.ufu.pds.library.entrypoint.api.dto.AuthResponse;
import br.ufu.pds.library.entrypoint.api.dto.LoginRequest;
import br.ufu.pds.library.entrypoint.api.dto.RegisterRequest;
import br.ufu.pds.library.entrypoint.api.dto.UserResponse;
import br.ufu.pds.library.infrastructure.persistence.RefreshTokenRepository;
import br.ufu.pds.library.infrastructure.persistence.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        User user =
                User.builder()
                        .name(request.getName())
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .role("STUDENT")
                        .build();

        User saved = userRepository.save(user);
        return generateAuthResponse(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user =
                userRepository
                        .findByEmail(request.getEmail())
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Usuário não encontrado após autenticação"));

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken =
                refreshTokenRepository
                        .findByToken(refreshTokenValue)
                        .orElseThrow(() -> new RuntimeException("Refresh token inválido"));

        if (!refreshToken.isUsable()) {
            throw new RuntimeException("Refresh token expirado ou revogado");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn(jwtService.getAccessTokenExpiration())
                .user(UserResponse.fromEntity(user))
                .build();
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        RefreshToken refreshToken =
                refreshTokenRepository
                        .findByToken(refreshTokenValue)
                        .orElseThrow(() -> new RuntimeException("Refresh token não encontrado"));

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken(user);

        // Persistir refresh token
        RefreshToken refreshToken =
                RefreshToken.builder()
                        .user(user)
                        .token(refreshTokenValue)
                        .expiresAt(
                                LocalDateTime.now()
                                        .plusSeconds(jwtService.getRefreshTokenExpiration() / 1000))
                        .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn(jwtService.getAccessTokenExpiration())
                .user(UserResponse.fromEntity(user))
                .build();
    }
}
