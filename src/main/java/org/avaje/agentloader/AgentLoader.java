package org.avaje.agentloader;

import org.avaje.agentloader.find.ByClassNameJarFinder;
import org.avaje.agentloader.find.ByPrefixJarFinder;
import org.avaje.agentloader.find.FindResult;
import org.avaje.agentloader.find.JarFinder;
import org.avaje.agentloader.load.DirectLoader;
import org.avaje.agentloader.load.ExternalLoader;
import org.avaje.agentloader.load.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides the ability to load an agent on a running process.
 * <p>
 * 
 * @author Richard Vowles - http://gplus.to/RichardVowles
 * @author Roland Praml, FOCONIS AG
 */
public class AgentLoader {

  private static final Logger log = LoggerFactory.getLogger(AgentLoader.class);

  private static final List<String> loaded = new ArrayList<String>();

  /**
   * The OpenJDK's property for specifying the legality of self-attachment.
   */
  private static final String JDK_ALLOW_SELF_ATTACH = "jdk.attach.allowAttachSelf";

  /**
   * Checks if we can direct attach the agent, because java9+ forbids self attach
   * in default configuration. You may find more details here:
   * https://github.com/raphw/byte-buddy/issues/295
   */
  private static boolean directAttach() {
    String version = System.getProperty("java.version");

    if (version.startsWith("1.")) { // means 1.6/1.7/1.8. Java 9+ will return 9./10./11.
      log.trace("using direct-attach for java {} < 9", version);
      return true;

    } else if (Boolean.getBoolean(JDK_ALLOW_SELF_ATTACH)) {
      log.trace("using direct-attach, because {} is set", JDK_ALLOW_SELF_ATTACH);
      return true;

    } else {
      log.trace("using external-attach for java {}", version);
      return false;
    }
  }

  /**
   * Returns the PID of this VM
   */
  private static final String discoverPid() {
    String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
    int p = nameOfRunningVM.indexOf('@');
    return nameOfRunningVM.substring(0, p);
  }

  /**
   * Load an agent providing the full file path.
   */
  public static boolean loadAgent(String jarFilePath) {
    return loadAgent(jarFilePath, "");
  }

  /**
   * Load an agent providing the full file path with parameters.
   */
  public synchronized static boolean loadAgent(String jarFilePath, String params) {
    if (loaded.contains(jarFilePath)) {
      return false;
    }
    long time = System.currentTimeMillis();
    log.info("dynamically loading javaagent for {}", jarFilePath);
    String pid = discoverPid();

    try {
      Loader loader = directAttach() ? new DirectLoader() : new ExternalLoader();
      loader.loadAgent(jarFilePath, pid, params);
      time = System.currentTimeMillis() - time;
      log.info("agent loaded by {} in {} ms", loader.getClass().getSimpleName(), time);
      loaded.add(jarFilePath);
      return true;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Load the agent from the classpath using its name.
   * 
   * @deprecated Better use {@link #loadAgentByMainClass(String)}, as this may not
   *             find nested jars on jdk9+
   */
  public static boolean loadAgentFromClasspath(String agentName) {
    return loadAgentFromClasspath(agentName, "");
  }

  /**
   * Load the agent from the classpath using its name and passing params.
   * 
   * @deprecated Better use {@link #loadAgentByMainClass(String)}, as this may not
   *             find nested jars on jdk9+
   */
  public synchronized static boolean loadAgentFromClasspath(String agentName, String params) {
    return loadAgentFromClasspath(agentName, getClassLoader(), params);
  }

  /**
   * Load the agent from the classloader's classpath using its name and passing params.
   * 
   * @deprecated Better use {@link #loadAgentByMainClass(String)}, as this may not
   *             find nested jars on jdk9+
   */
  public synchronized static boolean loadAgentFromClasspath(String agentName, ClassLoader cl, String params) {
    if (loaded.contains(agentName)) {
      // the agent is already loaded
      return true;
    }

    JarFinder finder = new ByPrefixJarFinder();
    try (FindResult result = finder.find(agentName, cl)) {
      if (result != null) {

        boolean ret = loadAgent(result.getFile().getAbsolutePath(), params);
        loaded.add(agentName);
        return ret;
      }
    }

    // Agent not found and not loaded
    return false;
  }

  /**
   * Load the agent-jar that contains the given (main) class. This should be u
   * used on jvm9+, as it finds also nested jars.
   */
  public static boolean loadAgentByMainClass(String mainClass) {
    return loadAgentByMainClass(mainClass, "");
  }

  /**
   * Short form of {@link #loadAgentByMainClass(String, ClassLoader, String)}.
   * Tries to use the contextClassLoader.
   */
  public synchronized static boolean loadAgentByMainClass(String mainClass, String params) {
    return loadAgentByMainClass(mainClass, getClassLoader(), params);
  }

  /**
   * Load the agent-jar that contains the given (main) class and passing params.
   * This should be u used on jvm9+, as it finds also nested jars.
   * 
   * @param mainClass specify the MainClass (e.g. "io.ebean.enhance.Transformer")
   *                  so that the correct jar can be located.
   * @param params    the agent parameters
   * @return true if the agent was loaded (or is already loaded)
   */
  public synchronized static boolean loadAgentByMainClass(String mainClass, ClassLoader cl, String params) {

    if (loaded.contains(mainClass)) {
      // the agent is already loaded
      return true;
    }

    JarFinder finder = new ByClassNameJarFinder();
    try (FindResult result = finder.find(mainClass, cl)) {
      if (result != null) {

        boolean ret = loadAgent(result.getFile().getAbsolutePath(), params);
        loaded.add(mainClass);
        return ret;
      }
    }

    // Agent not found and not loaded
    return false;
  }
  
  private static ClassLoader getClassLoader() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) {
      cl = JarFinder.class.getClassLoader();
    }
    return cl;
  }
}
