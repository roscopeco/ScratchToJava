package com.roscopeco.scratch.runtime;

import java.util.ArrayList;
import java.util.HashMap;

public class ScriptController {  
  private static ScriptController instance;  
  
  /* TODO this will become a factory that supplies different 
   * instances depending on the runtime being used (Java, Android, etc).
   */
  public static ScriptController getInstance() {
    if (instance == null) {
      instance = new ScriptController();
    }
    
    return instance;    
  }
  
  private final HashMap<String, Object> vars = new HashMap<String, Object>();  
  private final ArrayList<AbstractScript> startScripts;
  
  private ScriptController() {
    startScripts = new ArrayList<AbstractScript>();    
  }  
  
  public void registerStartScript(AbstractScript script) {
    startScripts.add(script);    
  }
  
  public void registerKeyEventReceiver(String keyName, AbstractScript script) {
    // TODO implement    
  }
  
  public void registerBroadcastReceiver(String broadcast, AbstractScript script) {
    // TODO implement
  }
  
  public void broadcast(String name) {
    // TODO implement
  }
  
  public void stopAllSounds() {
    // TODO implement
  }
  
  // TODO vars could eventually become straight inst vars on a generated
  // subclass of controller, or a global Binding object or something...
  public void setVar(String name, Object value) {
    vars.put(name, value);
  }
  
  public Object getVar(String name) {
    return vars.get(name);
  }
  
  public boolean checkKeyPress(String keyname) {
    // TODO implement
    return false;
  }
  
  public void start() {
    // TODO run startups, start looper.
  }
  
  public void stopAll() {
    // TODO implement
  }
  
  public boolean allStopped() {
    // TODO implement
    return false;
  }
}
