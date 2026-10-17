package com.jnta.auth.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.jnta.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthenticationIntegrationTest {

    private static final String CLIENT_PASSWORD = "client-password-123";
    private static final String ADMIN_PASSWORD = "admin-password-123";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse(TestcontainersConfiguration.POSTGRES_IMAGE));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void clearUsers() {
        jdbcTemplate.update("delete from analyst_states");
        jdbcTemplate.update("delete from solicitations");
        jdbcTemplate.update("delete from users");
    }

    @Test
    @DisplayName("given an unregistered email, when a client registers, then a CLIENT account stores a BCrypt hash")
    void shouldRegisterClientWithHashedPassword() throws Exception {
        MvcResult response = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Jonata","email":"jonata@example.com","password":"client-password-123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andReturn();

        String userId = JsonPath.read(response.getResponse().getContentAsString(), "$.id");
        String passwordHash = jdbcTemplate.queryForObject(
                "select password_hash from users where id = ?", String.class, UUID.fromString(userId));

        assertThat(passwordHash).isNotEqualTo(CLIENT_PASSWORD);
        assertThat(passwordEncoder.matches(CLIENT_PASSWORD, passwordHash)).isTrue();
    }

    @Test
    @DisplayName("given an existing email, when another client registers, then the API returns conflict")
    void shouldRejectDuplicateEmail() throws Exception {
        registerClient("duplicate@example.com", CLIENT_PASSWORD);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Another Client","email":"duplicate@example.com","password":"client-password-123"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("given a registered client, when valid credentials are submitted, then a signed JWT is returned")
    void shouldAuthenticateClientAndIssueJwt() throws Exception {
        registerClient("login@example.com", CLIENT_PASSWORD);

        MvcResult response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"login@example.com","password":"client-password-123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        String token = JsonPath.read(response.getResponse().getContentAsString(), "$.accessToken");
        var claims = jwtDecoder.decode(token);
        assertThat(claims.getClaimAsString("role")).isEqualTo("CLIENT");
        assertThat(claims.getSubject()).isNotBlank();
    }

    @Test
    @DisplayName("given a registered user, when an incorrect password is provided, then authentication is rejected")
    void shouldRejectInvalidCredentials() throws Exception {
        registerClient("invalid-login@example.com", CLIENT_PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"invalid-login@example.com","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("given an authenticated ADMIN, when an analyst is created with state coverage, then coverage is persisted")
    void shouldCreateAnalystWithCoverage() throws Exception {
        String adminToken = createAdminAndLogin();

        MvcResult response = mockMvc.perform(post("/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Analyst","email":"analyst@example.com","password":"analyst-password-123","role":"ANALYST","coverage":["DF","GO"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ANALYST"))
                .andExpect(jsonPath("$.coverage.length()").value(2))
                .andReturn();

        String id = JsonPath.read(response.getResponse().getContentAsString(), "$.id");
        Integer coverageCount = jdbcTemplate.queryForObject(
                "select count(*) from analyst_states where analyst_id = ?", Integer.class, UUID.fromString(id));

        assertThat(coverageCount).isEqualTo(2);
    }

    @Test
    @DisplayName("given an authenticated CLIENT, when creating an ADMIN is attempted, then access is denied")
    void shouldPreventClientFromCreatingAdmin() throws Exception {
        registerClient("client@example.com", CLIENT_PASSWORD);
        String clientToken = login("client@example.com", CLIENT_PASSWORD);

        mockMvc.perform(post("/admin/users")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Escalated","email":"escalated@example.com","password":"admin-password-123","role":"ADMIN"}
                                """))
                .andExpect(status().isForbidden());
    }

    private void registerClient(String email, String password) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test Client","email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isCreated());
    }

    private String createAdminAndLogin() throws Exception {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into users (id, name, email, password_hash, user_role) values (?, ?, ?, ?, ?)",
                id, "Admin", "admin@example.com", passwordEncoder.encode(ADMIN_PASSWORD), "ADMIN");
        return login("admin@example.com", ADMIN_PASSWORD);
    }

    private String login(String email, String password) throws Exception {
        MvcResult response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(response.getResponse().getContentAsString(), "$.accessToken");
    }
}
