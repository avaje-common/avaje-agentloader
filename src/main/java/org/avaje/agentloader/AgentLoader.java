package org.avaje.agentloader;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.spi.AttachProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.tools.attach.BsdVirtualMachine;
import sun.tools.attach.LinuxVirtualMachine;
import sun.tools.attach.SolarisVirtualMachine;
import sun.tools.attach.WindowsVirtualMachine;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;


/**
 * Provides the ability to load an agent on a running process.
 * <p>
 * author: Richard Vowles - http://gplus.to/RichardVowles
 */
public class AgentLoader {

  private static final Logger log = LoggerFactory.getLogger(AgentLoader.class);

  private static final List<String> loaded = new ArrayList<String>();

  /**
   * Buffer size used when extracting the agent (when it is embedded).
   */
  private static final int IO_BUFFER_SIZE = 1024 * 4;

  private static final String discoverPid() {
    String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
    int p = nameOfRunningVM.indexOf('@');
    return nameOfRunningVM.substring(0, p);
  }

  private static final AttachProvider ATTACH_PROVIDER = new AttachProvider() {
    @Override
    public String name() {
      return null;
    }

    @Override
    public String type() {
      return null;
    }

    @Override
    public VirtualMachine attachVirtualMachine(String id) {
      return null;
    }

    @Override
    public List<VirtualMachineDescriptor> listVirtualMachines() {
      return null;
    }
  };

  /**
   * Load an agent providing the full file path.
   */
  public static void loadAgent(String jarFilePath) {
    loadAgent(jarFilePath, "");
  }

