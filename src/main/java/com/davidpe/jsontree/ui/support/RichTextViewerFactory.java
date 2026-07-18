package com.davidpe.jsontree.ui.support;

import org.springframework.stereotype.Component;

/**
 * Factory for the shared RichTextFX-based viewer surface.
 *
 * <p>The factory keeps the construction details in one place so controllers only depend on the
 * wrapper API and not on RichTextFX internals.
 */
@Component
public class RichTextViewerFactory {

  public RichTextViewerSurface create() {
    return new RichTextViewerSurface();
  }
}
