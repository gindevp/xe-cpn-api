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
    private String ahamoveToken;

    @Size(max = 255)
    private String grabToken;

    @Size(max = 255)
    private String xanhsmToken;

    @Size(max = 255)
    private String distanceApiToken;

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

    public String getAhamoveToken() {
        return ahamoveToken;
    }

    public void setAhamoveToken(String ahamoveToken) {
        this.ahamoveToken = ahamoveToken;
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
            ", ahamoveToken='" + getAhamoveToken() + "'" +
            ", grabToken='" + getGrabToken() + "'" +
            ", xanhsmToken='" + getXanhsmToken() + "'" +
            ", distanceApiToken='" + getDistanceApiToken() + "'" +
            ", telegramToken='" + getTelegramToken() + "'" +
            ", telegramChatId='" + getTelegramChatId() + "'" +
            ", webhookUrl='" + getWebhookUrl() + "'" +
            ", webhookSecret='" + getWebhookSecret() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
