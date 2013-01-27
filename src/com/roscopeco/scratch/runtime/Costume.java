package com.roscopeco.scratch.runtime;

public interface Costume {
  /**
   * Get the (1-based) index of this costume within it's sprite. 
   */
  public int index();
  
  /**
   * Get the original (Scratch) media name of this costume.
   */
  public String name();
}
