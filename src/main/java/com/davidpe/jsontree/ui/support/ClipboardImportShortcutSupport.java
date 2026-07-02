package com.davidpe.jsontree.ui.support;

import javafx.scene.input.KeyCode;
import org.springframework.stereotype.Component;

@Component
public class ClipboardImportShortcutSupport {

  public boolean shouldTrigger(
      KeyCode keyCode,
      boolean shortcutDown,
      boolean altDown,
      boolean shiftDown,
      boolean textInputTarget,
      boolean modalVisible
  ) {
    if (modalVisible || textInputTarget) {
      return false;
    }
    return keyCode == KeyCode.P && shortcutDown && !altDown && !shiftDown;
  }
}
