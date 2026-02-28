public class SupabaseSession {
    private final String accessToken;
    private final String refreshToken;
    private final String userId;
    private final String email;

    public SupabaseSession(String accessToken, String refreshToken, String userId, String email) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.email = email;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }
}
