package br.ufu.pds.library.infrastructure.service;

import br.ufu.pds.library.core.domain.User;
import br.ufu.pds.library.core.exceptions.DuplicateEmailException;
import br.ufu.pds.library.core.exceptions.UserNotFoundException;
import br.ufu.pds.library.entrypoint.api.dto.CreateUserRequest;
import br.ufu.pds.library.entrypoint.api.dto.UserResponse;
import br.ufu.pds.library.infrastructure.persistence.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .role(request.getRole())
                .build();

        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findByActiveTrue().stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse update(Long id, CreateUserRequest request) {
        User existing = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));

        if (!existing.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        existing.setName(request.getName());
        existing.setEmail(request.getEmail());
        existing.setPassword(request.getPassword());
        existing.setRole(request.getRole());

        User saved = userRepository.save(existing);
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public void deactivate(Long id) {
        User existing = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));

        // TODO: impedir desativação se o usuário tiver empréstimos ativos — implementar quando o módulo de loans estiver pronto
        existing.setActive(false);
        userRepository.save(existing);
    }
}
