package la.meleva.fundraiser.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** P20-F26: o cookie CSRF precisa ser legível pelas páginas do frontend, fora do context-path `/api`. */
class CsrfCookieTest {
    @Test
    void tokenCookieUsesRootPathAndStaysReadableByTheFrontend() {
        var repository = SecurityConfig.csrfTokenRepository();
        var request = new MockHttpServletRequest("GET", "/api/campaigns/public/x");
        request.setContextPath("/api");
        var response = new MockHttpServletResponse();

        repository.saveToken(repository.generateToken(request), request, response);

        var cookie = response.getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.isHttpOnly()).isFalse();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
    }

    /** API em subdomínio do site (ex.: api.meleva.la + app.meleva.la): cookie escopado no domínio pai. */
    @Test
    void tokenCookieCanBeScopedToParentDomain() {
        var repository = SecurityConfig.csrfTokenRepository(".meleva.la");
        var request = new MockHttpServletRequest("GET", "/api/campaigns/public/x");
        request.setContextPath("/api");
        var response = new MockHttpServletResponse();

        repository.saveToken(repository.generateToken(request), request, response);

        var cookie = response.getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getDomain()).isEqualTo(".meleva.la");
    }
}
