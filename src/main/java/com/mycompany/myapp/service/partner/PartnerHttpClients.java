package com.mycompany.myapp.service.partner;

import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Shared HttpClient builder for partner HTTPS calls (Ahamove, …).
 * {@code insecureSsl} dùng khi JVM local/proxy không trust được CA (giống VTHK).
 */
final class PartnerHttpClients {

    private PartnerHttpClients() {}

    static HttpClient build(Duration connectTimeout, boolean insecureSsl) {
        try {
            HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(connectTimeout);
            if (insecureSsl) {
                TrustManager[] trustAll = new TrustManager[] {
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    },
                };
                SSLContext ssl = SSLContext.getInstance("TLS");
                ssl.init(null, trustAll, new SecureRandom());
                builder.sslContext(ssl);
            }
            return builder.build();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot build partner HttpClient", e);
        }
    }
}
