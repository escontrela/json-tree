package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DroppedJsonPathResolverTest {

    private final DroppedJsonPathResolver resolver = new DroppedJsonPathResolver();

    @Test
    void resolvesFirstSupportedJsonFileFromDroppedPayload() {
        Path resolved = resolver.resolve(List.of(
                Path.of("/tmp/notes.txt").toFile(),
                Path.of("/tmp/example.JSON").toFile(),
                Path.of("/tmp/ignored.json").toFile()
        )).orElseThrow();

        assertEquals(Path.of("/tmp/example.JSON"), resolved);
    }

    @Test
    void ignoresUnsupportedOrEmptyPayloadsGracefully() {
        assertTrue(resolver.resolve(List.of(
                Path.of("/tmp/readme.md").toFile(),
                Path.of("/tmp/data.yaml").toFile()
        )).isEmpty());
        assertTrue(resolver.resolve(List.<File>of()).isEmpty());
        assertTrue(resolver.resolve(null).isEmpty());
    }
}
