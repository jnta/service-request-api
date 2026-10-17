package com.jnta.auth.application.port.out;

import com.jnta.user.domain.User;

public interface AccessTokenIssuer {

    String issue(User user);
}
