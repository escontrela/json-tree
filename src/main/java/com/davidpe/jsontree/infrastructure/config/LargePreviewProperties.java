package com.davidpe.jsontree.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "json-tree.large-preview")
public class LargePreviewProperties {

  public static final long DEFAULT_FULL_RENDER_MAX_BYTES = 1_048_576L;
  public static final int MIN_EDITABLE_BYTES = 1024;
  public static final int DEFAULT_WARM_PAGE_RADIUS = 20;
  public static final int MAX_WARM_PAGE_RADIUS = 200;
  public static final int DEFAULT_PAGE_INDEX_STRIDE_BYTES = 512 * 1024;
  public static final int DEFAULT_VISIBLE_CHUNK_BYTES = 150 * 1024;
  public static final int DEFAULT_CHUNK_OVERLAP_BYTES = 12 * 1024;

  private long fullRenderMaxBytes = DEFAULT_FULL_RENDER_MAX_BYTES;
  private int previewMaxLines = 400;
  private int pageLineCount = 400;
  private int warmPageRadius = DEFAULT_WARM_PAGE_RADIUS;
  private int previewMaxDepth = 8;
  private int previewMaxChildrenPerContainer = 64;
  private int pageIndexStrideBytes = DEFAULT_PAGE_INDEX_STRIDE_BYTES;
  private int visibleChunkBytes = DEFAULT_VISIBLE_CHUNK_BYTES;
  private int chunkOverlapBytes = DEFAULT_CHUNK_OVERLAP_BYTES;

  public long getFullRenderMaxBytes() {
    return fullRenderMaxBytes;
  }

  public void setFullRenderMaxBytes(long fullRenderMaxBytes) {
    this.fullRenderMaxBytes = Math.max(MIN_EDITABLE_BYTES, fullRenderMaxBytes);
  }

  public int getPreviewMaxLines() {
    return previewMaxLines;
  }

  public void setPreviewMaxLines(int previewMaxLines) {
    this.previewMaxLines = previewMaxLines;
  }

  public int getPageLineCount() {
    return pageLineCount;
  }

  public void setPageLineCount(int pageLineCount) {
    this.pageLineCount = pageLineCount;
  }

  public int getWarmPageRadius() {
    return warmPageRadius;
  }

  public void setWarmPageRadius(int warmPageRadius) {
    this.warmPageRadius = Math.max(0, Math.min(warmPageRadius, MAX_WARM_PAGE_RADIUS));
  }

  public int getPreviewMaxDepth() {
    return previewMaxDepth;
  }

  public void setPreviewMaxDepth(int previewMaxDepth) {
    this.previewMaxDepth = previewMaxDepth;
  }

  public int getPreviewMaxChildrenPerContainer() {
    return previewMaxChildrenPerContainer;
  }

  public void setPreviewMaxChildrenPerContainer(int previewMaxChildrenPerContainer) {
    this.previewMaxChildrenPerContainer = previewMaxChildrenPerContainer;
  }

  public int getPageIndexStrideBytes() {
    return pageIndexStrideBytes;
  }

  public void setPageIndexStrideBytes(int pageIndexStrideBytes) {
    this.pageIndexStrideBytes = Math.max(MIN_EDITABLE_BYTES, pageIndexStrideBytes);
  }

  public int getVisibleChunkBytes() {
    return visibleChunkBytes;
  }

  public void setVisibleChunkBytes(int visibleChunkBytes) {
    this.visibleChunkBytes = Math.max(MIN_EDITABLE_BYTES, visibleChunkBytes);
  }

  public int getChunkOverlapBytes() {
    return Math.max(0, Math.min(chunkOverlapBytes, Math.max(0, visibleChunkBytes - MIN_EDITABLE_BYTES)));
  }

  public void setChunkOverlapBytes(int chunkOverlapBytes) {
    this.chunkOverlapBytes = Math.max(0, chunkOverlapBytes);
  }
}
