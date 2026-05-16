package sodresoftwares.homebeauty.infra.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sodresoftwares.homebeauty.controllers.AuthenticationController;
import sodresoftwares.homebeauty.repositories.CategoryRepository;
import sodresoftwares.homebeauty.repositories.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for SecurityConfigurations
 * Validates public routes, strict role-based restrictions, and generic authentication blocks.
 * Protected against accidental deletion of critical admin paths.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SecurityConfigurations Tests")
class SecurityConfigurationsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AuthenticationController authenticationController;

    @MockitoBean
    private CategoryRepository categoryRepository;

    // ==========================================
    // PUBLIC ROUTES (.permitAll())
    // ==========================================

    @Test
    @DisplayName("Should allow public access to login endpoint")
    void shouldAllowPublicAccessToLogin() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"test\",\"password\":\"123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow public access to GET categories")
    void shouldAllowPublicAccessToCategories() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk());
    }

    // ==========================================
    // CRITICAL ROUTE SECURITY (ADMIN)
    // ==========================================

    @Test
    @DisplayName("Should block admin endpoints for unauthenticated users")
    void shouldBlockAdminEndpointsForUnauthenticated() throws Exception {
        mockMvc.perform(patch("/auth/1/role/admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should allow admin endpoints for users with ADMIN role")
    void shouldAllowAdminEndpointsForAdmin() throws Exception {
        mockMvc.perform(patch("/auth/1/role/admin"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PROFESSIONAL")
    @DisplayName("Should forbid admin promotion endpoint for normal professionals")
    void shouldForbidAdminPromotionForNormalProfessionals() throws Exception {
        mockMvc.perform(patch("/auth/1/role/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PROFESSIONAL")
    @DisplayName("Should forbid demote endpoint for normal professionals to prevent privilege escalation")
    void shouldForbidDemoteEndpointForNormalProfessionals() throws Exception {
        mockMvc.perform(patch("/auth/1/role/demote"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PROFESSIONAL")
    @DisplayName("Should forbid user search endpoint for normal Professionals")
    void shouldForbidUserSearchForNormalProfessionals() throws Exception {
        mockMvc.perform(get("/users/search"))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // PROTECTION NETWORK (.anyRequest())
    // ==========================================

    @Test
    @DisplayName("Should block generic protected endpoints when no token is present")
    void shouldBlockGenericProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/services/all"))
                .andExpect(status().isUnauthorized());
    }
}