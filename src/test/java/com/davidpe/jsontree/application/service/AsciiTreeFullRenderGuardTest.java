package com.davidpe.jsontree.application.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.jsontree.domain.model.AsciiTreeDocument;
import org.junit.jupiter.api.Test;

class AsciiTreeFullRenderGuardTest {

  @Test
  void staysWithinBudgetForCompactDocuments() {
    AsciiTreeFullRenderGuard guard = new AsciiTreeFullRenderGuard(16);
    AsciiTreeDocument document =
        new AsciiTreeDocument("root", "root\n├─ id: 42\n└─ active: true", 3);

    assertFalse(guard.exceedsBudget(document));
  }

  @Test
  void promotesLargeDocumentsBeforeFullSyntaxRenderFallback() {
    AsciiTreeFullRenderGuard guard = new AsciiTreeFullRenderGuard(12);
    AsciiTreeDocument document =
        new AsciiTreeDocument(
            "root",
            """
            root
            ├─ game
            │  ├─ id: 1
            │  ├─ white: "alpha"
            │  ├─ black: "beta"
            │  └─ result: "1-0"
            └─ tags [3]""",
            7);

    assertTrue(guard.exceedsBudget(document));
  }
}
