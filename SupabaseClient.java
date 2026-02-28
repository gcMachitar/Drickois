import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SupabaseClient {
    public static class ActionLogRecord {
        private final String actionType;
        private final String details;
        private final String createdAt;

        public ActionLogRecord(String actionType, String details, String createdAt) {
            this.actionType = actionType;
            this.details = details;
            this.createdAt = createdAt;
        }

        public String getActionType() {
            return actionType;
        }

        public String getDetails() {
            return details;
        }

        public String getCreatedAt() {
            return createdAt;
        }
    }

    public static class InventoryRecord {
        private final String itemName;
        private final String category;
        private final int quantity;
        private final double price;
        private final String dateAdded;
        private final String dateUpdated;

        public InventoryRecord(String itemName, String category, int quantity, double price, String dateAdded, String dateUpdated) {
            this.itemName = itemName;
            this.category = category;
            this.quantity = quantity;
            this.price = price;
            this.dateAdded = dateAdded;
            this.dateUpdated = dateUpdated;
        }

        public String getItemName() {
            return itemName;
        }

        public String getCategory() {
            return category;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getPrice() {
            return price;
        }

        public String getDateAdded() {
            return dateAdded;
        }

        public String getDateUpdated() {
            return dateUpdated;
        }
    }

    private final String supabaseUrl;
    private final String publishableKey;
    private final HttpClient httpClient;

    public SupabaseClient(String supabaseUrl, String publishableKey) {
        this.supabaseUrl = trimTrailingSlash(supabaseUrl);
        this.publishableKey = publishableKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public SupabaseSession signUp(String email, String password) throws IOException, InterruptedException {
        String body = "{\"email\":\"" + jsonEscape(email) + "\",\"password\":\"" + jsonEscape(password) + "\"}";
        HttpResponse<String> response = sendJsonRequest("POST", "/auth/v1/signup", body, null, false);
        return parseSession(response.body(), false);
    }

    public SupabaseSession signIn(String email, String password) throws IOException, InterruptedException {
        String body = "{\"email\":\"" + jsonEscape(email) + "\",\"password\":\"" + jsonEscape(password) + "\"}";
        HttpResponse<String> response = sendJsonRequest(
                "POST",
                "/auth/v1/token?grant_type=password",
                body,
                null,
                false
        );
        return parseSession(response.body(), true);
    }

    public SupabaseSession refreshSession(String refreshToken) throws IOException, InterruptedException {
        String body = "{\"refresh_token\":\"" + jsonEscape(refreshToken) + "\"}";
        HttpResponse<String> response = sendJsonRequest(
                "POST",
                "/auth/v1/token?grant_type=refresh_token",
                body,
                null,
                false
        );
        return parseSession(response.body(), true);
    }

    public void upsertProfile(SupabaseSession session, UserData userData) throws IOException, InterruptedException {
        String body = "[{\"user_id\":\"" + jsonEscape(session.getUserId()) + "\","
                + "\"username\":\"" + jsonEscape(userData.getUsername()) + "\","
                + "\"full_name\":\"" + jsonEscape(userData.getName()) + "\","
                + "\"age\":" + userData.getAge() + ","
                + "\"address\":\"" + jsonEscape(userData.getAddress()) + "\","
                + "\"email\":\"" + jsonEscape(userData.getEmail()) + "\","
                + "\"phone_number\":\"" + jsonEscape(userData.getPhoneNumber()) + "\"}]";

        sendJsonRequest("POST", "/rest/v1/profiles?on_conflict=user_id", body, session.getAccessToken(), true);
    }

    public void insertInventoryItem(
            SupabaseSession session,
            String itemName,
            String category,
            int quantity,
            double price,
            String dateAdded,
            String dateUpdated
    ) throws IOException, InterruptedException {
        String fullQuantityBody = "[{\"user_id\":\"" + jsonEscape(session.getUserId()) + "\","
                + "\"item_name\":\"" + jsonEscape(itemName) + "\","
                + "\"category\":\"" + jsonEscape(category) + "\","
                + "\"quantity\":" + quantity + ","
                + "\"price\":" + price + ","
                + "\"date_added\":\"" + jsonEscape(dateAdded) + "\","
                + "\"date_updated\":\"" + jsonEscape(dateUpdated) + "\"}]";

        String fullQtyBody = "[{\"user_id\":\"" + jsonEscape(session.getUserId()) + "\","
                + "\"item_name\":\"" + jsonEscape(itemName) + "\","
                + "\"category\":\"" + jsonEscape(category) + "\","
                + "\"qty\":" + quantity + ","
                + "\"price\":" + price + ","
                + "\"date_added\":\"" + jsonEscape(dateAdded) + "\","
                + "\"date_updated\":\"" + jsonEscape(dateUpdated) + "\"}]";

        String fallbackQuantityBody = "[{\"user_id\":\"" + jsonEscape(session.getUserId()) + "\","
                + "\"item_name\":\"" + jsonEscape(itemName) + "\","
                + "\"quantity\":" + quantity + ","
                + "\"price\":" + price + ","
                + "\"date_added\":\"" + jsonEscape(dateAdded) + "\","
                + "\"date_updated\":\"" + jsonEscape(dateUpdated) + "\"}]";

        String fallbackQtyBody = "[{\"user_id\":\"" + jsonEscape(session.getUserId()) + "\","
                + "\"item_name\":\"" + jsonEscape(itemName) + "\","
                + "\"qty\":" + quantity + ","
                + "\"price\":" + price + ","
                + "\"date_added\":\"" + jsonEscape(dateAdded) + "\","
                + "\"date_updated\":\"" + jsonEscape(dateUpdated) + "\"}]";

        String minimalQuantityBody = "[{\"user_id\":\"" + jsonEscape(session.getUserId()) + "\","
                + "\"item_name\":\"" + jsonEscape(itemName) + "\","
                + "\"quantity\":" + quantity + ","
                + "\"price\":" + price + "}]";

        String minimalQtyBody = "[{\"user_id\":\"" + jsonEscape(session.getUserId()) + "\","
                + "\"item_name\":\"" + jsonEscape(itemName) + "\","
                + "\"qty\":" + quantity + ","
                + "\"price\":" + price + "}]";

        String[] payloads = {
                fullQuantityBody,
                fullQtyBody,
                fallbackQuantityBody,
                fallbackQtyBody,
                minimalQuantityBody,
                minimalQtyBody
        };

        IOException lastException = null;
        for (String payload : payloads) {
            try {
                sendJsonRequest("POST", "/rest/v1/inventory", payload, session.getAccessToken(), false);
                return;
            } catch (IOException e) {
                if (isSchemaMismatch(e)) {
                    lastException = e;
                    continue;
                }
                throw e;
            }
        }
        throw (lastException != null ? lastException : new IOException("Could not insert inventory row."));
    }

    public void updateInventoryItemByName(
            SupabaseSession session,
            String originalItemName,
            String itemName,
            String category,
            int quantity,
            double price,
            String dateUpdated
    ) throws IOException, InterruptedException {
        String encodedOriginalName = urlEncode(originalItemName);
        String path = "/rest/v1/inventory?user_id=eq." + urlEncode(session.getUserId())
                + "&item_name=eq." + encodedOriginalName;

        String body = "{"
                + "\"item_name\":\"" + jsonEscape(itemName) + "\","
                + "\"category\":\"" + jsonEscape(category) + "\","
                + "\"quantity\":" + quantity + ","
                + "\"price\":" + price + ","
                + "\"date_updated\":\"" + jsonEscape(dateUpdated) + "\""
                + "}";
        String qtyBody = "{"
                + "\"item_name\":\"" + jsonEscape(itemName) + "\","
                + "\"category\":\"" + jsonEscape(category) + "\","
                + "\"qty\":" + quantity + ","
                + "\"price\":" + price + ","
                + "\"date_updated\":\"" + jsonEscape(dateUpdated) + "\""
                + "}";

        String fallbackBody = "{"
                + "\"item_name\":\"" + jsonEscape(itemName) + "\","
                + "\"quantity\":" + quantity + ","
                + "\"price\":" + price + ","
                + "\"date_updated\":\"" + jsonEscape(dateUpdated) + "\""
                + "}";

        String fallbackQtyBody = "{"
                + "\"item_name\":\"" + jsonEscape(itemName) + "\","
                + "\"qty\":" + quantity + ","
                + "\"price\":" + price + ","
                + "\"date_updated\":\"" + jsonEscape(dateUpdated) + "\""
                + "}";

        String minimalBody = "{"
                + "\"item_name\":\"" + jsonEscape(itemName) + "\","
                + "\"quantity\":" + quantity + ","
                + "\"price\":" + price
                + "}";

        String minimalQtyBody = "{"
                + "\"item_name\":\"" + jsonEscape(itemName) + "\","
                + "\"qty\":" + quantity + ","
                + "\"price\":" + price
                + "}";

        String[] payloads = {body, qtyBody, fallbackBody, fallbackQtyBody, minimalBody, minimalQtyBody};
        IOException lastException = null;
        for (String payload : payloads) {
            try {
                sendJsonRequest("PATCH", path, payload, session.getAccessToken(), false);
                return;
            } catch (IOException e) {
                if (isSchemaMismatch(e)) {
                    lastException = e;
                    continue;
                }
                throw e;
            }
        }
        throw (lastException != null ? lastException : new IOException("Could not update inventory row."));
    }

    public void deleteInventoryItemByName(SupabaseSession session, String itemName) throws IOException, InterruptedException {
        String path = "/rest/v1/inventory?user_id=eq." + urlEncode(session.getUserId())
                + "&item_name=eq." + urlEncode(itemName);
        sendJsonRequest("DELETE", path, null, session.getAccessToken(), false);
    }

    public List<InventoryRecord> fetchInventory(SupabaseSession session) throws IOException, InterruptedException {
        String fullPath = "/rest/v1/inventory?select=item_name,category,quantity,price,date_added,date_updated,created_at,updated_at"
                + "&user_id=eq." + urlEncode(session.getUserId())
                + "&order=created_at.desc";
        try {
            HttpResponse<String> response = sendJsonRequest("GET", fullPath, null, session.getAccessToken(), false);
            return parseInventoryRecords(response.body());
        } catch (IOException e) {
            if (!isMissingColumnError(e, "category") && !isMissingColumnError(e, "date_added")
                    && !isMissingColumnError(e, "date_updated")) {
                throw e;
            }
        }

        String fallbackPath = "/rest/v1/inventory?select=item_name,quantity,price,created_at,updated_at"
                + "&user_id=eq." + urlEncode(session.getUserId())
                + "&order=created_at.desc";
        try {
            HttpResponse<String> fallbackResponse = sendJsonRequest("GET", fallbackPath, null, session.getAccessToken(), false);
            return parseInventoryRecords(fallbackResponse.body());
        } catch (IOException e) {
            if (!isMissingColumnError(e, "quantity")) {
                throw e;
            }
        }

        String qtyPath = "/rest/v1/inventory?select=item_name,qty,price,created_at,updated_at"
                + "&user_id=eq." + urlEncode(session.getUserId())
                + "&order=created_at.desc";
        HttpResponse<String> qtyResponse = sendJsonRequest("GET", qtyPath, null, session.getAccessToken(), false);
        return parseInventoryRecords(qtyResponse.body());
    }

    public void logAction(SupabaseSession session, String actionType, String details) throws IOException, InterruptedException {
        String body = "[{\"user_id\":\"" + jsonEscape(session.getUserId()) + "\","
                + "\"action_type\":\"" + jsonEscape(actionType) + "\","
                + "\"details\":\"" + jsonEscape(details) + "\"}]";
        sendJsonRequest("POST", "/rest/v1/action_logs", body, session.getAccessToken(), false);
    }

    public List<ActionLogRecord> fetchActionLogs(SupabaseSession session) throws IOException, InterruptedException {
        String path = "/rest/v1/action_logs?select=action_type,details,created_at"
                + "&user_id=eq." + urlEncode(session.getUserId())
                + "&order=created_at.desc";
        HttpResponse<String> response = sendJsonRequest("GET", path, null, session.getAccessToken(), false);
        return parseActionLogRecords(response.body());
    }

    private HttpResponse<String> sendJsonRequest(
            String method,
            String path,
            String body,
            String bearerToken,
            boolean upsert
    ) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(supabaseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("apikey", publishableKey)
                .header("Content-Type", "application/json");

        if (bearerToken != null && !bearerToken.isEmpty()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }

        if (upsert) {
            builder.header("Prefer", "return=representation,resolution=merge-duplicates");
        } else {
            builder.header("Prefer", "return=representation");
        }

        switch (method) {
            case "GET" -> builder.GET();
            case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
            case "PATCH" -> builder.method("PATCH", HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
            case "DELETE" -> builder.DELETE();
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Supabase request failed (" + response.statusCode() + "): " + response.body());
        }
        return response;
    }

    private SupabaseSession parseSession(String jsonBody, boolean requireTokens) throws IOException {
        String accessToken = extractJsonString(jsonBody, "access_token");
        String refreshToken = extractJsonString(jsonBody, "refresh_token");
        String userId = extractNestedUserId(jsonBody);
        String email = extractJsonString(jsonBody, "email");

        if (userId == null) {
            throw new IOException("Missing required session fields in response: " + jsonBody);
        }
        if (requireTokens && (accessToken == null || refreshToken == null)) {
            throw new IOException("Missing tokens in auth response: " + jsonBody);
        }

        return new SupabaseSession(
                accessToken == null ? "" : accessToken,
                refreshToken == null ? "" : refreshToken,
                userId,
                email
        );
    }

    private static String extractJsonString(String jsonBody, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(jsonBody);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String extractNestedUserId(String jsonBody) {
        Pattern pattern = Pattern.compile("\"user\"\\s*:\\s*\\{[^}]*\"id\"\\s*:\\s*\"([^\"]+)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(jsonBody);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return extractJsonString(jsonBody, "user_id");
    }

    private static List<InventoryRecord> parseInventoryRecords(String json) throws IOException {
        List<InventoryRecord> records = new ArrayList<>();
        for (String objectJson : splitTopLevelObjects(json)) {
            String itemName = extractJsonString(objectJson, "item_name");
            String category = extractJsonString(objectJson, "category");
            if (category == null || category.isBlank()) {
                category = extractJsonString(objectJson, "item_category");
            }
            if (category == null || category.isBlank()) {
                category = "Other";
            }
            int quantity = extractJsonInt(objectJson, "quantity");
            if (quantity == 0) {
                quantity = extractJsonInt(objectJson, "qty");
            }
            double price = extractJsonDouble(objectJson, "price");
            String dateAdded = extractJsonString(objectJson, "date_added");
            if (dateAdded == null || dateAdded.isBlank()) {
                dateAdded = extractJsonString(objectJson, "created_at");
            }
            String dateUpdated = extractJsonString(objectJson, "date_updated");
            if (dateUpdated == null || dateUpdated.isBlank()) {
                dateUpdated = extractJsonString(objectJson, "updated_at");
            }

            if (itemName == null) {
                throw new IOException("Malformed inventory row: " + objectJson);
            }
            records.add(new InventoryRecord(
                    itemName,
                    category,
                    quantity,
                    price,
                    dateAdded == null ? "" : dateAdded,
                    dateUpdated == null ? "" : dateUpdated
            ));
        }
        return records;
    }

    private static List<ActionLogRecord> parseActionLogRecords(String json) {
        List<ActionLogRecord> records = new ArrayList<>();
        for (String objectJson : splitTopLevelObjects(json)) {
            String actionType = extractJsonString(objectJson, "action_type");
            String details = extractJsonString(objectJson, "details");
            String createdAt = extractJsonString(objectJson, "created_at");
            records.add(new ActionLogRecord(
                    actionType == null ? "" : actionType,
                    details == null ? "" : details,
                    createdAt == null ? "" : createdAt
            ));
        }
        return records;
    }

    private static List<String> splitTopLevelObjects(String json) {
        List<String> objects = new ArrayList<>();
        if (json == null || json.isBlank() || json.equals("[]")) {
            return objects;
        }

        int depth = 0;
        boolean inString = false;
        int objectStart = -1;
        char prev = '\0';

        for (int i = 0; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '"' && prev != '\\') {
                inString = !inString;
            }
            if (!inString) {
                if (ch == '{') {
                    if (depth == 0) {
                        objectStart = i;
                    }
                    depth++;
                } else if (ch == '}') {
                    depth--;
                    if (depth == 0 && objectStart >= 0) {
                        objects.add(json.substring(objectStart, i + 1));
                        objectStart = -1;
                    }
                }
            }
            prev = ch;
        }
        return objects;
    }

    private static int extractJsonInt(String jsonBody, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*(-?\\d+)");
        Matcher matcher = pattern.matcher(jsonBody);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    private static double extractJsonDouble(String jsonBody, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(jsonBody);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0.0;
    }

    private static boolean isMissingColumnError(IOException e, String columnName) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        return message.contains("Could not find the '" + columnName + "' column");
    }

    private static boolean isSchemaMismatch(IOException e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        return message.contains("Could not find the '") && message.contains("' column");
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
