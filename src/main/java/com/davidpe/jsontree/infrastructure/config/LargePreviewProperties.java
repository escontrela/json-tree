package com.davidpe.jsontree.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "json-tree.large-preview")
public class LargePreviewProperties {

  private long fullRenderMaxBytes = 1_048_576L;
  private int previewMaxLines = 400;
  private int pageLineCount = 400;
  private int previewMaxDepth = 8;
  private int previewMaxChildrenPerContainer = 64;
  private int textNodeBudget = 12000;

  public long getFullRenderMaxBytes() {
    return fullRenderMaxBytes;
  }

  public void setFullRenderMaxBytes(long fullRenderMaxBytes) {
    this.fullRenderMaxBytes = fullRenderMaxBytes;
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

  public int getTextNodeBudget() {
    return textNodeBudget;
  }

  public void setTextNodeBudget(int textNodeBudget) {
    this.textNodeBudget = textNodeBudget;
  }
}
