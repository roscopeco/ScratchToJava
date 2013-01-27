package com.roscopeco.scratch.runtime;

import java.util.HashMap;

public abstract class BaseScriptController extends ScriptController {
  private final HashMap<String, Object> vars = new HashMap<String, Object>();
  
  public BaseScriptController() {
    super();
  }
  
  // TODO vars could eventually become straight inst vars on a generated
  // subclass of controller, or a global Binding object or something...
  public void setVar(String name, Object value) {
    vars.put(name, value);
  }
  
  public Object getVar(String name) {
    return vars.get(name);
  }
}
