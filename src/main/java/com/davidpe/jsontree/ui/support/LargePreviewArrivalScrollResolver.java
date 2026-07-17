package com.davidpe.jsontree.ui.support;

import com.davidpe.jsontree.application.model.LargePreviewPageDescriptor;
import org.springframework.stereotype.Component;

/**
 * Chooses the vertical scroll landing point after a large-preview chunk change so overlapping byte
 * windows feel continuous when the user moves forward or backward.
 */
@Component
public class LargePreviewArrivalScrollResolver {

  public double resolve(LargePreviewPageDescriptor descriptor, int navigationDirection) {
    if (descriptor == null || descriptor.logicalLineCount() <= 0 || navigationDirection == 0) {
      return navigationDirection < 0 ? 1.0 : 0.0;
    }

    double overlapRatio =
        Math.max(descriptor.leadingOverlapBytes(), descriptor.trailingOverlapBytes())
            / (double) descriptor.logicalLineCount();
    double clampedRatio = Math.max(0.04, Math.min(0.35, overlapRatio));
    return navigationDirection < 0 ? 1.0 - clampedRatio : clampedRatio;
  }
}
