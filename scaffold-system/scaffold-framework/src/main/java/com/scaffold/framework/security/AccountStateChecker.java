package com.scaffold.framework.security;

/** Keeps token authentication in sync with the current database account state. */
public interface AccountStateChecker {
    State check(String loginAccount);

    record State(boolean enabled, boolean mustChangePassword) {
        public static State active() { return new State(true, false); }
    }
}
