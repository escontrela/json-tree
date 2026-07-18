package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.JsonViewerLoadResult;
import com.davidpe.jsontree.domain.model.JsonValidationStatus;
import org.springframework.stereotype.Component;

/**
 * Resolves whether the zoom entry point can open for the currently rendered viewer state.
 */
@Component
public class ZoomActionAvailabilityResolver {

  public boolean shouldEnable(JsonViewerLoadResult result) {
    return result != null
        && result.validationResult().status() == JsonValidationStatus.VALID
        && result.hasRenderableTree();
  }
}
