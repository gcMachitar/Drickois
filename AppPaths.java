import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AppPaths {
    private static final String APP_DIR_NAME = "DrickSysApp";

    private AppPaths() {
    }

    public static Path getDataDirectory() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, APP_DIR_NAME);
        }

        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, APP_DIR_NAME);
        }

        return Path.of(System.getProperty("user.home", "."), "." + APP_DIR_NAME);
    }

    public static Path dataFile(String fileName) {
        return getDataDirectory().resolve(fileName);
    }

    public static Path receiptsDirectory() {
        return getDataDirectory().resolve("receipts");
    }

    public static void ensureDataDirectoryExists() throws IOException {
        Files.createDirectories(getDataDirectory());
        Files.createDirectories(receiptsDirectory());
    }

    public static void migrateWorkingFileIfMissing(String fileName) throws IOException {
        Path target = dataFile(fileName);
        if (Files.exists(target)) {
            return;
        }

        ensureDataDirectoryExists();
        Path source = Path.of(System.getProperty("user.dir", "."), fileName);
        if (Files.exists(source) && Files.isRegularFile(source)) {
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    public static void seedBundledFileIfMissing(String resourceName, String fileName) throws IOException {
        Path target = dataFile(fileName);
        if (Files.exists(target)) {
            return;
        }

        ensureDataDirectoryExists();
        try (InputStream inputStream = AppPaths.class.getResourceAsStream(resourceName)) {
            if (inputStream != null) {
                Files.copy(inputStream, target);
            }
        }
    }
}
