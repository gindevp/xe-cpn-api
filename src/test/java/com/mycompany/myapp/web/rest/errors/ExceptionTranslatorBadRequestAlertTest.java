package com.mycompany.myapp.web.rest.errors;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import tech.jhipster.web.rest.errors.ProblemDetailWithCause;

/** Title của BadRequestAlertException là thông báo cho người dùng — không được bị ghi đè thành "Bad Request". */
class ExceptionTranslatorBadRequestAlertTest {

    private final ExceptionTranslator translator = new ExceptionTranslator(new MockEnvironment());

    @Test
    void keepsBusinessMessageAsTitle() {
        String msg = "Kết nối Wi-fi ở văn phòng để chấm công (IP hiện tại: 1.2.3.4)";

        ProblemDetailWithCause problem = translator.wrapAndCustomizeProblem(
            new BadRequestAlertException(msg, "attendance", "wifiMismatch"),
            null
        );

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo(msg);
        assertThat(problem.getProperties()).containsEntry("message", "error.wifiMismatch");
    }
}
