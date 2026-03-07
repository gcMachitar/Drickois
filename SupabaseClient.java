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

    public static class SaleItem {
        private final String itemName;
        private final String category;
        private final int soldQuantity;
        private final double unitPrice;
        private final int remainingQuantity;

        public SaleItem(String itemName, String category, int soldQuantity, double unitPrice, int remainingQuantity) {
            this.itemName = itemName;
            this.category = category;
            this.soldQuantity = soldQuantity;
            this.unitPrice = unitPrice;
            this.remainingQuantity = remainingQuantity;
        }

        public String getItemName() {
            return itemName;
        }

        public String getCategory() {
            return category;
        }

        public int getSoldQuantity() {
            return soldQuantity;
        }

        public double getUnitPrice() {
            return unitPrice;
        }

        public int getRemainingQuantity() {
            return remainingQuantity;
        }
    }

    public static class SupplierRecord {
        private final long supplierId;
        private final String supplierName;
        private final String contactPerson;
        private final String phone;
        private final String email;
        private final String address;
        private final String status;

        public SupplierRecord(long supplierId, String supplierName, String contactPerson, String phone, String email, String address, String status) {
            this.supplierId = supplierId;
            this.supplierName = supplierName;
            this.contactPerson = contactPerson;
            this.phone = phone;
            this.email = email;
            this.address = address;
            this.status = status;
        }

        public long getSupplierId() {
            return supplierId;
        }

        public String getSupplierName() {
            return supplierName;
        }

        public String getContactPerson() {
            return contactPerson;
        }

        public String getPhone() {
            return phone;
        }

        public String getEmail() {
            return email;
        }

        public String getAddress() {
            return address;
        }

        public String getStatus() {
            return status;
        }
    }

    public static class SaleHistoryLineRecord {
        private final long saleId;
        private final String saleDate;
        private final String productName;
        private final int quantity;
        private final double price;

        public SaleHistoryLineRecord(long saleId, String saleDate, String productName, int quantity, double price) {
            this.saleId = saleId;
            this.saleDate = saleDate;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
        }

        public long getSaleId() {
            return saleId;
        }

        public String getSaleDate() {
            return saleDate;
        }

        public String getProductName() {
            return productName;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getPrice() {
            return price;
        }
    }

    public static class ExpirationRecord {
        private final long expirationId;
        private final String itemName;
        private final String unitType;
        private final int unitQuantity;
        private final String expirationDate;

        public ExpirationRecord(long expirationId, String itemName, String unitType, int unitQuantity, String expirationDate) {
            this.expirationId = expirationId;
            this.itemName = itemName;
            this.unitType = unitType;
            this.unitQuantity = unitQuantity;
            this.expirationDate = expirationDate;
        }

        public long getExpirationId() {
            return expirationId;
        }

        public String getItemName() {
            return itemName;
        }

        public String getUnitType() {
            return unitType;
        }

        public int getUnitQuantity() {
            return unitQuantity;
        }

        public String getExpirationDate() {
            return expirationDate;
        }
    }

    public static class IngredientRecord {
        private final String productName;
        private final String itemName;
        private final String unitType;
        private final int quantityNeeded;

        public IngredientRecord(String productName, String itemName, String unitType, int quantityNeeded) {
            this.productName = productName;
            this.itemName = itemName;
            this.unitType = unitType;
            this.quantityNeeded = quantityNeeded;
        }

        public String getProductName() {
            return productName;
        }

        public String getItemName() {
            return itemName;
        }

        public String getUnitType() {
            return unitType;
        }

        public int getQuantityNeeded() {
            return quantityNeeded;
        }
    }

    public static class StockOutItemRecord {
        private final long stockoutItemId;
        private final String stockoutDate;
        private final String reason;
        private final String itemName;
        private final int quantity;
        private final double cost;

        public StockOutItemRecord(long stockoutItemId, String stockoutDate, String reason, String itemName, int quantity, double cost) {
            this.stockoutItemId = stockoutItemId;
            this.stockoutDate = stockoutDate;
            this.reason = reason;
            this.itemName = itemName;
            this.quantity = quantity;
            this.cost = cost;
        }

        public long getStockoutItemId() {
            return stockoutItemId;
        }

        public String getStockoutDate() {
            return stockoutDate;
        }

        public String getReason() {
            return reason;
        }

        public String getItemName() {
            return itemName;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getCost() {
            return cost;
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
        long existingUserId = findUserIdByUsername(session, userData.getUsername());
        if (existingUserId > 0) {
            String patchBody = "{"
                    + "\"full_name\":\"" + jsonEscape(userData.getName()) + "\","
                    + "\"role\":\"staff\","
                    + "\"password\":\"" + jsonEscape(userData.getPassword()) + "\","
                    + "\"is_active\":true"
                    + "}";
            sendJsonRequest("PATCH", "/rest/v1/user?user_id=eq." + existingUserId, patchBody, session.getAccessToken(), false);
            return;
        }

        String insertBody = "[{\"full_name\":\"" + jsonEscape(userData.getName()) + "\","
                + "\"role\":\"staff\","
                + "\"username\":\"" + jsonEscape(userData.getUsername()) + "\","
                + "\"password\":\"" + jsonEscape(userData.getPassword()) + "\","
                + "\"is_active\":true}]";
        sendJsonRequest("POST", "/rest/v1/user", insertBody, session.getAccessToken(), false);
    }

    public void insertInventoryItem(
            SupabaseSession session,
            String itemName,
            String category,
            int quantity,
            double price,
            String dateAdded,
            String dateUpdated,
            boolean linkToProduct
    ) throws IOException, InterruptedException {
        long categoryId = ensureCategory(session, category);
        long itemId = insertItem(session, itemName, category, quantity, price);
        if (linkToProduct) {
            ensureProductAndIngredient(session, itemName, category, categoryId, itemId);
        } else {
            deactivateProductByName(session, itemName, itemId);
        }
    }

    public void updateInventoryItemByName(
            SupabaseSession session,
            String originalItemName,
            String itemName,
            String category,
            int quantity,
            double price,
            String dateUpdated,
            boolean linkToProduct
    ) throws IOException, InterruptedException {
        long categoryId = ensureCategory(session, category);
        long itemId = findItemIdByName(session, originalItemName);
        if (itemId <= 0) {
            throw new IOException("Item not found: " + originalItemName);
        }

        String path = "/rest/v1/item?item_id=eq." + itemId;
        String body = "{"
                + "\"item_name\":\"" + jsonEscape(itemName) + "\","
                + "\"unit_type\":\"" + jsonEscape(category) + "\","
                + "\"quantity_on_hand\":" + quantity + ","
                + "\"unit_cost\":" + price
                + "}";
        sendJsonRequest("PATCH", path, body, session.getAccessToken(), false);

        long refreshedItemId = findItemIdByName(session, itemName);
        if (refreshedItemId <= 0) {
            refreshedItemId = itemId;
        }
        if (linkToProduct) {
            ensureProductAndIngredient(session, itemName, category, categoryId, refreshedItemId);
        } else {
            deactivateProductByName(session, originalItemName, refreshedItemId);
            if (!originalItemName.equalsIgnoreCase(itemName)) {
                deactivateProductByName(session, itemName, refreshedItemId);
            }
        }
    }

    public void deleteInventoryItemByName(SupabaseSession session, String itemName) throws IOException, InterruptedException {
        long itemId = findItemIdByName(session, itemName);
        long productId = findProductIdByName(session, itemName);

        if (productId > 0) {
            sendJsonRequest(
                    "DELETE",
                    "/rest/v1/ingredients?product_id=eq." + productId,
                    null,
                    session.getAccessToken(),
                    false
            );
            sendJsonRequest(
                    "PATCH",
                    "/rest/v1/product?product_id=eq." + productId,
                    "{\"is_active\":false}",
                    session.getAccessToken(),
                    false
            );
        }

        if (itemId > 0) {
            sendJsonRequest("DELETE", "/rest/v1/item?item_id=eq." + itemId, null, session.getAccessToken(), false);
        }
    }

    public List<InventoryRecord> fetchInventory(SupabaseSession session) throws IOException, InterruptedException {
        String path = "/rest/v1/item?select=item_name,unit_type,quantity_on_hand,unit_cost&order=item_id.desc";
        HttpResponse<String> response = sendJsonRequest("GET", path, null, session.getAccessToken(), false);
        return parseInventoryRecords(response.body());
    }

    public void updateInventoryQuantityByName(
            SupabaseSession session,
            String itemName,
            int quantity
    ) throws IOException, InterruptedException {
        long itemId = findItemIdByName(session, itemName);
        if (itemId <= 0) {
            throw new IOException("Item not found: " + itemName);
        }
        sendJsonRequest(
                "PATCH",
                "/rest/v1/item?item_id=eq." + itemId,
                "{\"quantity_on_hand\":" + quantity + "}",
                session.getAccessToken(),
                false
        );
    }

    public void placeSaleForInventoryItem(
            SupabaseSession session,
            String itemName,
            String category,
            int soldQuantity,
            double unitPrice,
            int remainingQuantity
    ) throws IOException, InterruptedException {
        long userId = ensureOperationalUser(session);
        long itemId = findItemIdByName(session, itemName);
        if (itemId <= 0) {
            throw new IOException("Item not found for sale: " + itemName);
        }

        long categoryId = ensureCategory(session, category);
        long productId = ensureProductAndIngredient(session, itemName, category, categoryId, itemId);

        long saleId = insertSale(session, userId);
        insertSalesDetail(session, saleId, productId, soldQuantity, unitPrice);

        String updateStockBody = "{\"quantity_on_hand\":" + remainingQuantity + "}";
        sendJsonRequest(
                "PATCH",
                "/rest/v1/item?item_id=eq." + itemId,
                updateStockBody,
                session.getAccessToken(),
                false
        );
    }

    public long placeSale(SupabaseSession session, List<SaleItem> saleItems) throws IOException, InterruptedException {
        if (saleItems == null || saleItems.isEmpty()) {
            return -1;
        }

        long userId = ensureOperationalUser(session);
        long saleId = insertSale(session, userId);

        for (SaleItem saleItem : saleItems) {
            long itemId = findItemIdByName(session, saleItem.getItemName());
            if (itemId <= 0) {
                throw new IOException("Item not found for sale: " + saleItem.getItemName());
            }

            long categoryId = ensureCategory(session, saleItem.getCategory());
            long productId = ensureProductAndIngredient(session, saleItem.getItemName(), saleItem.getCategory(), categoryId, itemId);

            insertSalesDetail(session, saleId, productId, saleItem.getSoldQuantity(), saleItem.getUnitPrice());

            String updateStockBody = "{\"quantity_on_hand\":" + saleItem.getRemainingQuantity() + "}";
            sendJsonRequest(
                    "PATCH",
                    "/rest/v1/item?item_id=eq." + itemId,
                    updateStockBody,
                    session.getAccessToken(),
                    false
            );
        }
        return saleId;
    }

    public long placeProductSale(SupabaseSession session, List<SaleItem> saleItems) throws IOException, InterruptedException {
        if (saleItems == null || saleItems.isEmpty()) {
            return -1;
        }

        long userId = ensureOperationalUser(session);
        long saleId = insertSale(session, userId);

        for (SaleItem saleItem : saleItems) {
            long categoryId = ensureCategory(session, saleItem.getCategory());
            long productId = ensureStandaloneProduct(session, saleItem.getItemName(), saleItem.getCategory(), categoryId);
            insertSalesDetail(session, saleId, productId, saleItem.getSoldQuantity(), saleItem.getUnitPrice());
        }
        return saleId;
    }

    public void logAction(SupabaseSession session, String actionType, String details) throws IOException, InterruptedException {
        // No action_logs table exists in the ERD, so this is intentionally a no-op.
    }

    public List<ActionLogRecord> fetchActionLogs(SupabaseSession session) throws IOException, InterruptedException {
        return new ArrayList<>();
    }

    public List<SupplierRecord> fetchSuppliers(SupabaseSession session) throws IOException, InterruptedException {
        String path = "/rest/v1/supplier?select=supplier_id,supplier_name,contact_person,phone,email,address,status&order=supplier_id.desc";
        HttpResponse<String> response = sendJsonRequest("GET", path, null, session.getAccessToken(), false);
        return parseSupplierRecords(response.body());
    }

    public List<SaleHistoryLineRecord> fetchSalesHistory(SupabaseSession session) throws IOException, InterruptedException {
        String path = "/rest/v1/sales_details?select=quantity,price,sales(sale_id,sale_date),product(product_name)&order=sale_detail_id.desc";
        HttpResponse<String> response = sendJsonRequest("GET", path, null, session.getAccessToken(), false);
        return parseSaleHistoryLineRecords(response.body());
    }

    public void addSupplier(
            SupabaseSession session,
            String supplierName,
            String contactPerson,
            String phone,
            String email,
            String address,
            String status
    ) throws IOException, InterruptedException {
        String body = "[{\"supplier_name\":\"" + jsonEscape(supplierName) + "\","
                + "\"contact_person\":\"" + jsonEscape(contactPerson) + "\","
                + "\"phone\":\"" + jsonEscape(phone) + "\","
                + "\"email\":\"" + jsonEscape(email) + "\","
                + "\"address\":\"" + jsonEscape(address) + "\","
                + "\"status\":\"" + jsonEscape(status) + "\"}]";
        sendJsonRequest("POST", "/rest/v1/supplier", body, session.getAccessToken(), false);
    }

    public void deleteSupplierById(SupabaseSession session, long supplierId) throws IOException, InterruptedException {
        sendJsonRequest("DELETE", "/rest/v1/supplier?supplier_id=eq." + supplierId, null, session.getAccessToken(), false);
    }

    public void updateSupplierById(
            SupabaseSession session,
            long supplierId,
            String supplierName,
            String contactPerson,
            String phone,
            String email,
            String address,
            String status
    ) throws IOException, InterruptedException {
        String body = "{"
                + "\"supplier_name\":\"" + jsonEscape(supplierName) + "\","
                + "\"contact_person\":\"" + jsonEscape(contactPerson) + "\","
                + "\"phone\":\"" + jsonEscape(phone) + "\","
                + "\"email\":\"" + jsonEscape(email) + "\","
                + "\"address\":\"" + jsonEscape(address) + "\","
                + "\"status\":\"" + jsonEscape(status) + "\""
                + "}";
        sendJsonRequest("PATCH", "/rest/v1/supplier?supplier_id=eq." + supplierId, body, session.getAccessToken(), false);
    }

    public List<ExpirationRecord> fetchExpirations(SupabaseSession session) throws IOException, InterruptedException {
        String path = "/rest/v1/expiration?select=expiration_id,unit_type,unit_quantity,expiration_date,item(item_name)&order=expiration_id.desc";
        HttpResponse<String> response = sendJsonRequest("GET", path, null, session.getAccessToken(), false);
        return parseExpirationRecords(response.body());
    }

    public void addExpiration(
            SupabaseSession session,
            String itemName,
            String unitType,
            int unitQuantity,
            String expirationDate
    ) throws IOException, InterruptedException {
        long itemId = findItemIdByName(session, itemName);
        if (itemId <= 0) {
            throw new IOException("Item not found: " + itemName);
        }
        String body = "[{\"item_id\":" + itemId + ","
                + "\"unit_type\":\"" + jsonEscape(unitType) + "\","
                + "\"unit_quantity\":" + unitQuantity + ","
                + "\"expiration_date\":\"" + jsonEscape(expirationDate) + "\"}]";
        sendJsonRequest("POST", "/rest/v1/expiration", body, session.getAccessToken(), false);
    }

    public void deleteExpirationById(SupabaseSession session, long expirationId) throws IOException, InterruptedException {
        sendJsonRequest("DELETE", "/rest/v1/expiration?expiration_id=eq." + expirationId, null, session.getAccessToken(), false);
    }

    public void updateExpirationById(
            SupabaseSession session,
            long expirationId,
            String itemName,
            String unitType,
            int unitQuantity,
            String expirationDate
    ) throws IOException, InterruptedException {
        long itemId = findItemIdByName(session, itemName);
        if (itemId <= 0) {
            throw new IOException("Item not found: " + itemName);
        }
        String body = "{"
                + "\"item_id\":" + itemId + ","
                + "\"unit_type\":\"" + jsonEscape(unitType) + "\","
                + "\"unit_quantity\":" + unitQuantity + ","
                + "\"expiration_date\":\"" + jsonEscape(expirationDate) + "\""
                + "}";
        sendJsonRequest("PATCH", "/rest/v1/expiration?expiration_id=eq." + expirationId, body, session.getAccessToken(), false);
    }

    public List<IngredientRecord> fetchIngredients(SupabaseSession session) throws IOException, InterruptedException {
        String path = "/rest/v1/ingredients?select=unit_type,quantity_needed,product(product_name),item(item_name)&order=product_id.asc";
        HttpResponse<String> response = sendJsonRequest("GET", path, null, session.getAccessToken(), false);
        return parseIngredientRecords(response.body());
    }

    public void addOrUpdateIngredient(
            SupabaseSession session,
            String productName,
            String itemName,
            String unitType,
            int quantityNeeded
    ) throws IOException, InterruptedException {
        long productId = findProductIdByName(session, productName);
        long itemId = findItemIdByName(session, itemName);
        if (productId <= 0) {
            throw new IOException("Product not found: " + productName);
        }
        if (itemId <= 0) {
            throw new IOException("Item not found: " + itemName);
        }

        sendJsonRequest(
                "DELETE",
                "/rest/v1/ingredients?product_id=eq." + productId + "&item_id=eq." + itemId + "&unit_type=eq." + urlEncode(unitType),
                null,
                session.getAccessToken(),
                false
        );
        String body = "[{\"product_id\":" + productId + ","
                + "\"item_id\":" + itemId + ","
                + "\"unit_type\":\"" + jsonEscape(unitType) + "\","
                + "\"quantity_needed\":" + quantityNeeded + "}]";
        sendJsonRequest("POST", "/rest/v1/ingredients", body, session.getAccessToken(), false);
    }

    public void deleteIngredient(
            SupabaseSession session,
            String productName,
            String itemName,
            String unitType
    ) throws IOException, InterruptedException {
        long productId = findProductIdByName(session, productName);
        long itemId = findItemIdByName(session, itemName);
        if (productId <= 0 || itemId <= 0) {
            return;
        }
        sendJsonRequest(
                "DELETE",
                "/rest/v1/ingredients?product_id=eq." + productId + "&item_id=eq." + itemId + "&unit_type=eq." + urlEncode(unitType),
                null,
                session.getAccessToken(),
                false
        );
    }

    public List<StockOutItemRecord> fetchStockOutItems(SupabaseSession session) throws IOException, InterruptedException {
        String path = "/rest/v1/stock_out_item?select=stockout_item_id,quantity,cost,stock_out(stockout_date,reason),item(item_name)&order=stockout_item_id.desc";
        HttpResponse<String> response = sendJsonRequest("GET", path, null, session.getAccessToken(), false);
        return parseStockOutItemRecords(response.body());
    }

    public void recordItemStockOut(
            SupabaseSession session,
            String itemName,
            int quantity,
            double cost,
            String reason
    ) throws IOException, InterruptedException {
        long userId = ensureOperationalUser(session);
        long itemId = findItemIdByName(session, itemName);
        if (itemId <= 0) {
            throw new IOException("Item not found: " + itemName);
        }

        int onHand = fetchItemQuantityOnHand(session, itemId);
        if (quantity > onHand) {
            throw new IOException("Stock out exceeds quantity on hand. Available: " + onHand);
        }

        long stockoutId = insertStockOut(session, userId, reason);
        String detailBody = "[{\"stockout_id\":" + stockoutId + ","
                + "\"item_id\":" + itemId + ","
                + "\"quantity\":" + quantity + ","
                + "\"cost\":" + cost + "}]";
        sendJsonRequest("POST", "/rest/v1/stock_out_item", detailBody, session.getAccessToken(), false);

        int newQuantity = onHand - quantity;
        sendJsonRequest(
                "PATCH",
                "/rest/v1/item?item_id=eq." + itemId,
                "{\"quantity_on_hand\":" + newQuantity + "}",
                session.getAccessToken(),
                false
        );
    }

    private long ensureOperationalUser(SupabaseSession session) throws IOException, InterruptedException {
        String username = session.getEmail();
        if (username == null || username.isBlank()) {
            String userIdToken = session.getUserId() == null ? "unknown" : session.getUserId().replace("-", "");
            username = "user_" + userIdToken.substring(0, Math.min(12, userIdToken.length()));
        }

        long existing = findUserIdByUsername(session, username);
        if (existing > 0) {
            return existing;
        }

        String body = "[{\"full_name\":\"" + jsonEscape(username) + "\","
                + "\"role\":\"staff\","
                + "\"username\":\"" + jsonEscape(username) + "\","
                + "\"password\":\"__supabase_auth__\","
                + "\"is_active\":true}]";
        sendJsonRequest("POST", "/rest/v1/user", body, session.getAccessToken(), false);

        long created = findUserIdByUsername(session, username);
        if (created <= 0) {
            throw new IOException("Unable to create/resolve ERD user.");
        }
        return created;
    }

    private long ensureCategory(SupabaseSession session, String categoryName) throws IOException, InterruptedException {
        String normalized = (categoryName == null || categoryName.isBlank()) ? "Other" : categoryName.trim();
        long existingId = findCategoryIdByName(session, normalized);
        if (existingId > 0) {
            return existingId;
        }

        String body = "[{\"category_name\":\"" + jsonEscape(normalized) + "\","
                + "\"description\":\"Auto-generated from app\"}]";
        sendJsonRequest("POST", "/rest/v1/category", body, session.getAccessToken(), false);

        long createdId = findCategoryIdByName(session, normalized);
        if (createdId <= 0) {
            throw new IOException("Unable to create/resolve category: " + normalized);
        }
        return createdId;
    }

    private long insertItem(
            SupabaseSession session,
            String itemName,
            String unitType,
            int quantity,
            double unitCost
    ) throws IOException, InterruptedException {
        String body = "[{\"item_name\":\"" + jsonEscape(itemName) + "\","
                + "\"unit_type\":\"" + jsonEscape(unitType) + "\","
                + "\"quantity_on_hand\":" + quantity + ","
                + "\"unit_cost\":" + unitCost + "}]";
        HttpResponse<String> response = sendJsonRequest("POST", "/rest/v1/item", body, session.getAccessToken(), false);
        long id = extractJsonLong(response.body(), "item_id");
        if (id > 0) {
            return id;
        }
        long lookup = findItemIdByName(session, itemName);
        if (lookup > 0) {
            return lookup;
        }
        throw new IOException("Unable to resolve inserted item id for: " + itemName);
    }

    private long ensureProductAndIngredient(
            SupabaseSession session,
            String productName,
            String unit,
            long categoryId,
            long itemId
    ) throws IOException, InterruptedException {
        long productId = findProductIdByName(session, productName);
        if (productId <= 0) {
            String productBody = "[{\"product_name\":\"" + jsonEscape(productName) + "\","
                    + "\"category_id\":" + categoryId + ","
                    + "\"unit\":\"" + jsonEscape(unit) + "\","
                    + "\"reorder_level\":10,"
                    + "\"is_active\":true}]";
            sendJsonRequest("POST", "/rest/v1/product", productBody, session.getAccessToken(), false);
            productId = findProductIdByName(session, productName);
        } else {
            String patchProductBody = "{"
                    + "\"product_name\":\"" + jsonEscape(productName) + "\","
                    + "\"category_id\":" + categoryId + ","
                    + "\"unit\":\"" + jsonEscape(unit) + "\","
                    + "\"is_active\":true"
                    + "}";
            sendJsonRequest(
                    "PATCH",
                    "/rest/v1/product?product_id=eq." + productId,
                    patchProductBody,
                    session.getAccessToken(),
                    false
            );
        }

        if (productId <= 0) {
            throw new IOException("Unable to create/resolve product: " + productName);
        }

        sendJsonRequest(
                "DELETE",
                "/rest/v1/ingredients?product_id=eq." + productId + "&item_id=eq." + itemId,
                null,
                session.getAccessToken(),
                false
        );
        String ingredientBody = "[{\"product_id\":" + productId + ","
                + "\"item_id\":" + itemId + ","
                + "\"unit_type\":\"" + jsonEscape(unit) + "\","
                + "\"quantity_needed\":1}]";
        sendJsonRequest("POST", "/rest/v1/ingredients", ingredientBody, session.getAccessToken(), false);
        return productId;
    }

    private long ensureStandaloneProduct(
            SupabaseSession session,
            String productName,
            String unit,
            long categoryId
    ) throws IOException, InterruptedException {
        long productId = findProductIdByName(session, productName);
        if (productId <= 0) {
            String productBody = "[{\"product_name\":\"" + jsonEscape(productName) + "\","
                    + "\"category_id\":" + categoryId + ","
                    + "\"unit\":\"" + jsonEscape(unit) + "\","
                    + "\"reorder_level\":10,"
                    + "\"is_active\":true}]";
            sendJsonRequest("POST", "/rest/v1/product", productBody, session.getAccessToken(), false);
            productId = findProductIdByName(session, productName);
        } else {
            String patchProductBody = "{"
                    + "\"product_name\":\"" + jsonEscape(productName) + "\","
                    + "\"category_id\":" + categoryId + ","
                    + "\"unit\":\"" + jsonEscape(unit) + "\","
                    + "\"is_active\":true"
                    + "}";
            sendJsonRequest("PATCH", "/rest/v1/product?product_id=eq." + productId, patchProductBody, session.getAccessToken(), false);
        }

        if (productId <= 0) {
            throw new IOException("Unable to create/resolve product: " + productName);
        }
        return productId;
    }

    private void deactivateProductByName(
            SupabaseSession session,
            String productName,
            long itemId
    ) throws IOException, InterruptedException {
        long productId = findProductIdByName(session, productName);
        if (productId <= 0) {
            return;
        }

        String ingredientPath = itemId > 0
                ? "/rest/v1/ingredients?product_id=eq." + productId + "&item_id=eq." + itemId
                : "/rest/v1/ingredients?product_id=eq." + productId;
        sendJsonRequest("DELETE", ingredientPath, null, session.getAccessToken(), false);
        sendJsonRequest(
                "PATCH",
                "/rest/v1/product?product_id=eq." + productId,
                "{\"is_active\":false}",
                session.getAccessToken(),
                false
        );
    }

    private long insertSale(SupabaseSession session, long userId) throws IOException, InterruptedException {
        String body = "[{\"user_id\":" + userId + "}]";
        HttpResponse<String> response = sendJsonRequest("POST", "/rest/v1/sales", body, session.getAccessToken(), false);
        long saleId = extractJsonLong(response.body(), "sale_id");
        if (saleId <= 0) {
            throw new IOException("Unable to resolve created sale id.");
        }
        return saleId;
    }

    private long insertStockOut(SupabaseSession session, long userId, String reason) throws IOException, InterruptedException {
        String body = "[{\"user_id\":" + userId + ",\"reason\":\"" + jsonEscape(reason == null ? "" : reason) + "\"}]";
        HttpResponse<String> response = sendJsonRequest("POST", "/rest/v1/stock_out", body, session.getAccessToken(), false);
        long stockoutId = extractJsonLong(response.body(), "stockout_id");
        if (stockoutId <= 0) {
            throw new IOException("Unable to resolve created stock_out id.");
        }
        return stockoutId;
    }

    private void insertSalesDetail(
            SupabaseSession session,
            long saleId,
            long productId,
            int quantity,
            double price
    ) throws IOException, InterruptedException {
        String body = "[{\"sale_id\":" + saleId + ","
                + "\"product_id\":" + productId + ","
                + "\"quantity\":" + quantity + ","
                + "\"price\":" + price + "}]";
        sendJsonRequest("POST", "/rest/v1/sales_details", body, session.getAccessToken(), false);
    }

    private long findUserIdByUsername(SupabaseSession session, String username) throws IOException, InterruptedException {
        if (username == null || username.isBlank()) {
            return -1;
        }
        String path = "/rest/v1/user?select=user_id&username=eq." + urlEncode(username) + "&limit=1";
        HttpResponse<String> response = sendJsonRequest("GET", path, null, session.getAccessToken(), false);
        return extractJsonLong(response.body(), "user_id");
    }

    private long findCategoryIdByName(SupabaseSession session, String categoryName) throws IOException, InterruptedException {
        String path = "/rest/v1/category?select=category_id&category_name=eq." + urlEncode(categoryName) + "&limit=1";
        HttpResponse<String> response = sendJsonRequest("GET", path, null, session.getAccessToken(), false);
        return extractJsonLong(response.body(), "category_id");
    }

    private long findItemIdByName(SupabaseSession session, String itemName) throws IOException, InterruptedException {
        String path = "/rest/v1/item?select=item_id&item_name=eq." + urlEncode(itemName) + "&limit=1";
        HttpResponse<String> response = sendJsonRequest("GET", path, null, session.getAccessToken(), false);
        return extractJsonLong(response.body(), "item_id");
    }

    private long findProductIdByName(SupabaseSession session, String productName) throws IOException, InterruptedException {
        String path = "/rest/v1/product?select=product_id&product_name=eq." + urlEncode(productName) + "&limit=1";
        HttpResponse<String> response = sendJsonRequest("GET", path, null, session.getAccessToken(), false);
        return extractJsonLong(response.body(), "product_id");
    }

    private int fetchItemQuantityOnHand(SupabaseSession session, long itemId) throws IOException, InterruptedException {
        String path = "/rest/v1/item?select=quantity_on_hand&item_id=eq." + itemId + "&limit=1";
        HttpResponse<String> response = sendJsonRequest("GET", path, null, session.getAccessToken(), false);
        int quantity = extractJsonInt(response.body(), "quantity_on_hand");
        return Math.max(quantity, 0);
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

    private static String extractNestedJsonString(String jsonBody, String objectName, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(objectName) + "\"\\s*:\\s*\\{(.*?)\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(jsonBody);
        if (!matcher.find()) {
            return null;
        }
        return extractJsonString(matcher.group(1), fieldName);
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
            String category = extractJsonString(objectJson, "unit_type");
            if (category == null || category.isBlank()) {
                category = "Other";
            }
            int quantity = extractJsonInt(objectJson, "quantity_on_hand");
            double price = extractJsonDouble(objectJson, "unit_cost");
            String dateAdded = "";
            String dateUpdated = "";

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

    private static List<SupplierRecord> parseSupplierRecords(String json) {
        List<SupplierRecord> records = new ArrayList<>();
        for (String objectJson : splitTopLevelObjects(json)) {
            records.add(new SupplierRecord(
                    extractJsonLong(objectJson, "supplier_id"),
                    defaultString(extractJsonString(objectJson, "supplier_name")),
                    defaultString(extractJsonString(objectJson, "contact_person")),
                    defaultString(extractJsonString(objectJson, "phone")),
                    defaultString(extractJsonString(objectJson, "email")),
                    defaultString(extractJsonString(objectJson, "address")),
                    defaultString(extractJsonString(objectJson, "status"))
            ));
        }
        return records;
    }

    private static List<ExpirationRecord> parseExpirationRecords(String json) {
        List<ExpirationRecord> records = new ArrayList<>();
        for (String objectJson : splitTopLevelObjects(json)) {
            String itemName = extractNestedItemName(objectJson);
            records.add(new ExpirationRecord(
                    extractJsonLong(objectJson, "expiration_id"),
                    itemName == null ? "" : itemName,
                    defaultString(extractJsonString(objectJson, "unit_type")),
                    extractJsonInt(objectJson, "unit_quantity"),
                    defaultString(extractJsonString(objectJson, "expiration_date"))
            ));
        }
        return records;
    }

    private static List<IngredientRecord> parseIngredientRecords(String json) {
        List<IngredientRecord> records = new ArrayList<>();
        for (String objectJson : splitTopLevelObjects(json)) {
            records.add(new IngredientRecord(
                    defaultString(extractNestedProductName(objectJson)),
                    defaultString(extractNestedItemName(objectJson)),
                    defaultString(extractJsonString(objectJson, "unit_type")),
                    extractJsonInt(objectJson, "quantity_needed")
            ));
        }
        return records;
    }

    private static List<StockOutItemRecord> parseStockOutItemRecords(String json) {
        List<StockOutItemRecord> records = new ArrayList<>();
        for (String objectJson : splitTopLevelObjects(json)) {
            records.add(new StockOutItemRecord(
                    extractJsonLong(objectJson, "stockout_item_id"),
                    defaultString(extractNestedStockOutField(objectJson, "stockout_date")),
                    defaultString(extractNestedStockOutField(objectJson, "reason")),
                    defaultString(extractNestedItemName(objectJson)),
                    extractJsonInt(objectJson, "quantity"),
                    extractJsonDouble(objectJson, "cost")
            ));
        }
        return records;
    }

    private static List<SaleHistoryLineRecord> parseSaleHistoryLineRecords(String json) {
        List<SaleHistoryLineRecord> records = new ArrayList<>();
        for (String objectJson : splitTopLevelObjects(json)) {
            records.add(new SaleHistoryLineRecord(
                    extractJsonLong(objectJson, "sale_id"),
                    defaultString(extractNestedJsonString(objectJson, "sales", "sale_date")),
                    defaultString(extractNestedJsonString(objectJson, "product", "product_name")),
                    extractJsonInt(objectJson, "quantity"),
                    extractJsonDouble(objectJson, "price")
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

    private static long extractJsonLong(String jsonBody, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*(-?\\d+)");
        Matcher matcher = pattern.matcher(jsonBody);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return -1;
    }

    private static String extractNestedItemName(String jsonBody) {
        Pattern pattern = Pattern.compile("\"item\"\\s*:\\s*\\{[^}]*\"item_name\"\\s*:\\s*\"([^\"]*)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(jsonBody);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String extractNestedProductName(String jsonBody) {
        Pattern pattern = Pattern.compile("\"product\"\\s*:\\s*\\{[^}]*\"product_name\"\\s*:\\s*\"([^\"]*)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(jsonBody);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String extractNestedStockOutField(String jsonBody, String fieldName) {
        Pattern pattern = Pattern.compile("\"stock_out\"\\s*:\\s*\\{[^}]*\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]*)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(jsonBody);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String defaultString(String value) {
        return value == null ? "" : value;
    }

    private static double extractJsonDouble(String jsonBody, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(jsonBody);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0.0;
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
