package com.roscopeco.scratch.runtime;

public abstract class AbstractScript implements Runnable {
  private final ScriptController controller;
  
  protected AbstractScript(ScriptController controller) {
    this.controller = controller;
  }
  
  protected ScriptController controller() {
    return controller;
  }
}