  /**
   * Load an agent providing the full file path with parameters.
   */
  public static void loadAgent(String jarFilePath, String params) {

    log.info("dynamically loading javaagent for " + jarFilePath);
    try {

      String pid = discoverPid();

      VirtualMachine vm;
      if (AttachProvider.providers().isEmpty()) {
        vm = getVirtualMachineImplementationFromEmbeddedOnes(pid);
      } else {
        vm = VirtualMachine.attach(pid);
      }

      vm.loadAgent(jarFilePath, params);
      vm.detach();

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Load the agent from the classpath using its name.
   */
  public static void loadAgentFromClasspath(String agentName) {
    loadAgentFromClasspath(agentName, "");
  }

  /**
   * Load the agent from the classpath using its name and passing params.
   */
  public synchronized static boolean loadAgentFromClasspath(String agentName, String params) {

    if (loaded.contains(agentName)) {
      // the agent is already loaded
      return true;
    }
    try {
      // Search for the agent jar in the classpath
      if (AgentLoader.class.getClassLoader() instanceof URLClassLoader) {
        URLClassLoader cl = (URLClassLoader) (AgentLoader.class.getClassLoader());
        for (URL url : cl.getURLs()) {
          if (isMatch(url, agentName)) {
            // We have found the agent jar in the classpath
            String fullName = url.toURI().getPath();

            boolean isEmbedded = false;
            if (isAgentEmbeddedInJar(url, fullName)) {
              // extract the agent jar into a tmp directory for use
              fullName = extractJar(url, agentName);
              isEmbedded = true;
            }

            if (fullName != null && !loaded.contains(fullName)) {
              if (fullName.startsWith("/") && isWindows()) {
                fullName = fullName.substring(1);
              }

              try {
                loadAgent(fullName, params);
                loaded.add(fullName);
                return true;
              } finally {
                if (isEmbedded) {
                  try {
                    new File(fullName).delete();
                  } catch (Exception ex) {
                    log.warn("Cannot delete Agent file from JVM temporary folder", ex);
                  }
                }
              }
            }

          }
        }
      }

      // Agent not found and not loaded
      return false;

    } catch (URISyntaxException use) {
      throw new RuntimeException(use);
    }
  }

  /**
   * Return true if the agent jar is embedded. In this case we extract it out into a tmp directory.
   */
  private static boolean isAgentEmbeddedInJar(URL url, String fullName) {
    return fullName == null && url.getProtocol().equals("jar") && url.getPath().contains("!/");
  }

  /**
   * Check to see if this url/jar matches our agent name.
   */
  private static boolean isMatch(URL url, String partial) {
    String fullPath = url.getFile();
    int lastSlash = fullPath.lastIndexOf('/');
    if (lastSlash < 0) {
      return false;
    }
    /**
     * Use 'contains' so ignoring the version of the agent and offset of inner jar
     */
    return fullPath.contains(partial);
  }

  private static final boolean isWindows() {
    return File.separatorChar == '\\';
  }

  /**
   * @return true if the path is an embedded JAR file.
   *
   * If it is an embedded JAR file, we only need to export it.
   * Otherwise, we have to extract all class files, and package them into a JAR file.
   */
  private static boolean isEmbededJar(String path) {
    return path.endsWith(".jar!/");
  }

  /**
   * This method will extract agent JAR file from URL path.
   * Due to the package implementation, this method will cover two cases as below:
   * Embedded Class Files: jar:file:path-to-filename.war!/WEB-INF/jar/jar-file/
   * Embedded Jar Files: jar:file:path-to-filename.war!/WEB-INF/jar/jar-file!/
   *
   * @param path is full url entry in the classpath
   * @param agentName is the agent name that we are trying to match
   * @return null if it fails or a full path to the jar file if it succeeds
   */
  public static String extractJar(URL path, String agentName) {
    String fullPath = null;
    OutputStream outputJar = null;
    JarFile inputZip = null;

    log.debug("Extracting agent {} from source {}", agentName, path.getPath());

    try {
      String[] jarNames = path.getPath().split(":");
      String[] packageFileAndFileOffset = jarNames[1].split("!/");

      // the full path of Final Agent Jar File
      fullPath = System.getProperty("java.io.tmpdir") + "/" + agentName + ".jar";

      String packageFile = packageFileAndFileOffset[0];
      String fileOffset = packageFileAndFileOffset[1];
      inputZip = new JarFile(packageFile);

      boolean isJarFile = isEmbededJar(jarNames[1]);
      outputJar = isJarFile ?
              new FileOutputStream(fullPath) :
              new JarOutputStream(new FileOutputStream(fullPath));

      exportFile(inputZip, outputJar, fileOffset, isJarFile);
      log.debug("Extracted agent to {}", fullPath);

    } catch (Exception ex) {
      log.error("Failed to export agent " + agentName, ex);
    } finally {
      if (outputJar != null) {
        try {
          outputJar.close();
        } catch (IOException ioEx) {
          log.error("Error closing Agent JarFile", ioEx);
        }
      }
      if (inputZip != null) {
        try {
          inputZip.close();
        } catch (IOException ioEx) {
          log.error("Error closing input JarFile", ioEx);
        }
      }
    }

    return fullPath;
  }

  /**
   * Export the file from source JarFile to target file
   *
   * @param source is the source Jar file
   * @param target is the target file (either JarFile, or File)
   * @param partial if offset of expected file in the source Jar file
   * @param isJar is flag of expected file type
   *
   */
  protected static void exportFile(JarFile source, OutputStream target, String partial, boolean isJar) throws NoSuchFieldException, IllegalAccessException, IOException {
    Enumeration<JarEntry> entries = source.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();

      if (entry.getName().startsWith(partial)) {
        log.debug("copying {} to output", entry.getName());
        if (isJar) {
          // Only need to export a single JAR file
          copyBytes(source.getInputStream(entry), target);
          break;
        } else {
          // Need to extract all class files and put into a new JarFile
          String internalName = entry.getName().substring(partial.length());
          JarEntry ze = new JarEntry(entry);
          Field f = ze.getClass().getSuperclass().getDeclaredField("name");
          f.setAccessible(true);
          f.set(ze, internalName);

          try {
            ((JarOutputStream) target).putNextEntry(ze);
            copyBytes(source.getInputStream(entry), target);
          } finally {
            ((JarOutputStream) target).closeEntry();
          }
        }
      }
    }

  }

  /**
   * Copy the bytes from input to output streams (using a 4K buffer).
   */
  protected static long copyBytes(InputStream input, OutputStream output) throws IOException {

    byte[] buffer = new byte[IO_BUFFER_SIZE];

    long count = 0;
    int n;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  private static VirtualMachine getVirtualMachineImplementationFromEmbeddedOnes(String pid) {
    try {
      if (isWindows()) {
        return new WindowsVirtualMachine(ATTACH_PROVIDER, pid);
      }

      String osName = System.getProperty("os.name");

      if (osName.startsWith("Linux") || osName.startsWith("LINUX")) {
        return new LinuxVirtualMachine(ATTACH_PROVIDER, pid);

      } else if (osName.startsWith("Mac OS X")) {
        return new BsdVirtualMachine(ATTACH_PROVIDER, pid);

      } else if (osName.startsWith("Solaris")) {
        return new SolarisVirtualMachine(ATTACH_PROVIDER, pid);
      }

    } catch (AttachNotSupportedException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (UnsatisfiedLinkError e) {
      throw new IllegalStateException("Native library for Attach API not available in this JRE", e);
    }

    return null;
  }

}
