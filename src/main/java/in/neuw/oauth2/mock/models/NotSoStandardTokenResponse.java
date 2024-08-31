package in.neuw.oauth2.mock.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NotSoStandardTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    // or try removing this
    @JsonProperty("expires")
    private Integer expiresIn;

    public String getAccessToken() {
        return accessToken;
    }

    public NotSoStandardTokenResponse setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public String getTokenType() {
        return tokenType;
    }

    public NotSoStandardTokenResponse setTokenType(String tokenType) {
        this.tokenType = tokenType;
        return this;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public NotSoStandardTokenResponse setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
        return this;
    }
}
