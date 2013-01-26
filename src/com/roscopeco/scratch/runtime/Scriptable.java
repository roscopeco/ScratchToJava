package com.roscopeco.scratch.runtime;

public abstract class Scriptable {
  private final ScriptController controller;
  
  protected Scriptable() {
    controller = ScriptController.getInstance();
  }
  
  protected ScriptController controller() {
    return controller;
  }
  
  public abstract void showVariable(String name);
  
  public abstract void hideVariable(String name);
  
  public abstract void registerScripts();
}
