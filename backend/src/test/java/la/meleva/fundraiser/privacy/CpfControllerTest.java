package la.meleva.fundraiser.privacy;

import java.util.List;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.identity.AppUserDetails;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.cpf.encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.cpf.hmac-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CpfControllerTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    @Test
    void registersCpfMaskedAndRejectsInvalidAndDuplicate() throws Exception {
        var first = users.save(new User("Pessoa", "Um", "+5511999100001"));
        var second = users.save(new User("Pessoa", "Dois", "+5511999100002"));
        var authFirst = auth(first);
        var authSecond = auth(second);

        mvc.perform(put("/privacy/me/cpf").with(authentication(authFirst))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cpf\":\"11144477735\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(put("/privacy/me/cpf").with(authentication(authFirst)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cpf\":\"111.444.777-35\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(get("/privacy/me/cpf").with(authentication(authFirst)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedCpf").value("***.***.777-35"));

        mvc.perform(put("/privacy/me/cpf").with(authentication(authSecond)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cpf\":\"111.444.777-00\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/privacy/me/cpf").with(authentication(authSecond)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cpf\":\"11144477735\"}"))
                .andExpect(status().isConflict());

        var third = users.save(new User("Pessoa", "Três", "+5511999100003"));
        mvc.perform(get("/privacy/me/cpf").with(authentication(auth(third))))
                .andExpect(status().isNotFound());
    }

    private UsernamePasswordAuthenticationToken auth(User user) {
        var principal = new AppUserDetails(user.getId(), user.getPublicId(), user.getPhoneNumber(), List.of(UserRole.PARTICIPANT));
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
