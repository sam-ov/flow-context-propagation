package com.example.repro

import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiagnosticsControllerTests {
    @LocalServerPort
    private var port: Int = 0

    private val client: WebTestClient by lazy {
        WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    @Test
    fun `suspend endpoint has propagated mdc`() {
        val response = client
            .get()
            .uri("/diagnostics/suspend")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<DiagnosticResponse>()
            .returnResult()
            .responseBody

        assertSoftly {
            it.assertThat(response?.mdc).isNotNull.containsEntry("foo", "bar")
            it.assertThat(response?.coroutineContext).isNotNull.contains("PropagationContextElement")
        }
    }

    @Test
    fun `flow endpoint has propagated mdc`() {
        val response = client
            .get()
            .uri("/diagnostics/flow")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList<DiagnosticResponse>()
            .returnResult()
            .responseBody
            .orEmpty()
            .first()

        // This assertion is intentionally expected to fail to demonstrate the bug.
        assertSoftly {
            it.assertThat(response.mdc).containsEntry("foo", "bar")
            it.assertThat(response.coroutineContext).contains("PropagationContextElement")
        }
    }
}
