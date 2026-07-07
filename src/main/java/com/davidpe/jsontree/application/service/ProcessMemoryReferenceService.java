package com.davidpe.jsontree.application.service;

import org.springframework.stereotype.Service;

/**
 * Captures the process memory reference once at startup so the settings workflow can expose a
 * stable advisory comparison value.
 */
@Service
public class ProcessMemoryReferenceService {

  private final long startupMaxMemoryBytes;

  public ProcessMemoryReferenceService() {
    this(Runtime.getRuntime().maxMemory());
  }

  ProcessMemoryReferenceService(long startupMaxMemoryBytes) {
    this.startupMaxMemoryBytes = Math.max(0L, startupMaxMemoryBytes);
  }

  public long startupMaxMemoryBytes() {
    return startupMaxMemoryBytes;
  }
}
