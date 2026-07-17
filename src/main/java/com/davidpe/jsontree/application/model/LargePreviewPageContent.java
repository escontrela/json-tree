package com.davidpe.jsontree.application.model;

public record LargePreviewPageContent(LargePreviewPageDescriptor descriptor, String content) {

  public LargePreviewPageContent {
    if (descriptor == null) {
      throw new IllegalArgumentException("Large-preview page descriptor is required.");
    }
    if (content == null) {
      throw new IllegalArgumentException("Large-preview page content is required.");
    }
  }
}
