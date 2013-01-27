package com.roscopeco.scratch.runtime;


public abstract class ScriptController {  
  private static ScriptController instance;  

  public static void registerInstance(ScriptController _instance) {
    instance = _instance;
  }
  
  public static ScriptController getInstance() {
    if (instance == null) {
      throw new IllegalStateException("No MediaManager instance is registered");
    }
    
    return instance;    
  }
    
  protected ScriptController() { }  
  
  public abstract void registerStartScript(AbstractScript script);
  
  public abstract void registerKeyEventReceiver(String keyName, AbstractScript script);
  
  public abstract void registerBroadcastReceiver(String broadcast, AbstractScript script);
  
  public abstract void registerMouseClickEventReceiver(AbstractScript script);
  
  public abstract void broadcast(String name);
  
  public abstract void stopAllSounds();
  
  public abstract void setVar(String name, Object value);  

  public abstract Object getVar(String name);
  
  public abstract boolean checkKeyPress(String keyname);
  
  public abstract void start();
  
  public abstract void stopAll();
  
  public abstract boolean allStopped();
}
