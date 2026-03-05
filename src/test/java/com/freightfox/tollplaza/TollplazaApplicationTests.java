package com.freightfox.tollplaza;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "mappls.client-id=test-client-id",
        "mappls.client-secret=test-client-secret"
})
class TollplazaApplicationTests {

    @Test
    void contextLoads() {
    }
}
