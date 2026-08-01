package com.davidpe.jsontree.ui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.jsontree.ui.model.ViewerPresentationMode;
import org.junit.jupiter.api.Test;

class ViewerPresentationHeaderResolverTest {

  private final ViewerPresentationHeaderResolver resolver = new ViewerPresentationHeaderResolver();

  @Test
  void resolvesDistinctTitlesForKeyPresentationModes() {
    assertEquals(
        "Structured developer output",
        resolver.resolve(ViewerPresentationMode.ASCII_TREE, false).titleText());
    assertEquals(
        "Merged JSON structure",
        resolver.resolve(ViewerPresentationMode.STRUCTURE, false).titleText());
    assertEquals(
        "Rendered Markdown reading view",
        resolver.resolve(ViewerPresentationMode.MARKDOWN_RENDERED, false).titleText());
    assertEquals(
        "Exact JSON source",
        resolver.resolve(ViewerPresentationMode.RAW_JSON, false).titleText());
  }

  @Test
  void prioritizesCropTitleWhenCropIsActive() {
    ViewerPresentationHeader header = resolver.resolve(ViewerPresentationMode.ASCII_TREE, true);

    assertEquals("Crop view", header.eyebrowText());
    assertEquals("Search-derived JSON subset", header.titleText());
  }
}
