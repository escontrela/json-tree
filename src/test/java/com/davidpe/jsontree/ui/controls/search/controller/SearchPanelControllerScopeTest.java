package com.davidpe.jsontree.ui.controls.search.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

class SearchPanelControllerScopeTest {

  @Test
  void declaresPrototypeScopeSoEachWindowGetsItsOwnControllerInstance() {
    Scope scope = SearchPanelController.class.getAnnotation(Scope.class);

    assertNotNull(scope);
    assertEquals(ConfigurableBeanFactory.SCOPE_PROTOTYPE, scope.value());
  }
}
