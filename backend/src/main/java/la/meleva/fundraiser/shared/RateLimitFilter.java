package la.meleva.fundraiser.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_SECONDS = 60;

    private final Map<String, RateWindow> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getServletPath();
        if (!isRateLimited(path)) {
            chain.doFilter(request, response);
            return;
        }

        String key = clientKey(request) + ":" + path;
        var window = windows.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new RateWindow();
            }
            return existing;
        });

        if (window.incrementAndGet() > MAX_ATTEMPTS) {
            response.setStatus(429);
            response.setContentType("application/problem+json");
            response.getWriter().write("""
                {"type":"about:blank","title":"Muitas tentativas","status":429,"detail":"Limite de requisições excedido. Tente novamente em breve."}""");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isRateLimited(String path) {
        return path.startsWith("/auth/login") || path.startsWith("/auth/register");
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RateWindow {
        private final Instant start = Instant.now();
        private final AtomicInteger count = new AtomicInteger(0);

        boolean isExpired() {
            return Instant.now().isAfter(start.plusSeconds(WINDOW_SECONDS));
        }

        int incrementAndGet() {
            return count.incrementAndGet();
        }
    }
}
