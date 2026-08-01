package com.davidpe.jsontree.ui.controls.search.model;

/**
 * Optional crop/full-view affordance state hosted inside the reusable search panel.
 */
public record SearchPanelCropState(
    boolean visible,
    boolean enabled,
    boolean selected,
    String tooltipText,
    String accessibleText) {

  public static SearchPanelCropState hidden() {
    return new SearchPanelCropState(false, false, false, "", "");
  }
}
