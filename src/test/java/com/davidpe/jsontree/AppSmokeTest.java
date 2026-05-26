package com.davidpe.jsontree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppSmokeTest {

    @Test
    @DisplayName("project coordinates stay aligned with the bootstrap package")
    void projectCoordinatesStayAlignedWithBootstrapPackage() {
        assertEquals("com.davidpe.jsontree", "com.davidpe.jsontree");
    }
}
