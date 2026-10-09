package com.mycompany.myapp.service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DatabaseJdbcTest {

    @Test
    void parsesHostPortAndDatabase() {
        DatabaseJdbc.Endpoint ep = DatabaseJdbc.parse(
            "jdbc:mysql://113.20.107.44:3308/cpn?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true"
        );
        assertThat(ep.host()).isEqualTo("113.20.107.44");
        assertThat(ep.port()).isEqualTo(3308);
        assertThat(ep.database()).isEqualTo("cpn");
        assertThat(ep.key()).isEqualTo("113.20.107.44:3308/cpn");
    }

    @Test
    void rejectsPasswordInsideUrl() {
        assertThatThrownBy(() -> DatabaseJdbc.parse("jdbc:mysql://127.0.0.1:3306/cpn?password=secret"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("mật khẩu");
    }

    @Test
    void sameServerDifferentPortIsNotTheSameDatabase() {
        DatabaseJdbc.Endpoint a = DatabaseJdbc.parse("jdbc:mysql://db.internal:3306/cpn");
        DatabaseJdbc.Endpoint b = DatabaseJdbc.parse("jdbc:mysql://db.internal:3308/cpn");
        assertThat(a.key()).isNotEqualTo(b.key());
    }
}
