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
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .role(request.role())
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

        if (!existing.getEmail().equals(request.email())
                && userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        existing.setName(request.name());
        existing.setEmail(request.email());
        existing.setPhone(request.phone());
        existing.setRole(request.role());

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
