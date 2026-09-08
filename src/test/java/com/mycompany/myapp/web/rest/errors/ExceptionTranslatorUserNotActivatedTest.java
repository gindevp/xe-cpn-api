package com.mycompany.myapp.web.rest.errors;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.security.UserNotActivatedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import tech.jhipster.web.rest.errors.ProblemDetailWithCause;

/**
 * Tài khoản bị khóa (jhi_user.activated = 0) phải ra 401 kèm mã lỗi rõ ràng để web/mobile
 * hiển thị đúng, thay vì 500 "Internal Server Error" như trước.
 * Chạy được không cần Docker, khác {@link ExceptionTranslatorIT}.
 */
class ExceptionTranslatorUserNotActivatedTest {

    private static final String LOCKED_DETAIL = "Tài khoản đã bị khóa. Liên hệ quản trị viên để mở lại.";

    private final ExceptionTranslator translator = new ExceptionTranslator(new MockEnvironment());

    @Test
    void wrappedUserNotActivatedMapsToUnauthorized() {
        // Spring bọc mọi thứ loadUserByUsername ném ra vào InternalAuthenticationServiceException.
        Throwable ex = new InternalAuthenticationServiceException("wrapped", new UserNotActivatedException("User admin was not activated"));

        ProblemDetailWithCause problem = translator.wrapAndCustomizeProblem(ex, null);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problem.getProperties()).containsEntry("message", ErrorConstants.ERR_USER_NOT_ACTIVATED);
        // Không lộ login và không dùng tiếng Anh mặc định của Spring.
        assertThat(problem.getDetail()).isEqualTo(LOCKED_DETAIL);
    }

    @Test
    void directUserNotActivatedMapsToUnauthorized() {
        ProblemDetailWithCause problem = translator.wrapAndCustomizeProblem(
            new UserNotActivatedException("User admin was not activated"),
            null
        );

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problem.getProperties()).containsEntry("message", ErrorConstants.ERR_USER_NOT_ACTIVATED);
        assertThat(problem.getDetail()).isEqualTo(LOCKED_DETAIL);
    }

    @Test
    void unrelatedFailureStaysInternalServerError() {
        ProblemDetailWithCause problem = translator.wrapAndCustomizeProblem(new RuntimeException("boom"), null);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getProperties()).doesNotContainEntry("message", ErrorConstants.ERR_USER_NOT_ACTIVATED);
    }
}
