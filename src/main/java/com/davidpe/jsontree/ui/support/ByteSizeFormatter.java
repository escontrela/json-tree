package com.davidpe.jsontree.ui.support;

import java.util.Locale;

/**
 * Formats byte counts for UI metadata labels using binary unit boundaries.
 *
 * <p>The formatter keeps the existing compact presentation style while ensuring exact unit
 * boundaries stay in the correct bucket, for example 1 MB is never labeled as GB.
 */
public final class ByteSizeFormatter {

  private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB", "PB", "EB"};

  private ByteSizeFormatter() {}

  public static String format(long bytes) {
    if (bytes < 1024L) {
      return bytes + " B";
    }

    double scaled = bytes;
    int unitIndex = 0;
    while (scaled >= 1024.0 && unitIndex < UNITS.length - 1) {
      scaled /= 1024.0;
      unitIndex++;
    }

    return String.format(Locale.ROOT, "%.1f %s", scaled, UNITS[unitIndex]);
  }
}
