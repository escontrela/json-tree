package com.davidpe.jsontree.application.port.out;

import java.util.Optional;

public interface ClipboardPort {

    void copy(String text);

    Optional<String> readText();
}
