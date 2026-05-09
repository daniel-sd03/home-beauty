
package sodresoftwares.homebeauty.infra.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import sodresoftwares.homebeauty.infra.security.SecurityFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = GlobalExceptionHandlerTest.TestController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = SecurityFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerTest.TestController.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @PostMapping("/response-status-exception")
        public void throwResponseStatusException() {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Test business error");
        }

        @PostMapping("/bad-credentials")
        public void throwBadCredentialsException() {
            throw new BadCredentialsException("Invalid credentials");
        }

        @PostMapping("/validation-error")
        public void throwValidationException(@Valid @RequestBody TestDTO dto) {
            // This will trigger MethodArgumentNotValidException
        }

        @PostMapping("/malformed-json")
        public void throwMalformedJson(@RequestBody Object dummy) {
            // This will be triggered by sending invalid JSON
        }

        @GetMapping("/missing-param")
        public void throwMissingParam(@RequestParam String requiredParam) {
            // This will trigger MissingServletRequestParameterException if param is missing
        }

        @GetMapping("/type-mismatch/{id}")
        public void throwTypeMismatch(@PathVariable Long id) {
            // This will trigger MethodArgumentTypeMismatchException if id is not a number
        }

        @PostMapping("/generic-exception")
        public void throwGenericException() {
            throw new RuntimeException("Test generic error");
        }
    }

    record TestDTO(@NotBlank String name) {}

    @Test
    @DisplayName("Should handle ResponseStatusException with correct error response")
    void shouldHandleResponseStatusException() throws Exception {
        mockMvc.perform(post("/test/response-status-exception"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("400 BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Test business error"))
                .andExpect(jsonPath("$.path").value("/test/response-status-exception"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle BadCredentialsException with 401 Unauthorized")
    void shouldHandleBadCredentialsException() throws Exception {
        mockMvc.perform(post("/test/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid email or password."))
                .andExpect(jsonPath("$.path").value("/test/bad-credentials"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with validation errors")
    void shouldHandleValidationException() throws Exception {
        TestDTO invalidDto = new TestDTO("");

        mockMvc.perform(post("/test/validation-error")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("name: must not be blank"))
                .andExpect(jsonPath("$.path").value("/test/validation-error"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle HttpMessageNotReadableException with malformed JSON")
    void shouldHandleHttpMessageNotReadableException() throws Exception {
        mockMvc.perform(post("/test/malformed-json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed JSON request. Please verify the data format, such as correct date/time patterns (e.g., 'HH:mm'), exact Enum values, and proper JSON syntax."))
                .andExpect(jsonPath("$.path").value("/test/malformed-json"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle MissingServletRequestParameterException")
    void shouldHandleMissingServletRequestParameterException() throws Exception {
        mockMvc.perform(get("/test/missing-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Missing required parameter: requiredParam"))
                .andExpect(jsonPath("$.path").value("/test/missing-param"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle MethodArgumentTypeMismatchException")
    void shouldHandleMethodArgumentTypeMismatchException() throws Exception {
        mockMvc.perform(get("/test/type-mismatch/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid format for parameter: id"))
                .andExpect(jsonPath("$.path").value("/test/type-mismatch/not-a-number"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle generic Exception with 500 Internal Server Error")
    void shouldHandleGenericException() throws Exception {
        mockMvc.perform(post("/test/generic-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected server error occurred."))
                .andExpect(jsonPath("$.path").value("/test/generic-exception"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
