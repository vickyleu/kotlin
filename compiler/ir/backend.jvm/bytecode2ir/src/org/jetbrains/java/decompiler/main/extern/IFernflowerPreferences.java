// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.main.extern;

import org.jetbrains.java.decompiler.util.InterpreterUtil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface IFernflowerPreferences {
  @Name("Remove Bridge Methods")
  @Description("Removes any methods that are marked as bridge from the decompiled output.")
  String REMOVE_BRIDGE = "rbr";

  @Name("Remove Synthetic Methods And Fields")
  @Description("Removes any methods and fields that are marked as synthetic from the decompiled output.")
  String REMOVE_SYNTHETIC = "rsy";

  @Name("Decompile Inner Classes")
  @Description("Process inner classes and add them to the decompiled output.")
  String DECOMPILE_INNER = "din";

  @Name("Decompile Java 4 class references")
  @Description("Java 1 to Java 4 had a different class reference format. This resugars them properly.")
  String DECOMPILE_CLASS_1_4 = "dc4";

  @Name("Decompile Assertions")
  @Description("Decompile assert statements.")
  String DECOMPILE_ASSERTIONS = "das";

  @Name("Hide Empty super()")
  @Description("Hide super() calls with no parameters.")
  String HIDE_EMPTY_SUPER = "hes";

  @Name("Hide Default Constructor")
  @Description("Hide constructors with no parameters and no code.")
  String HIDE_DEFAULT_CONSTRUCTOR = "hdc";

  @Name("Decompile Generics")
  @Description("Decompile generics in variables, fields, and statements.")
  String DECOMPILE_GENERIC_SIGNATURES = "dgs";

  @Name("Decompile Enums")
  @Description("Decompile enums.")
  String DECOMPILE_ENUM = "den";

  @Name("Decompile Preview Features")
  @Description("Decompile features marked as preview or incubating in the latest Java versions.")
  String DECOMPILE_PREVIEW = "dpr";

  @Name("Remove reference getClass()")
  @Description("obj.new Inner() or calling invoking a method on a method reference will create a synthetic getClass() call. This removes it.")
  String REMOVE_GET_CLASS_NEW = "rgn";

  @Name("Represent boolean as 0/1")
  @Description("The JVM represents booleans as integers 0 and 1. This decodes 0 and 1 as boolean when it makes sense.")
  String BOOLEAN_TRUE_ONE = "bto";

  @Name("Synthetic Not Set")
  @Description("Treat some known structures as synthetic even when not explicitly set.")
  String SYNTHETIC_NOT_SET = "nns";

  @Name("Treat Undefined Param Type As Object")
  @Description("Treat nameless types as java.lang.Object.")
  String UNDEFINED_PARAM_TYPE_OBJECT = "uto";

  @Name("Use LVT Names")
  @Description("Use LVT names for local variables and parameters instead of var<index>_<version>.")
  String USE_DEBUG_VAR_NAMES = "udv";

  @Name("Use Method Parameters")
  @Description("Use method parameter names, as given in the MethodParameters attribute.")
  String USE_METHOD_PARAMETERS = "ump";

  @Name("Decompile Finally")
  @Description("Decompile finally blocks.")
  String FINALLY_DEINLINE = "fdi";

  @Name("Decompile Lambdas as Anonymous Classes")
  @Description("Decompile lambda expressions as anonymous classes.")
  String LAMBDA_TO_ANONYMOUS_CLASS = "lac";

  @Name("Bytecode to Source Mapping")
  @Description("Map Bytecode to source lines.")
  String BYTECODE_SOURCE_MAPPING = "bsm";

  @Name("Dump Code Lines")
  @Description("Dump line mappings to output archive zip entry extra data")
  String DUMP_CODE_LINES = "dcl";

  @Name("Ignore Invalid Bytecode")
  @Description("Ignore bytecode that is malformed.")
  String IGNORE_INVALID_BYTECODE = "iib";

  @Name("Verify Anonymous Classes")
  @Description("Verify that anonymous classes are local.")
  String VERIFY_ANONYMOUS_CLASSES = "vac";

  @Name("Try-Loop fix")
  @Description("Code with a while loop inside of a try-catch block sometimes is malformed, this fixes it.")
  String TRY_LOOP_FIX = "tlf";

  @Name("Second-Pass Stack Simplficiation")
  @Description("Simplify variables across stack bounds to resugar complex statements.")
  String SIMPLIFY_STACK_SECOND_PASS = "ssp";

  @Name("[Experimental] Verify Variable Merges")
  @Description("Double checks to make sure the validity of variable merges. If you are having strange recompilation issues, this is a good place to start.")
  String VERIFY_VARIABLE_MERGES = "vvm";

  @Name("Include Entire Classpath")
  @Description("Give the decompiler information about every jar on the classpath.")
  String INCLUDE_ENTIRE_CLASSPATH = "iec";

  @Name("Include Java Runtime")
  @Description("Give the decompiler information about the Java runtime, either 1 or current for the current runtime, or a path to another runtime")
  String INCLUDE_JAVA_RUNTIME = "jrt";

  @Name("Explicit Generic Arguments")
  @Description("Put explicit diamond generic arguments on method calls.")
  String EXPLICIT_GENERIC_ARGUMENTS = "ega";

  @Name("Logging Level")
  @Description("Logging level. Must be one of: 'info', 'debug', 'warn', 'error'.")
  String LOG_LEVEL = "log";

  @Name("[DEPRECATED] Max time to process method")
  @Description("Maximum time in seconds to process a method. This is deprecated, do not use.")
  String MAX_PROCESSING_METHOD = "mpm";

  @Name("New Line Seperator")
  @Description("Character that seperates lines in the decompiled output.")
  String NEW_LINE_SEPARATOR = "nls";

  @Name("Indent String")
  @Description("A string of spaces or tabs that is placed for each indent level.")
  String INDENT_STRING = "ind";

  @Name("Preferred line length")
  @Description("Max line length before formatting is applied.")
  String PREFERRED_LINE_LENGTH = "pll";

  @Name("User Renamer Class")
  @Description("Path to a class that implements IIdentifierRenamer.")
  String BANNER = "ban";

  @Name("Error Message")
  @Description("Message to display when an error occurs in the decompiler.")
  String ERROR_MESSAGE = "erm";

  @Name("Thread Count")
  @Description("How many threads to use to decompile.")
  String THREADS = "thr";

  String DUMP_ORIGINAL_LINES = "__dump_original_lines__";
  String UNIT_TEST_MODE = "__unit_test_mode__";

  String LINE_SEPARATOR_WIN = "\r\n";
  String LINE_SEPARATOR_UNX = "\n";

  @Name("JAD-Style Parameter Naming")
  @Description("Use JAD-style variable naming for parameters.")
  String USE_JAD_PARAMETER_NAMING = "jpr";

  @Name("Skip Extra Files")
  @Description("Skip copying non-class files from the input folder or file to the output")
  String SKIP_EXTRA_FILES = "sef";

  @Name("Warn about inconsistent inner attributes")
  @Description("Warn about inconsistent inner class attributes")
  String WARN_INCONSISTENT_INNER_CLASSES = "win";

  @Name("Decompiler Comments")
  @Description("Sometimes, odd behavior of the bytecode or unfixable problems occur. This enables or disables the adding of those to the decompiled output.")
  String DECOMPILER_COMMENTS = "dec";

  @Name("Decompile complex constant-dynamic expressions")
  @Description("Some constant-dynamic expressions can't be converted to a single Java expression with identical run-time behaviour. This decompiles them to a similar non-lazy expression, marked with a comment.")
  String DECOMPILE_COMPLEX_CONDYS = "dcc";

  Map<String, Object> DEFAULTS = getDefaults();

  static Map<String, Object> getDefaults() {
    Map<String, Object> defaults = new HashMap<>();

    defaults.put(REMOVE_BRIDGE, "1");
    defaults.put(REMOVE_SYNTHETIC, "1");
    defaults.put(DECOMPILE_INNER, "1");
    defaults.put(DECOMPILE_CLASS_1_4, "1");
    defaults.put(DECOMPILE_ASSERTIONS, "1");
    defaults.put(HIDE_EMPTY_SUPER, "1");
    defaults.put(HIDE_DEFAULT_CONSTRUCTOR, "1");
    defaults.put(DECOMPILE_GENERIC_SIGNATURES, "1");
    defaults.put(DECOMPILE_ENUM, "1");
    defaults.put(REMOVE_GET_CLASS_NEW, "1");
    defaults.put(BOOLEAN_TRUE_ONE, "1");
    defaults.put(SYNTHETIC_NOT_SET, "0");
    defaults.put(UNDEFINED_PARAM_TYPE_OBJECT, "1");
    defaults.put(USE_DEBUG_VAR_NAMES, "1");
    defaults.put(USE_METHOD_PARAMETERS, "1");
    defaults.put(FINALLY_DEINLINE, "1");
    defaults.put(LAMBDA_TO_ANONYMOUS_CLASS, "0");
    defaults.put(BYTECODE_SOURCE_MAPPING, "0");
    defaults.put(DUMP_CODE_LINES, "0");
    defaults.put(IGNORE_INVALID_BYTECODE, "0");
    defaults.put(VERIFY_ANONYMOUS_CLASSES, "0");
    defaults.put(TRY_LOOP_FIX, "1"); // Try loop fix is stable, and fixes hard to notice bugs
    defaults.put(SIMPLIFY_STACK_SECOND_PASS, "1"); // Generally produces better bytecode, useful to debug if it does something strange
    defaults.put(VERIFY_VARIABLE_MERGES, "0"); // Produces more correct code in rare cases, but hurts code cleanliness in the majority of cases. Default off until a better fix is created.
    defaults.put(DECOMPILE_PREVIEW, "1"); // Preview features are useful to decompile in almost all cases

    defaults.put(INCLUDE_ENTIRE_CLASSPATH, "0");
    defaults.put(INCLUDE_JAVA_RUNTIME, "");
    defaults.put(EXPLICIT_GENERIC_ARGUMENTS, "0");

    defaults.put(LOG_LEVEL, IFernflowerLogger.Severity.INFO.name());
    defaults.put(MAX_PROCESSING_METHOD, "0");
    defaults.put(NEW_LINE_SEPARATOR, (InterpreterUtil.IS_WINDOWS ? "0" : "1"));
    defaults.put(INDENT_STRING, "   ");
    defaults.put(PREFERRED_LINE_LENGTH, "160");
    // Point users towards reporting bugs if things don't decompile properly
    defaults.put(ERROR_MESSAGE, "Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)");
    defaults.put(UNIT_TEST_MODE, "0");
    defaults.put(DUMP_ORIGINAL_LINES, "0");
    defaults.put(THREADS, String.valueOf(Runtime.getRuntime().availableProcessors()));
    defaults.put(SKIP_EXTRA_FILES, "0");
    defaults.put(WARN_INCONSISTENT_INNER_CLASSES, "1");
    defaults.put(DECOMPILER_COMMENTS, "1");
    defaults.put(DECOMPILE_COMPLEX_CONDYS, "0");

    return Collections.unmodifiableMap(defaults);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface Name {
    String value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface Description {
    String value();
  }
}
