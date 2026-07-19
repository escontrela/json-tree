package com.davidpe.jsontree.ui.service;

/**
 * Opens and focuses the reusable curl editor modal for both new and prefilled sessions.
 */
public interface CurlEditorModalCoordinator {

  void openNew(Runnable onSuccess);

  void openPrefilled(String curlCommand, Runnable onSuccess);
}
