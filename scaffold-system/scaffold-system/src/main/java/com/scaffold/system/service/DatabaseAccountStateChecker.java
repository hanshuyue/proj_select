package com.scaffold.system.service;

import com.scaffold.framework.security.AccountStateChecker;
import com.scaffold.system.domain.AuthUserRow;
import com.scaffold.system.mapper.AuthMapper;
import org.springframework.stereotype.Component;

@Component
public class DatabaseAccountStateChecker implements AccountStateChecker {
    private final AuthMapper authMapper;

    public DatabaseAccountStateChecker(AuthMapper authMapper) { this.authMapper = authMapper; }

    @Override
    public State check(String loginAccount) {
        AuthUserRow user = authMapper.selectUserByUsername(loginAccount);
        return user == null
                ? new State(false, false)
                : new State(Integer.valueOf(1).equals(user.getStatus()), Boolean.TRUE.equals(user.getMustChangePassword()));
    }
}
