package com.mycompany.myapp.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.IntegrationConfig} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class IntegrationConfigDTO implements Serializable {

    private Long id;

    @Size(max = 255)
    private String ahamoveApiKey;

    @Size(max = 32)
    private String ahamoveMobile;

    @Size(max = 2000)
    private String ahamoveToken;

    private Instant ahamoveTokenFetchedAt;

    @Size(max = 255)
    private String grabToken;

    @Size(max = 255)
    private String xanhsmToken;

    @Size(max = 255)
    private String distanceApiToken;

    @Size(max = 16)
    private String mapProvider;

    @Size(max = 255)
    private String goongMapTilesKey;

    @Size(max = 255)
    private String telegramToken;

    @Size(max = 100)
    private String telegramChatId;

    @Size(max = 255)
    private String webhookUrl;

    @Size(max = 255)
    private String webhookSecret;

    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAhamoveApiKey() {
        return ahamoveApiKey;
    }

    public void setAhamoveApiKey(String ahamoveApiKey) {
        this.ahamoveApiKey = ahamoveApiKey;
    }

    public String getAhamoveMobile() {
        return ahamoveMobile;
    }

    public void setAhamoveMobile(String ahamoveMobile) {
        this.ahamoveMobile = ahamoveMobile;
    }

    public String getAhamoveToken() {
        return ahamoveToken;
    }

    public void setAhamoveToken(String ahamoveToken) {
        this.ahamoveToken = ahamoveToken;
    }

    public Instant getAhamoveTokenFetchedAt() {
        return ahamoveTokenFetchedAt;
    }

    public void setAhamoveTokenFetchedAt(Instant ahamoveTokenFetchedAt) {
        this.ahamoveTokenFetchedAt = ahamoveTokenFetchedAt;
    }

    public String getGrabToken() {
        return grabToken;
    }

    public void setGrabToken(String grabToken) {
        this.grabToken = grabToken;
    }

    public String getXanhsmToken() {
        return xanhsmToken;
    }

    public void setXanhsmToken(String xanhsmToken) {
        this.xanhsmToken = xanhsmToken;
    }

    public String getDistanceApiToken() {
        return distanceApiToken;
    }

    public void setDistanceApiToken(String distanceApiToken) {
        this.distanceApiToken = distanceApiToken;
    }

    public String getMapProvider() {
        return mapProvider;
    }

    public void setMapProvider(String mapProvider) {
        this.mapProvider = mapProvider;
    }

    public String getGoongMapTilesKey() {
        return goongMapTilesKey;
    }

    public void setGoongMapTilesKey(String goongMapTilesKey) {
        this.goongMapTilesKey = goongMapTilesKey;
    }

    public String getTelegramToken() {
        return telegramToken;
    }

    public void setTelegramToken(String telegramToken) {
        this.telegramToken = telegramToken;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IntegrationConfigDTO)) {
            return false;
        }

        IntegrationConfigDTO integrationConfigDTO = (IntegrationConfigDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, integrationConfigDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "IntegrationConfigDTO{" +
            "id=" + getId() +
            ", ahamoveApiKey='" + getAhamoveApiKey() + "'" +
            ", ahamoveMobile='" + getAhamoveMobile() + "'" +
            ", ahamoveToken='" + getAhamoveToken() + "'" +
            ", ahamoveTokenFetchedAt='" + getAhamoveTokenFetchedAt() + "'" +
            ", grabToken='" + getGrabToken() + "'" +
            ", xanhsmToken='" + getXanhsmToken() + "'" +
            ", distanceApiToken='" + getDistanceApiToken() + "'" +
            ", mapProvider='" + getMapProvider() + "'" +
            ", goongMapTilesKey='" + getGoongMapTilesKey() + "'" +
            ", telegramToken='" + getTelegramToken() + "'" +
            ", telegramChatId='" + getTelegramChatId() + "'" +
            ", webhookUrl='" + getWebhookUrl() + "'" +
            ", webhookSecret='" + getWebhookSecret() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
