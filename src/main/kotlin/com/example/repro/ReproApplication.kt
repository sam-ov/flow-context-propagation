package com.example.repro

import io.micrometer.context.ContextRegistry
import io.micrometer.context.integration.Slf4jThreadLocalAccessor
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono

@SpringBootApplication
class ReproApplication

fun main(args: Array<String>) {
    runApplication<ReproApplication>(*args)
}

@Configuration
class MdcPropagationConfig {
    @PostConstruct
    fun configure() {
        Hooks.enableAutomaticContextPropagation()
        ContextRegistry.getInstance().registerThreadLocalAccessor(Slf4jThreadLocalAccessor())
    }
}

@Component
class MdcSeedFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> =
        chain.filter(exchange)
            .contextWrite { ctx ->
                ctx.put(Slf4jThreadLocalAccessor.KEY, mapOf("foo" to "bar"))
            }
}

@RestController
@RequestMapping("/diagnostics")
class DiagnosticsController {
    @GetMapping("/suspend")
    suspend fun suspendEndpoint(): DiagnosticResponse =
        buildDiagnosticResponse()

    @GetMapping("/flow")
    fun flowEndpoint(): Flow<DiagnosticResponse> =
        flow { emit(buildDiagnosticResponse()) }

    private suspend fun buildDiagnosticResponse() = withContext(Dispatchers.Default) {
        DiagnosticResponse(
            mdc = MDC.getCopyOfContextMap().orEmpty(),
            coroutineContext = currentCoroutineContext()
                .fold(emptyList()) { ee, e -> ee + e::class.simpleName!! }
        )
    }
}

data class DiagnosticResponse(
    val mdc: Map<String, String>,
    val coroutineContext: List<String>
)
