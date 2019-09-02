package org.avaje.agentloader.load;

import java.io.File;
import java.io.InputStream;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import org.avaje.agentloader.AgentLoader;
import org.avaje.agentloader.find.ByClassNameJarFinder;
import org.avaje.agentloader.find.FindResult;
import org.avaje.agentloader.find.JarFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader that runs an external java process to attach the agent. This is
 * required for jdk9+, as direct attach is not possible here.
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public class ExternalLoader implements Loader {

  /**
   * The prefix of the artifact jar. This is required to find ourself in the class
   * path.
   */
  private static final Logger log = LoggerFactory.getLogger(AgentLoader.class);

  public static final boolean isWindows() {
    return File.separatorChar == '\\';
  }

  /**
   * Installs a Java agent to the current VM via an external process. This is
   * typically required starting with OpenJDK 9 when the
   * {@code jdk.attach.allowAttachSelf} property is set to {@code false} what is
   * the default setting.
   *
   * (idea is taken from the byteBuddy project)
   */
  @Override
  public void loadAgent(String jarFilePath, String pid, String params) throws Exception {

    JarFinder finder = new ByClassNameJarFinder();
    try (FindResult result = finder.find(AgentLoader.class.getName(), AgentLoader.class.getClassLoader())) {
      File classPath = null; 

      if (result != null) {
        classPath = result.getFile();
      } else {
        // we did not find the jar, so try to resolve via codeSource
        // probably in a unit-test
        ProtectionDomain pd = DirectLoader.class.getProtectionDomain();
        CodeSource cs = pd.getCodeSource();
        if (cs != null) {
          File file = new File(cs.getLocation().toURI());
          if (file.isAbsolute() && file.exists()) {
            classPath = file;
          }
        }
      }

      if (classPath == null) {
        log.error("Could not find classpath for loader");
        return;
      }

      StringBuilder cmd = new StringBuilder();
      cmd.append(System.getProperty("java.home")).append(File.separatorChar).append("bin").append(File.separatorChar);
      if (isWindows()) {
        cmd.append("java.exe");
      } else {
        cmd.append("java");
      }

      ProcessBuilder procBuilder = new ProcessBuilder(cmd.toString(), // path/to/java
          "-cp", classPath.getAbsolutePath(), // -cp <this-jar>
          DirectLoader.class.getName(), // <class-with-main>
          jarFilePath, pid, params); // >params for main>

      procBuilder.redirectErrorStream(true);

      Process proc = procBuilder.start();

      try (InputStream is = proc.getInputStream()) {
        if (proc.waitFor() == DirectLoader.LOAD_SUCCESS) {
          log.info("External attach was successful");
        } else { // dump first 4k of stdout if there was any error.
          byte[] buf = new byte[4096];
          int i = is.read(buf);
          log.error("External attach faileld: {}, cmd: {}", i == -1 ? "(no more info)" : new String(buf, 0, i),
              procBuilder.command());
        }
      }
    }
  }
}
