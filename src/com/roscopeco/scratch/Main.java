package com.roscopeco.scratch;

import com.roscopeco.scratch.codegen.CodeGenerator;
import com.roscopeco.scratch.io.ScratchProject;
import com.roscopeco.scratch.io.objects.Array;
import com.roscopeco.scratch.io.objects.OrderedCollection;
import com.roscopeco.scratch.io.objects.Point;
import com.roscopeco.scratch.io.objects.ScratchObject;
import com.roscopeco.scratch.io.objects.ScratchSpriteMorph;
import com.roscopeco.scratch.io.objects.ScratchStageMorph;

public class Main {
  
  static String mkIndent(int indent) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < indent; i++) {
      sb.append(" ");
    }    
    return sb.append("%s\n").toString();
  }
  
  @SuppressWarnings("unchecked")
  static void dumpBlock(int indent, Array<ScratchObject> code) {    
    String sindent = mkIndent(indent);
    
    for (ScratchObject o : code) {    
      if (o instanceof Array) {
        System.out.println(o);
        dumpBlock(indent + 4, (Array<ScratchObject>)o);
      } else {
        System.out.printf(sindent, o);
      }
    }    
    System.out.printf(sindent, "------");
  }
  
  static void dumpBlock(Array<ScratchObject> code) {
    dumpBlock(0, code);
  }
  
  @SuppressWarnings({ "unchecked" })
  static void dumpCode(ScratchStageMorph stage) {
    OrderedCollection<ScratchSpriteMorph> sprites = stage.sprites();
    
    System.out.println("Stage scripts");
    System.out.println("=============");
    
    System.out.println(stage.blocksBin() + " (" + stage.blocksBin().size() + ")");
    
    for (int i = 0; i < stage.blocksBin().size(); i++) {
      System.out.println("Script " + i);
      System.out.println("---------");
      dumpBlock(stage.blocksBin().get(i));
    }
    
    System.out.println();
    System.out.println("Sprites");
    System.out.println("=======");
    
    for (ScratchSpriteMorph sprite : sprites) {      
      System.out.println(sprite.objName());
      
      for (int i = 0; i < sprite.blocksBin().size(); i++) {
        System.out.println("Script " + i);
        System.out.println("---------");
        Array<ScratchObject> ary = sprite.blocksBin().get(i);
        Point p = (Point)ary.get(0);
        System.out.println("@("+p.x().intValue() + ", " + p.y().intValue()+")");
        dumpBlock((Array<ScratchObject>)ary.get(1));
      }
    }
  }
  
  public static void main(String[] args) throws Exception {
    if (args.length < 2 || args.length > 4) {
      System.out.println("Usage: scratch2java <scratch project file> <output base package> [codeDir=./gen] [resDir=./res]");
      System.exit(-1);
    }
    String outputPackage = args[1];
    String outputDir = (args.length > 2) ? args[2] : "gen";
    String resDir = (args.length > 3) ? args[3] : "res";
    
    ScratchProject project = ScratchProject.readProject(args[0]);
    
    //dumpCode(project.getStage());
    
    CodeGenerator gen = new CodeGenerator(outputDir, outputPackage, resDir, project.getStage());
    gen.generate();
    
    for (ScratchSpriteMorph sprite : project.getStage().sprites()) {
      gen = new CodeGenerator(outputDir, outputPackage, resDir, sprite);      
      gen.generate();
    }
  }
}
