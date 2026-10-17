package com.jnta.solicitation.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.jayway.jsonpath.JsonPath;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import com.jnta.TestcontainersConfiguration;
import com.jnta.shared.geography.BrazilianState;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.domain.SolicitationId;
import com.jnta.solicitation.domain.address.Address;
import com.jnta.solicitation.domain.address.Cep;
import com.jnta.solicitation.domain.basicdata.BasicData;
import com.jnta.solicitation.domain.basicdata.ServiceType;
import com.jnta.solicitation.domain.confirmation.Confirmation;
import com.jnta.solicitation.domain.money.Currency;
import com.jnta.solicitation.domain.money.Money;
import com.jnta.solicitation.domain.confirmation.Priority;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
    private static final WireMockServer VIA_CEP = new WireMockServer(options().dynamicPort());

    static {
        VIA_CEP.start();
    }

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse(TestcontainersConfiguration.POSTGRES_IMAGE));

    @DynamicPropertySource
    static void viacepBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("integration.viacep.base-url", VIA_CEP::baseUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SolicitationRepository solicitationRepository;

    @Autowired
    private Clock clock;

    @BeforeEach
    void clearData() {
        VIA_CEP.resetAll();
        jdbcTemplate.update("delete from analyst_states");
        jdbcTemplate.update("delete from solicitations");
        jdbcTemplate.update("delete from users");
    }

    @AfterAll
    static void stopViaCep() {
        VIA_CEP.stop();
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

    @Test
    @DisplayName("given the owner has a DRAFT, when valid basic data is submitted, then it is saved and the step advances")
    void shouldUpdateBasicDataForOwner() throws Exception {
        var client = registerAndLogin("basic-data@example.com");
        var solicitationId = createSolicitation(client.accessToken());

        mockMvc.perform(put("/solicitations/{id}/steps/basic-data", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken()))
                        .contentType("application/json")
                        .content(basicDataRequest("Equipment installation")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basicData.title").value("Equipment installation"))
                .andExpect(jsonPath("$.currentStep").value("ADDRESS"));

        mockMvc.perform(get("/solicitations/{id}", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basicData.serviceType").value("INSTALLATION"))
                .andExpect(jsonPath("$.currentStep").value("ADDRESS"));
    }

    @Test
    @DisplayName("given the owner has a DRAFT, when a short title is submitted, then validation fails and the step does not advance")
    void shouldRejectShortTitleWithoutChangingDraft() throws Exception {
        var client = registerAndLogin("invalid-basic-data@example.com");
        var solicitationId = createSolicitation(client.accessToken());

        mockMvc.perform(put("/solicitations/{id}/steps/basic-data", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken()))
                        .contentType("application/json")
                        .content(basicDataRequest("AB")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/solicitations/{id}", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStep").value("BASIC_DATA"))
                .andExpect(jsonPath("$.basicData").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("given another client's DRAFT, when basic data is updated, then access is denied")
    void shouldDenyBasicDataUpdateByAnotherClient() throws Exception {
        var owner = registerAndLogin("basic-owner@example.com");
        var otherClient = registerAndLogin("basic-other@example.com");
        var solicitationId = createSolicitation(owner.accessToken());

        mockMvc.perform(put("/solicitations/{id}/steps/basic-data", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherClient.accessToken()))
                        .contentType("application/json")
                        .content(basicDataRequest("Equipment installation")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("given an owned SUBMITTED solicitation, when basic data is updated, then a business conflict is returned")
    void shouldRejectBasicDataUpdateAfterSubmission() throws Exception {
        var owner = registerAndLogin("submitted-owner@example.com");
        var solicitationId = createSolicitation(owner.accessToken());
        submitSolicitation(UUID.fromString(solicitationId), owner.userId());

        mockMvc.perform(put("/solicitations/{id}/steps/basic-data", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner.accessToken()))
                        .contentType("application/json")
                        .content(basicDataRequest("Changed title")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("given a solicitation at ADDRESS and ViaCEP data, when the owner submits a CEP and number, then the address is populated")
    void shouldCompleteAddressUsingViaCep() throws Exception {
        var client = registerAndLogin("address@example.com");
        var solicitationId = createSolicitation(client.accessToken());
        completeBasicData(solicitationId, client.accessToken());
        stubViaCep("70040902", 200, """
                {"cep":"70040-902","logradouro":"Praça dos Três Poderes","complemento":"","unidade":"","bairro":"Zona Cívico-Administrativa","localidade":"Brasília","uf":"DF","estado":"Distrito Federal","regiao":"Centro-Oeste","ibge":"5300108","gia":"","ddd":"61","siafi":"9701"}
                """);

        mockMvc.perform(put("/solicitations/{id}/steps/address", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken()))
                        .contentType("application/json")
                        .content(addressRequest("70040-902", "123", "Sala 4")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address.cep").value("70040902"))
                .andExpect(jsonPath("$.address.street").value("Praça dos Três Poderes"))
                .andExpect(jsonPath("$.address.city").value("Brasília"))
                .andExpect(jsonPath("$.address.state").value("DF"))
                .andExpect(jsonPath("$.address.number").value("123"))
                .andExpect(jsonPath("$.currentStep").value("CONFIRMATION"));

        mockMvc.perform(get("/solicitations/{id}", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address.complement").value("Sala 4"));
    }

    @Test
    @DisplayName("given ViaCEP reports an unknown CEP, when the owner submits it, then the address remains unchanged")
    void shouldRejectUnknownCepWithoutChangingAddress() throws Exception {
        var client = registerAndLogin("invalid-cep@example.com");
        var solicitationId = createSolicitation(client.accessToken());
        completeBasicData(solicitationId, client.accessToken());
        stubViaCep("00000000", 200, "{\"erro\":true}");

        mockMvc.perform(put("/solicitations/{id}/steps/address", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken()))
                        .contentType("application/json")
                        .content(addressRequest("00000000", "123", null)))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/solicitations/{id}", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.currentStep").value("ADDRESS"));
    }

    @Test
    @DisplayName("given ViaCEP is unavailable, when the owner submits an address, then the draft remains unchanged")
    void shouldKeepDraftUnchangedWhenViaCepIsUnavailable() throws Exception {
        var client = registerAndLogin("viacep-unavailable@example.com");
        var solicitationId = createSolicitation(client.accessToken());
        completeBasicData(solicitationId, client.accessToken());
        stubViaCep("70040902", 503, "service unavailable");

        mockMvc.perform(put("/solicitations/{id}/steps/address", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken()))
                        .contentType("application/json")
                        .content(addressRequest("70040902", "123", null)))
                .andExpect(status().isServiceUnavailable());

        mockMvc.perform(get("/solicitations/{id}", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.currentStep").value("ADDRESS"));
    }

    @Test
    @DisplayName("given a solicitation at BASIC_DATA, when address data is submitted, then ViaCEP is not called")
    void shouldRejectAddressBeforeBasicDataWithoutCallingViaCep() throws Exception {
        var client = registerAndLogin("address-order@example.com");
        var solicitationId = createSolicitation(client.accessToken());

        mockMvc.perform(put("/solicitations/{id}/steps/address", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken()))
                        .contentType("application/json")
                        .content(addressRequest("70040902", "123", null)))
                .andExpect(status().isConflict());

        VIA_CEP.verify(0, WireMock.getRequestedFor(WireMock.anyUrl()));
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

    private void submitSolicitation(UUID solicitationId, String ownerId) {
        var stored = solicitationRepository.findById(new SolicitationId(solicitationId)).orElseThrow();
        var solicitation = stored.solicitation();
        solicitation.updateBasicData(new BasicData(
                new ServiceType("INSTALLATION"), "Original title", "Install equipment at the main office."));
        solicitation.updateAddress(new Address(
                new Cep("70040-902"), "Main Street", "Central", "Brasilia", BrazilianState.DF, "123", null));
        solicitation.updateConfirmation(
                new Confirmation(Priority.LOW, LocalDate.now(clock), new Money(0, Currency.BRL), true), clock);
        solicitation.submit(clock);
        solicitationRepository.save(stored.withSolicitation(solicitation));
        assertThat(solicitation.owner().value().toString()).isEqualTo(ownerId);
    }

    private static String basicDataRequest(String title) {
        return """
                {"serviceType":"INSTALLATION","title":"%s","description":"Install equipment at the main office."}
                """.formatted(title);
    }

    private void completeBasicData(String solicitationId, String accessToken) throws Exception {
        mockMvc.perform(put("/solicitations/{id}/steps/basic-data", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType("application/json")
                        .content(basicDataRequest("Equipment installation")))
                .andExpect(status().isOk());
    }

    private static String addressRequest(String cep, String number, String complement) {
        return """
                {"cep":"%s","number":"%s","complement":%s}
                """.formatted(cep, number, complement == null ? "null" : "\"" + complement + "\"");
    }

    private static void stubViaCep(String cep, int status, String body) {
        VIA_CEP.stubFor(WireMock.get(WireMock.urlEqualTo("/" + cep + "/json/"))
                .willReturn(WireMock.aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record TestIdentity(String userId, String accessToken) {
    }
}
