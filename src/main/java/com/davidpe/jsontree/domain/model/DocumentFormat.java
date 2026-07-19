package com.davidpe.jsontree.domain.model;

/**
 * Supported source-document formats handled by the inspector workflow.
 */
public enum DocumentFormat {
  JSON,
  MARKDOWN;

  public boolean json() {
    return this == JSON;
  }

  public boolean markdown() {
    return this == MARKDOWN;
  }

  public String displayLabel() {
    return switch (this) {
      case JSON -> "JSON";
      case MARKDOWN -> "Markdown";
    };
  }
}
