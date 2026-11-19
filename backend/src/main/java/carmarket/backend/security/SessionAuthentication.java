package carmarket.backend.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;

public class SessionAuthentication extends AbstractAuthenticationToken {
    private final SessionUser user;

    public SessionAuthentication(SessionUser user) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
        this.user = user;
        setAuthenticated(true);
    }

    @Override public SessionUser getPrincipal() { return user; }
    @Override public Object getCredentials() { return ""; }
    @Override public String getName() { return user.login(); }
}
