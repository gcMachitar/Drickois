import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SupabaseConfig {
    private static final String DEFAULT_URL = "https://dqndvgmklfnwtvrwgioa.supabase.co";
    private static final String PROPERTIES_FILE = "supabase.properties";
    private static final Properties FILE_PROPERTIES = loadFileProperties();

    private SupabaseConfig() {
    }

    public static String getSupabaseUrl() {
        String value = System.getenv("SUPABASE_URL");
        if (value == null || value.isBlank()) {
            value = FILE_PROPERTIES.getProperty("SUPABASE_URL", "");
        }
        if (value == null || value.isBlank()) {
            return DEFAULT_URL;
        }
        return value.trim();
    }

    public static String getPublishableKey() {
        String value = System.getenv("SUPABASE_PUBLISHABLE_KEY");
        if (value == null || value.isBlank()) {
            value = System.getenv("SUPABASE_ANON_KEY");
        }
        if (value == null || value.isBlank()) {
            value = FILE_PROPERTIES.getProperty("SUPABASE_PUBLISHABLE_KEY", "");
        }
        if (value == null || value.isBlank()) {
            value = FILE_PROPERTIES.getProperty("SUPABASE_ANON_KEY", "");
        }
        return value == null ? "" : value.trim();
    }

    private static Properties loadFileProperties() {
        Properties properties = new Properties();
        for (Path path : candidatePaths()) {
            if (!Files.exists(path)) {
                continue;
            }
            try (FileInputStream inputStream = new FileInputStream(path.toFile())) {
                properties.load(inputStream);
                return properties;
            } catch (IOException ignored) {
            }
        }
        return properties;
    }

    private static Path[] candidatePaths() {
        String userDir = System.getProperty("user.dir", ".");
        return new Path[] {
                Path.of(userDir, PROPERTIES_FILE),
                Path.of(userDir, "Drickoi's", PROPERTIES_FILE),
                Path.of(PROPERTIES_FILE),
                Path.of("Drickoi's", PROPERTIES_FILE)
        };
    }
}
