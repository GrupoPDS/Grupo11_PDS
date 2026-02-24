package br.ufu.pds.library.infrastructure.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.ufu.pds.library.core.domain.User;
import br.ufu.pds.library.core.exceptions.DuplicateEmailException;
import br.ufu.pds.library.core.exceptions.UserNotFoundException;
import br.ufu.pds.library.infrastructure.persistence.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private UserService userService;

    private User user;

    @BeforeEach
    void setup() {
        user =
                User.builder()
                        .name("João")
                        .email("joao@example.com")
                        .password("secret")
                        .role("USER")
                        .build();
    }

    @Test
    void save_success_whenEmailNotExists() {
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenAnswer(
                        inv -> {
                          User u = inv.getArgument(0);
                          u.setId(1L);
                          return u;
                        });

        User saved = userService.save(user);
        assertEquals(1L, saved.getId());
        assertEquals(user.getEmail(), saved.getEmail());
    }

    @Test
    void save_throwsDuplicateEmail_whenEmailExists() {
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.save(user));
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.findById(99L));
    }
}
