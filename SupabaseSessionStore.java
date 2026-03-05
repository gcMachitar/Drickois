import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class SupabaseSessionStore {
    private static final String SESSION_FILE = "supabase_session.properties";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String APP_DIR_NAME = "DrickSys";

    private Path resolveSessionFilePath() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, APP_DIR_NAME, SESSION_FILE);
        }
        return Path.of(System.getProperty("user.home", "."), "." + APP_DIR_NAME, SESSION_FILE);
    }

    public void save(SupabaseSession session) throws IOException {
        Properties properties = new Properties();
        properties.setProperty(KEY_ACCESS_TOKEN, session.getAccessToken());
        properties.setProperty(KEY_REFRESH_TOKEN, session.getRefreshToken());
        properties.setProperty(KEY_USER_ID, session.getUserId());
        properties.setProperty(KEY_EMAIL, session.getEmail() == null ? "" : session.getEmail());

        Path sessionPath = resolveSessionFilePath();
        Files.createDirectories(sessionPath.getParent());
        try (FileOutputStream outputStream = new FileOutputStream(sessionPath.toFile())) {
            properties.store(outputStream, "Supabase Session");
        }
    }

    public SupabaseSession load() throws IOException {
        Properties properties = new Properties();
        Path sessionPath = resolveSessionFilePath();
        if (!Files.exists(sessionPath)) {
            return null;
        }

        try (FileInputStream inputStream = new FileInputStream(sessionPath.toFile())) {
            properties.load(inputStream);
        }

        String accessToken = properties.getProperty(KEY_ACCESS_TOKEN, "");
        String refreshToken = properties.getProperty(KEY_REFRESH_TOKEN, "");
        String userId = properties.getProperty(KEY_USER_ID, "");
        String email = properties.getProperty(KEY_EMAIL, "");

        if (accessToken.isEmpty() || refreshToken.isEmpty() || userId.isEmpty()) {
            return null;
        }

        return new SupabaseSession(accessToken, refreshToken, userId, email);
    }

    public void clear() throws IOException {
        Files.deleteIfExists(resolveSessionFilePath());
    }
}
