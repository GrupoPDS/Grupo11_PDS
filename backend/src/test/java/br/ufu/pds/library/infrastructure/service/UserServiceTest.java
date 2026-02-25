package br.ufu.pds.library.infrastructure.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.ufu.pds.library.core.domain.User;
import br.ufu.pds.library.core.exceptions.DuplicateEmailException;
import br.ufu.pds.library.core.exceptions.UserNotFoundException;
import br.ufu.pds.library.entrypoint.api.dto.CreateUserRequest;
import br.ufu.pds.library.entrypoint.api.dto.UserResponse;
import br.ufu.pds.library.infrastructure.persistence.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private UserService userService;

    private User userEntity;

    @BeforeEach
    void setup() {
        userEntity =
                User.builder()
                        .name("João")
                        .email("joao@example.com")
                        .phone("123456789")
                        .role("STUDENT")
                        .build();
    }

    @Test
    void should_create_user_when_emailNotExists() {
        CreateUserRequest req = new CreateUserRequest("João", "joao@example.com", "123456789", "STUDENT");

        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> {
                    User u = inv.getArgument(0);
                    u.setId(1L);
                    return u;
                });

        UserResponse resp = userService.create(req);

        assertEquals(1L, resp.id());
        assertEquals(req.name(), resp.name());
        assertEquals(req.email(), resp.email());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void should_throw_DuplicateEmailException_when_emailAlreadyExists() {
        CreateUserRequest req = new CreateUserRequest("João", "joao@example.com", "123456789", "STUDENT");
        when(userRepository.existsByEmail(req.email())).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.create(req));
    }

    @Test
    void should_deactivate_user_when_exists() {
        userEntity.setId(42L);
        userEntity.setActive(true);

        when(userRepository.findById(42L)).thenReturn(Optional.of(userEntity));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.deactivate(42L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertFalse(saved.getActive());
    }

    @Test
    void should_throw_UserNotFoundException_when_findById_missing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getById(99L));
    }

    @Test
    void should_list_only_active_users() {
        User u1 = User.builder().id(1L).name("A").email("a@example.com").active(true).role("STUDENT").build();
        User u2 = User.builder().id(2L).name("B").email("b@example.com").active(true).role("STUDENT").build();

        when(userRepository.findByActiveTrue()).thenReturn(List.of(u1, u2));

        List<UserResponse> responses = userService.findAll();

        assertEquals(2, responses.size());
        assertEquals("a@example.com", responses.get(0).email());
    }
}
