package com.roscopeco.scratch.codegen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.regex.Pattern;

import com.roscopeco.scratch.io.objects.Array;
import com.roscopeco.scratch.io.objects.Point;
import com.roscopeco.scratch.io.objects.ScratchObject;
import com.roscopeco.scratch.io.objects.ScratchSpriteMorph;
import com.roscopeco.scratch.io.objects.ScratchStageMorph;
import com.roscopeco.scratch.io.objects.Symbol;

public class ObjectCodeGenerator {
  static final Pattern FIRSTISDIGIT = Pattern.compile("^[0-9]");
  static final Pattern ALLVALIDCHARS = Pattern.compile("[^0-9a-zA-Z_]");
  
  static String scratchNameToIdentifier(String scratchName) {
    return scratchNameToIdentifier(scratchName, true);
  }
  
  static String scratchNameToIdentifier(String scratchName, boolean isClass) {
    String ivarname = ALLVALIDCHARS.matcher(scratchName).replaceAll("");
    if (FIRSTISDIGIT.matcher(scratchName).matches()) {
      return "_" + ivarname;
    } else {
      if (isClass) {
        return Character.toUpperCase(ivarname.charAt(0)) + ivarname.substring(1);
      } else {
        return Character.toLowerCase(ivarname.charAt(0)) + ivarname.substring(1);        
      }
    }
  }
  
  private final String codeDir;
  private final String resDir;
  private final String pkg;
  private final ScratchStageMorph stage;
  private final ScratchSpriteMorph sprite;
  private final boolean stageScript;
  private final boolean withRes;  
  private final String mainClassName;
    
  public ObjectCodeGenerator(String codeDir, String pkg, String resDir, ScratchStageMorph stage) {
    this.codeDir = codeDir;
    this.resDir = resDir;
    this.pkg = pkg;
    this.stage = stage;
    this.sprite = null;
    this.stageScript = true;
    this.withRes = true;
    this.mainClassName = scratchNameToIdentifier(stage.objName().toString());
  }
  
  public ObjectCodeGenerator(String codeDir, String pkg, String resDir, ScratchSpriteMorph sprite) {
    this.codeDir = codeDir;
    this.resDir = resDir;
    this.pkg = pkg;
    this.stage = null;
    this.sprite = sprite;
    this.stageScript = false;
    this.withRes = true;
    this.mainClassName = scratchNameToIdentifier(sprite.objName().toString());
  }
  
  public ObjectCodeGenerator(String codeDir, String pkg, ScratchStageMorph stage) {
    this.codeDir = codeDir;
    this.withRes = false;
    this.resDir = null;
    this.pkg = pkg;
    this.stage = stage;
    this.sprite = null;
    this.stageScript = true;
    this.mainClassName = scratchNameToIdentifier(stage.objName().toString());
  }
  
  public ObjectCodeGenerator(String codeDir, String pkg, ScratchSpriteMorph sprite) {
    this.codeDir = codeDir;
    this.withRes = false;
    this.resDir = null;
    this.pkg = pkg;
    this.stage = null;
    this.sprite = sprite;
    this.stageScript = false;
    this.mainClassName = scratchNameToIdentifier(sprite.objName().toString());
  }
  
  FileOutputStream openOutputFile(String clzName, String subpackage) throws IOException {
    String dirName;
    
    if (subpackage != null) {
      dirName = codeDir + File.separator + (pkg + "." + subpackage).replaceAll("\\.", File.separator);
    } else {
      dirName = codeDir + File.separator + pkg.replaceAll("\\.", File.separator);
    }
    
    File dir = new File(dirName);
    dir.mkdirs();
    
    String fn = clzName + ".java";
    File f = new File(dir, fn);
    
    return new FileOutputStream(f);
  }
  
