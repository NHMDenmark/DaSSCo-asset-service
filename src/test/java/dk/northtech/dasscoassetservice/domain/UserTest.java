package dk.northtech.dasscoassetservice.domain;

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertWithMessage;

class UserTest {
    @Test
    void toStringShouldNotPrintCredentials() {
        User user = new User("bob", "", 123);
        user.token = "Strengt fortroligt";
        String result = user.toString();
        assertWithMessage("Do not print credentials")
                .that(result)
                .doesNotContain("Strengt fortroligt");
    }

}