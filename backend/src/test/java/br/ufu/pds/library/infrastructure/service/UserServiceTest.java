package br.ufu.pds.library.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.ufu.pds.library.core.domain.User;
import br.ufu.pds.library.core.exceptions.DuplicateEmailException;
import br.ufu.pds.library.core.exceptions.UserNotFoundException;
import br.ufu.pds.library.infrastructure.persistence.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser =
                User.builder()
                        .id(1L)
                        .name("Maria Silva")
                        .email("maria@ufu.br")
                        .phone("34999999999")
                        .role("STUDENT")
                        .active(true)
                        .build();
    }

    // =============================================
    // CREATE
    // =============================================

    @Test
    @DisplayName("should_create_user_when_emailDoesNotExist")
    void shouldCreateUser() {
        when(userRepository.existsByEmail("maria@ufu.br")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        User result = userService.create(sampleUser);

        assertThat(result.getName()).isEqualTo("Maria Silva");
        assertThat(result.getEmail()).isEqualTo("maria@ufu.br");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("should_throw_DuplicateEmailException_when_emailAlreadyExists")
    void shouldThrowDuplicateEmail() {
        when(userRepository.existsByEmail("maria@ufu.br")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(sampleUser))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("maria@ufu.br");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("should_set_default_role_STUDENT_when_roleIsNull")
    void shouldSetDefaultRole() {
        User userNoRole = User.builder().name("Teste").email("teste@ufu.br").build();
        when(userRepository.existsByEmail("teste@ufu.br")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(userNoRole);

        userService.create(userNoRole);

        assertThat(userNoRole.getRole()).isEqualTo("STUDENT");
        verify(userRepository).save(any(User.class));
    }

    // =============================================
    // FIND
    // =============================================

    @Test
    @DisplayName("should_return_only_active_users_when_findAll")
    void shouldReturnOnlyActiveUsers() {
        when(userRepository.findByActiveTrue()).thenReturn(List.of(sampleUser));

        List<User> result = userService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActive()).isTrue();
    }

    @Test
    @DisplayName("should_return_user_when_findById_exists")
    void shouldReturnUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        User result = userService.findById(1L);

        assertThat(result.getEmail()).isEqualTo("maria@ufu.br");
    }

    @Test
    @DisplayName("should_throw_UserNotFoundException_when_idDoesNotExist")
    void shouldThrowNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    // =============================================
    // UPDATE
    // =============================================

    @Test
    @DisplayName("should_update_user_when_emailDoesNotConflict")
    void shouldUpdateUser() {
        User updatedData =
                User.builder().name("Maria Santos").email("maria@ufu.br").phone("34888").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        User result = userService.update(1L, updatedData);

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("should_throw_DuplicateEmailException_when_updatingToExistingEmail")
    void shouldThrowDuplicateOnUpdate() {
        User updatedData = User.builder().name("Maria").email("outro@ufu.br").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmail("outro@ufu.br")).thenReturn(true);

        assertThatThrownBy(() -> userService.update(1L, updatedData))
                .isInstanceOf(DuplicateEmailException.class);
    }

    // =============================================
    // DEACTIVATE (Soft Delete)
    // =============================================

    @Test
    @DisplayName("should_set_active_false_when_deactivate")
    void shouldDeactivateUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        userService.deactivate(1L);

        assertThat(sampleUser.getActive()).isFalse();
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("should_throw_UserNotFoundException_when_deactivatingNonexistent")
    void shouldThrowNotFoundOnDeactivate() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deactivate(99L))
                .isInstanceOf(UserNotFoundException.class);
    }
}
