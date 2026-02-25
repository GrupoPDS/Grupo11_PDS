package br.ufu.pds.library.entrypoint.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.ufu.pds.library.core.exceptions.DuplicateEmailException;
import br.ufu.pds.library.entrypoint.api.dto.CreateUserRequest;
import br.ufu.pds.library.entrypoint.api.dto.UserResponse;
import br.ufu.pds.library.infrastructure.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = UserController.class)
public class UserControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;

    @Test
    void should_return_201_when_post_valid() throws Exception {
        CreateUserRequest req = new CreateUserRequest("João", "joao@example.com", "123456789", "STUDENT");
        UserResponse resp = new UserResponse(1L, "João", "joao@example.com", "123456789", "STUDENT", true, LocalDateTime.now());

        when(userService.create(any(CreateUserRequest.class))).thenReturn(resp);

        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void should_return_400_when_post_invalidEmail() throws Exception {
        CreateUserRequest req = new CreateUserRequest("João", "invalid-email", "123456789", "STUDENT");

        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_409_when_post_duplicateEmail() throws Exception {
        CreateUserRequest req = new CreateUserRequest("João", "joao@example.com", "123456789", "STUDENT");
        when(userService.create(any(CreateUserRequest.class))).thenThrow(new DuplicateEmailException(req.email()));

        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void should_return_404_when_get_missing() throws Exception {
        when(userService.getById(99L)).thenThrow(new br.ufu.pds.library.core.exceptions.UserNotFoundException(99L));

        mockMvc.perform(get("/api/users/99")).andExpect(status().isNotFound());
    }
}

