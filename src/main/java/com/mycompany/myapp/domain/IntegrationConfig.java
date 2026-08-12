package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * A IntegrationConfig.
 */
@Entity
@Table(name = "integration_config")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class IntegrationConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Size(max = 255)
    @Column(name = "ahamove_token", length = 255)
    private String ahamoveToken;

    @Size(max = 255)
    @Column(name = "grab_token", length = 255)
    private String grabToken;

    @Size(max = 255)
    @Column(name = "xanhsm_token", length = 255)
    private String xanhsmToken;

    @Size(max = 255)
    @Column(name = "distance_api_token", length = 255)
    private String distanceApiToken;

    @Size(max = 255)
    @Column(name = "telegram_token", length = 255)
    private String telegramToken;

    @Size(max = 100)
    @Column(name = "telegram_chat_id", length = 100)
    private String telegramChatId;

    @Size(max = 255)
    @Column(name = "webhook_url", length = 255)
    private String webhookUrl;

    @Size(max = 255)
    @Column(name = "webhook_secret", length = 255)
    private String webhookSecret;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public IntegrationConfig id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAhamoveToken() {
        return this.ahamoveToken;
    }

    public IntegrationConfig ahamoveToken(String ahamoveToken) {
        this.setAhamoveToken(ahamoveToken);
        return this;
    }

    public void setAhamoveToken(String ahamoveToken) {
        this.ahamoveToken = ahamoveToken;
    }

    public String getGrabToken() {
        return this.grabToken;
    }

    public IntegrationConfig grabToken(String grabToken) {
        this.setGrabToken(grabToken);
        return this;
    }

    public void setGrabToken(String grabToken) {
        this.grabToken = grabToken;
    }

    public String getXanhsmToken() {
        return this.xanhsmToken;
    }

    public IntegrationConfig xanhsmToken(String xanhsmToken) {
        this.setXanhsmToken(xanhsmToken);
        return this;
    }

    public void setXanhsmToken(String xanhsmToken) {
        this.xanhsmToken = xanhsmToken;
    }

    public String getDistanceApiToken() {
        return this.distanceApiToken;
    }

    public IntegrationConfig distanceApiToken(String distanceApiToken) {
        this.setDistanceApiToken(distanceApiToken);
        return this;
    }

    public void setDistanceApiToken(String distanceApiToken) {
        this.distanceApiToken = distanceApiToken;
    }

    public String getTelegramToken() {
        return this.telegramToken;
    }

    public IntegrationConfig telegramToken(String telegramToken) {
        this.setTelegramToken(telegramToken);
        return this;
    }

    public void setTelegramToken(String telegramToken) {
        this.telegramToken = telegramToken;
    }

    public String getTelegramChatId() {
        return this.telegramChatId;
    }

    public IntegrationConfig telegramChatId(String telegramChatId) {
        this.setTelegramChatId(telegramChatId);
        return this;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public String getWebhookUrl() {
        return this.webhookUrl;
    }

    public IntegrationConfig webhookUrl(String webhookUrl) {
        this.setWebhookUrl(webhookUrl);
        return this;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getWebhookSecret() {
        return this.webhookSecret;
    }

    public IntegrationConfig webhookSecret(String webhookSecret) {
        this.setWebhookSecret(webhookSecret);
        return this;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public IntegrationConfig updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IntegrationConfig)) {
            return false;
        }
        return getId() != null && getId().equals(((IntegrationConfig) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "IntegrationConfig{" +
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
