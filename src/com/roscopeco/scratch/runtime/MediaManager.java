package com.roscopeco.scratch.runtime;

public abstract class MediaManager {
  private static MediaManager instance;
  
  public static void registerInstance(MediaManager _instance) {
    instance = _instance;    
  }
  
  public static MediaManager instance() {
    if (instance == null) {
      throw new IllegalStateException("No MediaManager instance is registered");
    }
    return instance;        
  }
  
  public abstract Costume loadImage(int index, String name, String id);
  
  public abstract Sound loadSound(String id);
}
