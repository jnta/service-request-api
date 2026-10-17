package com.jnta.solicitation.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
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
class SolicitationApiIntegrationTest {

    private static final String PASSWORD = "client-password-123";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse(TestcontainersConfiguration.POSTGRES_IMAGE));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearData() {
        jdbcTemplate.update("delete from analyst_states");
        jdbcTemplate.update("delete from solicitations");
        jdbcTemplate.update("delete from users");
    }

    @Test
    @DisplayName("given an authenticated CLIENT, when a solicitation is created, then the client owns a DRAFT at BASIC_DATA")
    void shouldCreateDraftForAuthenticatedClient() throws Exception {
        var client = registerAndLogin("create@example.com");

        mockMvc.perform(post("/solicitations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken())))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.ownerId").value(client.userId()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currentStep").value("BASIC_DATA"));
    }

    @Test
    @DisplayName("given a client-owned solicitation, when it is requested, then the solicitation is returned")
    void shouldReturnOwnedSolicitation() throws Exception {
        var client = registerAndLogin("owner@example.com");
        var solicitationId = createSolicitation(client.accessToken());

        mockMvc.perform(get("/solicitations/{id}", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(solicitationId))
                .andExpect(jsonPath("$.ownerId").value(client.userId()))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("given another client's solicitation, when requested, then access is denied")
    void shouldDenyReadingAnotherClientsSolicitation() throws Exception {
        var owner = registerAndLogin("owner@example.com");
        var otherClient = registerAndLogin("other@example.com");
        var solicitationId = createSolicitation(owner.accessToken());

        mockMvc.perform(get("/solicitations/{id}", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherClient.accessToken())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("given an authenticated client, when a missing solicitation is requested, then not found is returned")
    void shouldReturnNotFoundForUnknownSolicitation() throws Exception {
        var client = registerAndLogin("reader@example.com");

        mockMvc.perform(get("/solicitations/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken())))
                .andExpect(status().isNotFound());
    }

    private TestIdentity registerAndLogin(String email) throws Exception {
        MvcResult registration = mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"name":"Test Client","email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = JsonPath.read(registration.getResponse().getContentAsString(), "$.id");

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        String token = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        return new TestIdentity(userId, token);
    }

    private String createSolicitation(String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/solicitations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isCreated())
                .andReturn();
        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location).isNotBlank();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record TestIdentity(String userId, String accessToken) {
    }
}