  public void generate() throws IOException {
    Array<Array<ScratchObject>> blocksBin;
    
    if (stageScript) {
      blocksBin = stage.blocksBin();
    } else {
      blocksBin = sprite.blocksBin();
    }
    
    StringBuilder mainSb = new StringBuilder();
    
    generateClassHeader(mainSb, mainClassName);
    if (withRes) {
      generateResources(mainSb);
    }
    generateScripts(mainSb, blocksBin);
    generateClassFooter(mainSb);
    
    FileOutputStream mainClz = openOutputFile(mainClassName, "sprites");
    PrintWriter mainPw = new PrintWriter(mainClz);
    mainPw.write(mainSb.toString());
    mainPw.close();
  }
  
  void generateClassHeader(StringBuilder mainSb, String clz) {
    mainSb.append("/* Generated by ScratchToJava */\n");
    mainSb.append("package ").append(pkg).append(".sprites").append(";\n\n");
    
    mainSb.append("import com.roscopeco.scratch.runtime.AbstractStage;\n");
    if (!stageScript) {
      mainSb.append("import com.roscopeco.scratch.runtime.AbstractSprite;\n");
    }
    mainSb.append("import com.roscopeco.scratch.runtime.MediaManager;\n");
    mainSb.append("import com.roscopeco.scratch.runtime.Costume;\n");
    mainSb.append("import com.roscopeco.scratch.runtime.Sound;\n\n");
    
    mainSb.append("public final class ").append(clz).append(" extends Abstract").append(stageScript ? "Stage" : "Sprite").append(" {\n");
    
    if (!stageScript) {
      // sprite needs a constructor
      mainSb.append("  public ").append(clz).append("(AbstractStage owner) {\n");
      mainSb.append("    super(owner);\n");
      mainSb.append("    setCostume(").append(scratchNameToIdentifier(sprite.costume().mediaName().toString(), false)).append(");\n");
      mainSb.append("  }\n\n");
    }
  }
  
  void generateClassFooter(StringBuilder mainSb) {
    // Note: this works because costume.index is 1-based...
    if (!stageScript) {
      mainSb.append("  @Override\n");
      mainSb.append("  public void nextCostume() {\n");
      mainSb.append("    int current = costume().index();\n");
      mainSb.append("    if (current == costumes.length) {\n");
      mainSb.append("      current = 0;\n");
      mainSb.append("    }\n");
      mainSb.append("    setCostume(costumes[current]);\n");    
      mainSb.append("  }\n");
    }
    mainSb.append("}\n");    
  }
  
  void generateScripts(StringBuilder mainSb, Array<Array<ScratchObject>> blocksBin) throws IOException {
    generateRegisterHdr(mainSb);
    
    for (Array<ScratchObject> block : blocksBin) {
      Point p = (Point)block.get(0);
      
      @SuppressWarnings("unchecked")
      Array<Array<ScratchObject>> code = (Array<Array<ScratchObject>>)block.get(1);
      String scriptName = "Script" + mainClassName + p.x().intValue() + p.y().intValue();
      
      generateRegisterStatement(mainSb, code.get(0), scriptName);      
      generateSingleScript(scriptName,stageScript, mainClassName, code);
    }
    
    generateRegisterFooter(mainSb);
  }
  
  /*
   * Generates the header (preamble) for the registerScript method.
   */
  void generateRegisterHdr(StringBuilder mainSb) {
    // Register method
    mainSb.append("  @Override\n");
    mainSb.append("  public void registerScripts() {\n");
  }

