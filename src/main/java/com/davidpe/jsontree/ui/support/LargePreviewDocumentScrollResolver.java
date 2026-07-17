package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.springframework.stereotype.Component;

/**
 * Maps large-preview viewer scroll values onto document-wide page anchors derived from the
 * fully materialized logical line ranges stored in the active session.
 */
@Component
public class LargePreviewDocumentScrollResolver {

  public OptionalInt targetPage(JsonViewerLoadResult result, double verticalScrollValue) {
    if (result == null || !result.usesLargePreview() || !result.hasLargePreviewSession()) {
      return OptionalInt.empty();
    }
    return result.largePreviewSession().resolvePageIndexForScrollValue(verticalScrollValue);
  }

  public OptionalDouble pageStartScrollValue(JsonViewerLoadResult result, int pageIndex) {
    if (result == null || !result.usesLargePreview() || !result.hasLargePreviewSession()) {
      return OptionalDouble.empty();
    }
    return result.largePreviewSession().pageRange(pageIndex).isPresent()
        ? OptionalDouble.of(result.largePreviewSession().scrollValueForPageStart(pageIndex))
        : OptionalDouble.empty();
  }
}
