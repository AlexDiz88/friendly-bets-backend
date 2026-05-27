package net.friendly_bets.security.details;

import net.friendly_bets.models.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class AuthenticatedUser implements UserDetails {

    private final User user;
    private final boolean requireEmailConfirmedForLogin;

    public AuthenticatedUser(User user, boolean requireEmailConfirmedForLogin) {
        this.user = user;
        this.requireEmailConfirmedForLogin = requireEmailConfirmedForLogin;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleAsAuthority = user.getRole().name();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleAsAuthority);
        return Collections.singleton(authority);
    }

    @Override
    public String getPassword() {
        return user.getHashPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        if (!requireEmailConfirmedForLogin) {
            return true;
        }
        return !Boolean.FALSE.equals(user.getEmailIsConfirmed());
    }

    public User getUser() {
        return user;
    }
}
