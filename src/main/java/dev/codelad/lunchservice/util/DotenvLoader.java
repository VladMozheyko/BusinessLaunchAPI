package dev.codelad.lunchservice.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Загрузчик переменных окружения из файла .env.
 * <p>
 * Используется ТОЛЬКО для локальной разработки в IDEA.
 * В Docker отключается через ENABLE_DOTENV_LOADER=false.
 * <p>
 * Структура .env файла:
 * <ul>
 *   <li>=== LOCAL === → переменные с префиксом LOCAL_</li>
 *   <li>=== DEV === → переменные с префиксом DEV_</li>
 *   <li>=== PROD === → переменные с префиксом PROD_</li>
 * </ul>
 */
@Slf4j
public final class DotenvLoader {

    private DotenvLoader() {}

    private static final String[] SECTIONS = {"LOCAL", "DEV", "PROD"};

    public static void load() {
        // В Docker не загружаем — переменные уже есть из env_file
        if ("false".equalsIgnoreCase(System.getenv("ENABLE_DOTENV_LOADER"))) {
            log.debug("DotenvLoader отключен (ENABLE_DOTENV_LOADER=false)");
            return;
        }

        String profile = getActiveProfile();

        // Если профиль не задан снаружи — читаем из .env
        if (profile == null) {
            profile = readProfileFromFile(".env");
        }

        if (profile == null) {
            log.error("SPRING_PROFILES_ACTIVE не задан. Загрузка невозможна.");
            return;
        }
        
        log.info("Загрузка .env для профиля: {}", profile);

        loadFile();
    }

    /**
     * Прочитать профиль из файла (без применения).
     */
    private static String readProfileFromFile(String filename) {
        var file = new java.io.File(filename);
        if (!file.exists()) {
            log.error(".env файл не найден в {}", file.getAbsolutePath());
            return null;
        }

        try (var reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("SPRING_PROFILES_ACTIVE=")) {
                    return line.substring("SPRING_PROFILES_ACTIVE=".length()).trim();
                }
            }
        } catch (IOException e) {
            errorMessage(filename, e.getMessage());
        }
        return null;
    }

    /**
     * Загрузить переменные из файла для заданного профиля.
     */
    private static void loadFile() {
        Map<String, String> vars = readEnvFile();
        applyProperties(vars);
        log.info("Загружено {} переменных из {}", vars.size(), ".env");
    }

    /**
     * Прочитать .env файл.
     * Если файл не найден — возвращает пустую карту.
     */
    private static Map<String, String> readEnvFile() {
        var file = new java.io.File(".env");
        if (!file.exists()) {
            log.error("Файл .env не найден в {}", file.getAbsolutePath());
            return Map.of();
        }

        Map<String, String> vars = new HashMap<>();
        String currentSection = null;

        try (var reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                currentSection = processLine(line, currentSection, "LOCAL", vars);
            }
        } catch (IOException e) {
            errorMessage(file.getName(), e.getMessage());
        }
        return vars;
    }
    
    private static void errorMessage(String fileName, String message) {
        log.error("Ошибка чтения {}: {}", fileName, message);
    }

    /**
     * Обработать одну строку файла.
     * Возвращает обновлённый currentSection.
     */
    private static String processLine(
            String line, String currentSection, String profile, Map<String, String> vars) {

        if (isBlankOrComment(line)) {
            return updateSection(line, currentSection);
        }
        return parseKeyValue(line, currentSection, profile, vars);
    }

    /**
     * Проверить: пустая строка или комментарий.
     */
    private static boolean isBlankOrComment(String line) {
        return line.isEmpty() || line.startsWith("#");
    }

    /**
     * Обновить текущую секцию по заголовку.
     */
    private static String updateSection(String line, String currentSection) {
        if (line.startsWith("===")) {
            for (String section : SECTIONS) {
                if (line.contains("=== " + section + " ===")) {
                    return section;
                }
            }
        }
        return currentSection;
    }

    /**
     * Парсить KEY=VALUE строку.
     * Возвращает currentSection без изменений.
     */
    private static String parseKeyValue(
            String line, String currentSection, String profile, Map<String, String> vars) {

        int eq = line.indexOf('=');
        if (eq > 0) {
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            value = resolveVariables(value, vars);

            if (currentSection == null || currentSection.equals(profile)) {
                vars.put(key, value);
            }
        }
        return currentSection;
    }

    /**
     * Применить загруженные переменные к System properties.
     */
    private static void applyProperties(Map<String, String> vars) {
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }
    }

    private static String resolveVariables(String value, Map<String, String> vars) {
        int start = value.indexOf("${");
        if (start < 0) return value;

        int end = value.indexOf('}', start);
        if (end < 0) return value;

        String varName = value.substring(start + 2, end);
        String varValue = vars.get(varName);
        if (varValue == null) {
            varValue = System.getProperty(varName);
        }
        if (varValue == null) {
            varValue = System.getenv(varName);
        }
        if (varValue == null) {
            varValue = "";
        }

        return value.substring(0, start) + varValue + value.substring(end + 1);
    }

    private static String getActiveProfile() {
        String profile = System.getProperty("spring.profiles.active");
        if (profile == null) {
            profile = System.getenv("SPRING_PROFILES_ACTIVE");
        }
        return profile;
    }
}
