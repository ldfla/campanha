package la.meleva.fundraiser.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Fluxo de autenticação por código (P20-F01) para testes: cadastro/solicitação → verificação → sessão. */
public final class AuthTestSupport {
    /** Código aceito apenas pelos adaptadores DEMO (perfis default/local/test). */
    public static final String DEMO_CODE = "000000";

    private AuthTestSupport() { }

    public static void register(MockMvc mvc, String firstName, String surname, String phone) throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"" + firstName + "\",\"surname\":\"" + surname
                                + "\",\"phoneNumber\":\"" + phone + "\"}"))
                .andExpect(status().isAccepted());
    }

    /** Verifica o código e devolve o cookie de sessão autenticada (usuário sem papel administrativo). */
    public static Cookie verify(MockMvc mvc, String phone) throws Exception {
        var result = mvc.perform(post("/auth/login/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phone + "\",\"code\":\"" + DEMO_CODE + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return Objects.requireNonNull(result.getResponse().getCookie("SESSION"), "SESSION cookie not found");
    }

    public static Cookie registerAndSignIn(MockMvc mvc, String firstName, String surname, String phone) throws Exception {
        register(mvc, firstName, surname, phone);
        return verify(mvc, phone);
    }

    /** Login por código seguido de MFA DEMO na mesma sessão (papéis administrativos). */
    public static Cookie signInWithMfa(MockMvc mvc, String phone) throws Exception {
        var first = mvc.perform(post("/auth/login/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phone + "\",\"code\":\"" + DEMO_CODE + "\"}"))
                .andExpect(status().isForbidden()).andReturn();
        var pending = Objects.requireNonNull(first.getResponse().getCookie("SESSION"), "SESSION cookie not found");
        var result = mvc.perform(post("/auth/mfa/verify").cookie(pending).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phone + "\",\"code\":\"" + DEMO_CODE + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return Objects.requireNonNull(result.getResponse().getCookie("SESSION"), "SESSION cookie not found");
    }
}
