package dev.codelad.lunchservice.enums;

/**
 * Тип блюда.
 */
public enum DishType {

    MAIN("MAIN"),
    ADDON("ADDON");

    private final String value;

    DishType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DishType fromValue(String value) {
        for (DishType dishType : values()) {
            if (dishType.value.equals(value)) {
                return dishType;
            }
        }
        throw new IllegalArgumentException("Неизвестный тип блюда: " + value);
    }
}
