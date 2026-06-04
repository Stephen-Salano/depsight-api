package io.depsight.api.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Global request filter responsible for managing the distributed tracing context. *
 *
 * <p>This filter intercepts every incoming HTTP request to establish a unique {@code X-Trace-ID}
 * for log correlation and request tracking across the system architecture.
 *
 * <h3>Execution Flow:</h3>
 *
 * <ol>
 *   <li><strong>Trace ID Resolution:</strong> Checks the incoming request for an {@code X-Trace-ID}
 *       header. Uses the provided header value if present (e.g., from a React frontend); otherwise,
 *       generates a fresh {@link java.util.UUID}.
 *   <li><strong>MDC Injection:</strong> Binds the resolved trace ID to the SLF4J Mapped Diagnostic
 *       Context (MDC). This automatically enriches all downstream log statements in the current
 *       thread with the trace context (e.g., transforming {@code INFO Analysis started} into {@code
 *       INFO [traceId=abc-123] Analysis started}).
 *   <li><strong>Response Propagation:</strong> Attaches the trace ID to the outbound response
 *       headers as {@code X-Trace-ID}, allowing clients to reference it for debugging and log
 *       aggregation tools like Loki or Grafana.
 *   <li><strong>Context Cleanup:</strong> Ensures the MDC is explicitly cleared in a {@code
 *       finally} block. Because Spring reuses container threads, failing to clear the context would
 *       leak trace data into subsequent requests on the same thread.
 * </ol>
 *
 * <h3>Filter Precedence:</h3>
 *
 * <p>Annotated with {@code @Order(Ordered.HIGHEST_PRECEDENCE)} to guarantee this filter executes
 * before security configurations, logging frameworks, or any other application filters. This
 * ensures that early-lifecycle logs contain the appropriate tracing context.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestIdFilter extends OncePerRequestFilter {
  private static final String TRACE_ID_HEADER = "X-Trace-ID";
  private static final String MDC_TRACE_ID = "traceId";

  @Override
  protected void doFilterInternal(
      @NotNull HttpServletRequest request,
      @NotNull HttpServletResponse response,
      @NotNull FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String traceId = request.getHeader(TRACE_ID_HEADER);
      if (traceId == null || traceId.isBlank()) {
        traceId = UUID.randomUUID().toString();
      }
      MDC.put(MDC_TRACE_ID, traceId);
      response.setHeader(TRACE_ID_HEADER, traceId);

      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }
}