  /*
   * Generates a register script statement in the main class being generated 
   * (either a Sprite or Stage). Expects  
   */
  void generateRegisterStatement(StringBuilder mainSb, Array<ScratchObject> hatStmt, String scriptName) {
    mainSb.append("    // ").append(hatStmt.get(0)).append(" (").append(hatStmt.size()-1).append(" argument(s))\n");
    for (int i = 1; i < hatStmt.size(); i++) {
      mainSb.append("    //   #").append(i).append(" : ").append(hatStmt.get(i)).append("\n");        
    }
    
    Symbol hatType = (Symbol)hatStmt.get(0);
    if (":EventHatMorph".equals(hatType.toString())) {
      String hatEvent = hatStmt.get(1).toString();
      if ("Scratch-StartClicked".equals(hatEvent)) {
        // is a start-clicked event (startup script)
        mainSb.append("    controller().registerStartScript(new ").append(pkg).append(".scripts.").append(scriptName).append("(this, controller()));\n\n");
      } else {
        // is a broadcast receiver event
        mainSb.append("    controller().registerBroadcastReceiver(\"").append(hatEvent).append("\", new ").append(pkg).append(".scripts.").append(scriptName).append("(this, controller()));\n\n");
      }
    } else if (":KeyEventHatMorph".equals(hatType.toString())) {
      mainSb.append("    controller().registerKeyEventReceiver(\"").append(hatStmt.get(1)).append("\", new ").append(pkg).append(".scripts.").append(scriptName).append("(this, controller()));\n\n");
    } else if (":MouseClickEventHatMorph".equals(hatType.toString())) {
      mainSb.append("    controller().registerMouseClickEventReceiver(new ").append(pkg).append(".scripts.").append(scriptName).append("(this, controller()));\n\n");
    } else {
      throw new IllegalArgumentException("Unsupported hat type: " + hatType);
    }
  }
  
  /*
   * Generates the footer for the registerScripts method.
   */
  void generateRegisterFooter(StringBuilder mainSb) {
    mainSb.append("  }\n");    
  }  
  
  /*
   * Generates an individual script in a new Script subclass.
   */
  void generateSingleScript(String clz, boolean stageScript, String ownerName, Array<Array<ScratchObject>> code) throws IOException  {
    StringBuilder scriptSb = new StringBuilder();
    
    scriptSb.append("/* Generated by ScratchToJava */\n");
    scriptSb.append("package ").append(pkg).append(".scripts").append(";\n\n");
    
    scriptSb.append("import com.roscopeco.scratch.runtime.AbstractScript;\n");
    scriptSb.append("import com.roscopeco.scratch.runtime.Abstract").append(stageScript ? "Stage" : "Sprite").append(";\n");
    scriptSb.append("import com.roscopeco.scratch.runtime.ScriptController;\n\n");
    scriptSb.append("import ").append(pkg).append(".Objects;\n");
    scriptSb.append("import ").append(pkg).append(".sprites.").append(ownerName).append(";\n\n");
    
    scriptSb.append("public final class ").append(clz).append(" extends AbstractScript {\n");
    scriptSb.append("  final ").append(ownerName).append(" target;\n\n");
    
    // Constructor
    scriptSb.append("  public ").append(clz).append("(").append(ownerName).append(" target, ScriptController controller) {\n");
    scriptSb.append("    super(controller);\n");
    scriptSb.append("    this.target = target;\n");
    scriptSb.append("  }\n\n");
        
    // Run method
    scriptSb.append("  @Override\n");
    scriptSb.append("  public void run() {\n");

    generateRunBody(scriptSb, code);
    
    scriptSb.append("  }\n");
    scriptSb.append("}\n");

    PrintWriter pw = new PrintWriter(openOutputFile(clz, "scripts"));
    pw.write(scriptSb.toString());
    pw.close();
  }
  
  /*
   * Handles the actual script generation in the Script-subclass.
   */
  void generateRunBody(StringBuilder scriptSb, Array<Array<ScratchObject>> code) {
    // Skipping element 0 as that is the hat statement, which is 
    // handled by generateRegisterStatement.
    for (int i = 1; i < code.size(); i++) {
      generateStatement(2, scriptSb, code.get(i));      
    }    
  }
  
