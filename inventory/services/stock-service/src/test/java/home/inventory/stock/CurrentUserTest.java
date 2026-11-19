package home.inventory.stock;

import static org.assertj.core.api.Assertions.assertThat;

import home.inventory.stock.common.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserTest {

    @Test
    void noAuthenticationMeansNoUser() {
        SecurityContextHolder.clearContext();
        assertThat(CurrentUser.name()).isNull();
    }
}
