package dev.codelad.lunchservice.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Роли пользователей в системе.
 * Каждая роль имеет authority в формате ROLE_<NAME> для Spring Security.
 */
@Getter
public enum Role {

    ROLE_GLOBAL_ADMIN("ROLE_GLOBAL_ADMIN"),
    ROLE_FRANCHISE_OWNER("ROLE_FRANCHISE_OWNER"),
    ROLE_LOCATION_ADMIN("ROLE_LOCATION_ADMIN"),
    ROLE_USER("ROLE_USER"),
    ROLE_GUEST("ROLE_GUEST");

    private final String authority;

    Role(String authority) {
        this.authority = authority;
    }

    @JsonValue
    public String jsonValue() {
        return name();
    }

    /**
     * Получить короткое имя роли без префикса ROLE_.
     * <p>
     * Примеры:
     * <ul>
     *   <li>{@code ROLE_GLOBAL_ADMIN} → {@code GLOBAL_ADMIN}</li>
     *   <li>{@code ROLE_FRANCHISE_OWNER} → {@code FRANCHISE_OWNER}</li>
     *   <li>{@code ROLE_USER} → {@code USER}</li>
     * </ul>
     *
     * @return короткое имя роли
     */
    public String getShortName() {
        if (authority != null && authority.startsWith("ROLE_")) {
            return authority.substring(5);
        }
        return authority;
    }

    @JsonCreator
    public static Role fromString(String value) {
        for (Role role : Role.values()) {
            if (role.name().equalsIgnoreCase(value) || role.authority.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Неизвестная роль: " + value);
    }
}