  /*
   * Make an indent string (lvl * 2 number of spaces) 
   */
  String mkIndent(int lvl) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < lvl; i++) {
      sb.append("  ");
    }
    return sb.toString();
  }
  
  /*
   * Make a loop var appropriate to the given indent level (i, j, k, l etc)
   */
  String mkLoopVar(int lvl) {
    return new String(new byte[] { (byte)('i' + lvl) });
  }
  
  /*
   * Process an argument. If the argument is a nested expression, it will
   * be expanded. Otherwise it will be converted to an appropriate string.
   * 
   * The 'coerce' argument allows a type coercion method to be supplied
   * which will be used if the argument is a nested expression.
   */
  @SuppressWarnings("unchecked")
  String processArg(ScratchObject arg) {
    if (arg instanceof Array) {
      StringBuilder buf = new StringBuilder();
      generateStatement(0, buf, (Array<ScratchObject>)arg, false);
      return buf.toString();
    } else {
      return arg.toString();        
    }
  }
  
  HashMap<String, Class<?>> varTypes = new HashMap<String, Class<?>>();
  
  /* ** VAR TYPE TRACKING ** */
  @SuppressWarnings("rawtypes")
  void setVarType(String name, ScratchObject o) {
    if (!varTypes.containsKey(name)) {
      // TODO this is a hack; if nested expr use type of first argument. 
      //      This needs to be fixed as it simply doesn't work!
      //
      //      Rather than trying to track types, we could just coerce them
      //      as needed, depending on what type of expr they're being used in...
      while (o instanceof Array) {
        System.err.println("WARNING: Smells like code with all the var type inferring...");
        o = ((Array)o).get(1);      
      }
      
      //System.out.println("SETTING VAR TYPE FOR '" + name + "' (clz: " + o.getClass() + " : "+ o.toString() + ")");

      try {
        Integer.parseInt(o.toString());
        //System.out.println("  Parsed as int");
        varTypes.put(name, Long.class);
      } catch (NumberFormatException e) {
        try {
          Double.parseDouble(o.toString());
          //System.out.println("  Parsed as double");
          varTypes.put(name, Double.class);
        } catch (NumberFormatException f) {
          //System.out.println("  Defaulting to String");
          varTypes.put(name, String.class);        
        }
      }
    }
  }
  
  String getVarType(String name) {
    Class<?> clz = varTypes.get(name);
    if (clz == null) {
      // TODO default to long for now, maybe revisit (again) later?
      varTypes.put(name, Long.class);
      return int.class.getName();
    } else {
      return clz.getName();
    }
  }

  /* 
   * Generates an individual statement (or block, in the case of doUntil etc). in the run body.
   */
  void generateStatement(int indentLvl, StringBuilder scriptSb, Array<ScratchObject> stmt) {
    generateStatement(indentLvl, scriptSb, stmt, true);
  }
  
  /* 
   * Generates an individual statement (or block, in the case of doUntil etc). in the run body.
   * 
   * If statement is true, then comments and trailing newlines will be generated (i.e. the statement
   * is standalong rather than an embedded expression (e.g. an argument)).
   * 
   * This is the main code generation method.
   */
  @SuppressWarnings("unchecked")
  void generateStatement(int indentLvl, StringBuilder scriptSb, Array<ScratchObject> stmt, boolean statement) {
    String indent = mkIndent(indentLvl);
    
    if (statement) {
      // generate comments
      scriptSb.append(indent).append("// ").append(stmt.get(0)).append(" (").append(stmt.size()-1).append(" argument(s))\n");
      for (int i = 1; i < stmt.size(); i++) {
        scriptSb.append(indent).append("//   #").append(i).append(" : ").append(stmt.get(i)).append("\n");        
      }
    }
    
    // generate code
    Symbol type = (Symbol)stmt.get(0);
    
    // CONTROL
    if ("stopAll".equals(type.symbol())) {
      scriptSb.append(indent).append("controller().stopAll()");
    } else if ("broadcast:".equals(type.symbol())) {
      scriptSb.append(indent).append("controller().broadcast(\"").append(stmt.get(1)).append("\")");
    } else if ("wait:elapsed:from:".equals(type.symbol())) {
      // TODO not supporting from yet
      scriptSb.append(indent).append("try {\n");
      scriptSb.append(indent).append("  Thread.sleep((long)(").append(stmt.get(1)).append(" * 1000));\n");
      scriptSb.append(indent).append("} catch (InterruptedException e) { /* do nothing */ }");
    } else if ("keyPressed:".equals(type.symbol())) {
      scriptSb.append(indent).append("controller().checkKeyPress(\"").append(stmt.get(1)).append("\")");
    // MEDIA
    } else if ("showBackground:".equals(type.symbol())) {
      scriptSb.append(indent).append("target.showBackground(\"").append(stmt.get(1)).append("\")");      
    } else if ("changeGraphicEffect:by:".equals(type.symbol())) {
      scriptSb.append(indent).append("target.changeGraphicEffect(\"").append(stmt.get(1)).append("\", ").append(processArg(stmt.get(2))).append(")");
    } else if ("playSound:".equals(type.symbol())) {
      scriptSb.append(indent).append("target.playSound(\"").append(stmt.get(1)).append("\")");
    } else if ("changeVolumeBy:".equals(type.symbol())) {
      scriptSb.append(indent).append("target.changeVolumeBy(").append(stmt.get(1)).append(")");
    } else if ("stopAllSounds".equals(type.symbol())) {
      scriptSb.append(indent).append("controller().stopAllSounds()");
          
    // FLOW CONTROL
    } else if ("doIf".equals(type.symbol())) {
      scriptSb.append(indent).append("if (").append(processArg(stmt.get(1))).append(") {\n");
      generateNestedBlock(indentLvl + 1, scriptSb, (Array<Array<ScratchObject>>)stmt.get(2));
      scriptSb.append(indent).append("}\n\n");
      statement = false;  // hack to stop semicolon being generated for us!
    } else if ("doIfElse".equals(type.symbol())) {
      scriptSb.append(indent).append("if (").append(processArg(stmt.get(1))).append(") {\n");
      generateNestedBlock(indentLvl + 1, scriptSb, (Array<Array<ScratchObject>>)stmt.get(2));
      scriptSb.append(indent).append("} else {");
      generateNestedBlock(indentLvl + 1, scriptSb, (Array<Array<ScratchObject>>)stmt.get(3));
      scriptSb.append(indent).append("}\n\n");
      statement = false;  // hack to stop semicolon being generated for us!
    } else if ("doRepeat".equals(type.symbol())) {
      String loopVar = mkLoopVar(indentLvl);
      scriptSb.append(indent).append("for (int ").append(loopVar).append(" = 0; ").append(loopVar).append(" < ").append(stmt.get(1)).append("; ").append(loopVar).append("++) {\n");
      generateNestedBlock(indentLvl + 1, scriptSb, (Array<Array<ScratchObject>>)stmt.get(2));      
      scriptSb.append(indent).append("}\n\n");
      statement = false;  // hack to stop semicolon being generated for us!
    } else if ("doUntil".equals(type.symbol())) {
      scriptSb.append(indent).append("while (!(").append(processArg(stmt.get(1))).append(")) {\n");
      generateNestedBlock(indentLvl + 1, scriptSb, (Array<Array<ScratchObject>>)stmt.get(2));
      scriptSb.append(indent).append("}\n\n");
      statement = false;  // hack to stop semicolon being generated for us!
    } else if ("doForever".equals(type.symbol())) {
      scriptSb.append(indent).append("while (!controller().allStopped()) {\n");
      generateNestedBlock(indentLvl + 1, scriptSb, (Array<Array<ScratchObject>>)stmt.get(1));
      scriptSb.append(indent).append("}\n\n");
      statement = false;  // hack to stop semicolon being generated for us!
    } else if ("doReturn".equals(type.symbol())) {
      scriptSb.append(indent).append("break");
      
    // VARS  
    } else if ("hideVariable:".equals(type.symbol())) {
      scriptSb.append(indent).append("target.hideVariable(\"").append(stmt.get(1)).append("\")");
    } else if ("showVariable:".equals(type.symbol())) {
      scriptSb.append(indent).append("target.showVariable(\"").append(stmt.get(1)).append("\")");
    } else if ("readVariable".equals(type.symbol())) {
      scriptSb.append(indent).append("(").append(getVarType(stmt.get(1).toString())).append(")controller().getVar(\"").append(stmt.get(1)).append("\")");      
    } else if ("changeVariable".equals(type.symbol())) {
      setVarType(stmt.get(1).toString(), stmt.get(3));
      if (statement) {
        scriptSb.append(indent).append("// Inferred type as: ").append(getVarType(stmt.get(1).toString())).append("\n");
      }
      if ("setVar:to:".equals(((Symbol)stmt.get(2)).symbol())) {
        // is a literal
        scriptSb.append(indent).append("controller().setVar(\"").append(stmt.get(1)).append("\", ").append(processArg(stmt.get(3))).append(")");
      } else if ("changeVar:by:".equals(((Symbol)stmt.get(2)).symbol())) {
          // is relative
          scriptSb.append(indent).append("controller().setVar(\"").append(stmt.get(1)).append("\", ((").append(getVarType(stmt.get(1).toString())).append(")controller().getVar(\"").append(stmt.get(1)).append("\")) + ").append(processArg(stmt.get(3))).append(")");
      } else {
        scriptSb.append(indent).append("throwUnsupported(\"changeVariable type '").append(stmt.get(2)).append("' is not yet supported\")");
      }
      
    // SPRITE ATTRS/MOVEMENT
    } else if ("getAttribute:of:".equals(type.symbol())) {
      if (stmt.size() == 1 || stmt.get(2) == this.sprite) {
        scriptSb.append(indent).append("target.");
      } else {
        scriptSb.append(indent).append("Objects.").append(scratchNameToIdentifier(((ScratchSpriteMorph)stmt.get(2)).objName().string(), false)).append(".");
      }
      String attr = stmt.get(1).toString();
      if ("x position".equals(attr)) {
        scriptSb.append("x()");        
      } else if ("y position".equals(attr)) {
        scriptSb.append("y()");        
      } else {
        throw new UnsupportedOperationException("Sprite attribute `" + attr + "' is not yet supported");
      }
    } else if ("touching:".equals(type.symbol())) {
      if (stmt.get(1) == this.sprite) {
        // should never happen? Makes no sense in any event (would always be true)
        scriptSb.append(indent).append("target.");
      } else {
        scriptSb.append(indent).append("Objects.").append(scratchNameToIdentifier(((ScratchSpriteMorph)stmt.get(1)).objName().string(), false)).append(".");
      }
      scriptSb.append("touching(target)");
    } else if ("xpos".equals(type.symbol())) {
      scriptSb.append("target.x()");
    } else if ("ypos".equals(type.symbol())) {
      scriptSb.append("target.y()");
    } else if ("gotoX:y:".equals(type.symbol())) {
      scriptSb.append(indent).append("target.setXY(").append(processArg(stmt.get(1))).append(", ").append(processArg(stmt.get(2))).append(")");
    } else if ("changeXposBy:".equals(type.symbol())) {
      scriptSb.append(indent).append("target.setX(target.x() + ").append(processArg(stmt.get(1))).append(")");
    } else if ("changeYposBy:".equals(type.symbol())) {
      scriptSb.append(indent).append("target.setY(target.y() + ").append(processArg(stmt.get(1))).append(")");
    } else if ("forward:".equals(type.symbol())) {
      scriptSb.append(indent).append("target.forward(").append(stmt.get(1)).append(")");
    } else if ("glideSecs:toX:y:elapsed:from:".equals(type.symbol())) {
      // TODO not implementing elapsed/from yet
      scriptSb.append(indent).append("target.startGlideTo(").append(processArg(stmt.get(2))).append(", ").append(processArg(stmt.get(3))).append(", ").append("(long)(").append(stmt.get(1)).append(" * 1000))");
    } else if ("gotoSpriteOrMouse:".equals(type.symbol())) {
      // TODO not implementing mouse yet - not sure what argument will be?
      if (stmt.get(1) instanceof ScratchSpriteMorph) {
        scriptSb.append(indent).append("AbstractSprite goingTo = ").append("Objects.").append(scratchNameToIdentifier(((ScratchSpriteMorph)stmt.get(1)).objName().string(), false)).append(";\n");
        scriptSb.append(indent).append("target.setXY(goingTo.x(), goingTo.y())");
      } else {
        throw new UnsupportedOperationException("gotoMouse is not yet supported");
      }
    } else if ("show".equals(type.symbol())) {
      scriptSb.append(indent).append("target.show()");
    } else if ("hide".equals(type.symbol())) {
      scriptSb.append(indent).append("target.hide()");      
    } else if ("lookLike:".equals(type.symbol())) {
      scriptSb.append(indent).append("target.setCostume(").append(mainClassName).append(".").append(scratchNameToIdentifier(stmt.get(1).toString(), false)).append(")");
    } else if ("nextCostume".equals(type.symbol())) {
      scriptSb.append(indent).append("target.nextCostume()");
    } else if ("costumeIndex".equals(type.symbol())) {
      scriptSb.append(indent).append("target.costume().index()");
    } else if ("heading:".equals(type.symbol())) {
      scriptSb.append(indent).append("target.setHeading((double)").append(stmt.get(1)).append(")");
    } else if ("turnLeft:".equals(type.symbol())) {
      scriptSb.append(indent).append("target.turnLeft((double)").append(stmt.get(1)).append(")");
    } else if ("turnRight:".equals(type.symbol())) {
      scriptSb.append(indent).append("target.turnLeft((double)").append(stmt.get(1)).append(")");
    } else if ("bounceOffEdge".equals(type.symbol())) {
      scriptSb.append(indent).append("target.bounceOffEdge()");
    } else if ("think:duration:elapsed:from:".equals(type.symbol())) {
      // TODO not supporting elapsed/from yet
      scriptSb.append(indent).append("target.think(\"").append(stmt.get(1)).append("\", (long)(").append(stmt.get(2)).append("))");
      
    // BINARY OPS
    } else if ("=".equals(type.symbol())) {
      String lStr = processArg(stmt.get(1));
      String rStr = processArg(stmt.get(2));
      scriptSb.append(indent).append(lStr).append(" == ").append(rStr);
    } else if (">".equals(type.symbol())) {
      String lStr = processArg(stmt.get(1));
      String rStr = processArg(stmt.get(2));
      scriptSb.append(indent).append(lStr).append(" > ").append(rStr);
    } else if ("<".equals(type.symbol())) {
      String lStr = processArg(stmt.get(1));
      String rStr = processArg(stmt.get(2));
      scriptSb.append(indent).append(lStr).append(" < ").append(rStr);
    } else if ("+".equals(type.symbol())) {
      String lStr = processArg(stmt.get(1));
      String rStr = processArg(stmt.get(2));
      scriptSb.append(indent).append(lStr).append(" + ").append(rStr);
    } else if ("-".equals(type.symbol())) {
      String lStr = processArg(stmt.get(1));
      String rStr = processArg(stmt.get(2));
      scriptSb.append(indent).append(lStr).append(" - ").append(rStr);
    } else if ("*".equals(type.symbol())) {
      String lStr = processArg(stmt.get(1));
      String rStr = processArg(stmt.get(2));
      scriptSb.append(indent).append(lStr).append(" * ").append(rStr);
    } else if ("/".equals(type.symbol())) {
      String lStr = processArg(stmt.get(1));
      String rStr = processArg(stmt.get(2));
      scriptSb.append(indent).append(lStr).append(" / ").append(rStr);
    } else if ("&".equals(type.symbol())) {
      String lStr = processArg(stmt.get(1));
      String rStr = processArg(stmt.get(2));
      scriptSb.append(indent).append(lStr).append(" && ").append(rStr);
    } else {
      //throw new UnsupportedOperationException(type.symbol() + " is not yet supported");
      scriptSb.append(indent).append("// TODO unsupported operation here!\n");
      scriptSb.append(indent).append("__fail_To_Compile_Because_(\"").append(type.symbol()).append(" is not yet supported\")");
    }
    
    if (statement) {
      scriptSb.append(";\n\n");
    }
  }
  
  void generateNestedBlock(int indentLvl, StringBuilder sb, Array<Array<ScratchObject>> block) {
    for (Array<ScratchObject> stmt : block) {
      generateStatement(indentLvl, sb, stmt);
    }    
  }
  
  void generateResources(StringBuilder mainSb) throws IOException {
    ResourceGenerator gen;
    
    if (stageScript) {
      gen = new ResourceGenerator(resDir, codeDir, stage);
    } else {
      gen = new ResourceGenerator(resDir, codeDir, sprite);
    }
    
    gen.generate(mainSb);    
  }  
}
