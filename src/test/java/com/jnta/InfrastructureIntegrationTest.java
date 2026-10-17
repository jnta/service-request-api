package com.jnta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class InfrastructureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Test
    @DisplayName("GET /actuator/health reports UP for the application and every dependency")
    void shouldReportHealthUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"))
                .andExpect(jsonPath("$.components.elasticsearch.status").value("UP"));
    }

    @Test
    @DisplayName("the Elasticsearch client reaches the cluster")
    void shouldConnectToElasticsearch() throws Exception {
        var info = elasticsearchClient.info();

        assertThat(info.clusterName()).isNotBlank();
        assertThat(info.version().number()).isNotBlank();
    }
}
