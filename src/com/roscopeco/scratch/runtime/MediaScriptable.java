package com.roscopeco.scratch.runtime;

/**
 * Base class for Scriptables that are also associated with media
 * (i.e. sprites, stages).
 * 
 * @author rosco
 */
public abstract class MediaScriptable extends Scriptable {
  public void changeGraphicEffect(String effect, int value) {
    // TODO implement
  }
  
  public void playSound(String sound) {
    // TODO implement
  }
  
  public void changeVolumeBy(int delta) {
    // TODO implement
  }
}
