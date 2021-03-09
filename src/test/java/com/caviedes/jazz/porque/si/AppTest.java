package com.caviedes.jazz.porque.si;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppTest {
    private App app;

    @BeforeEach
    private void setup() {
        app = new App();
    }

    @AfterEach
    private void cleanup() {
        // delete temp folders and files
        // TODO
    }

    @Test
    void executeATest() {
        app.execute();

        Assertions.fail("to be implemented");
    }

}
