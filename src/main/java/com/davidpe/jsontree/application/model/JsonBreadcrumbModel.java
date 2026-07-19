package com.davidpe.jsontree.application.model;

import java.util.List;

public record JsonBreadcrumbModel(boolean available, List<JsonBreadcrumbAnchor> anchors) {

  public JsonBreadcrumbModel {
    anchors = List.copyOf(anchors);
  }

  public static JsonBreadcrumbModel unavailable() {
    return new JsonBreadcrumbModel(false, List.of());
  }
}
