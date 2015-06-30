package org.avaje.agentloader;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.spi.AttachProvider;
import org.apache.commons.io.IOUtils;
import sun.tools.attach.BsdVirtualMachine;
import sun.tools.attach.LinuxVirtualMachine;
import sun.tools.attach.SolarisVirtualMachine;
import sun.tools.attach.WindowsVirtualMachine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import java.util.logging.Logger;

/**
 * Provides the ability to load an agent on a running process.
 * <p/>
 * author: Richard Vowles - http://gplus.to/RichardVowles
 */
public class AgentLoader {

  private static final Logger log = Logger.getLogger(AgentLoader.class.getName());

  private static final List<String> loaded = new ArrayList<String>();

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
            if (fullName == null && url.getProtocol().equals("jar") && url.getPath().contains("!/")) {
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
                  new File(fullName).delete();
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
   * This method takes the jar:file:path-to-filename.war!/WEB-INF/jar/jar-file/ offset that is included in the
   * url classpath and extracts out a single jar containing the files in that match that url.
   *
   * @param path    - full url entry in the classpath
   * @param partial - the name of the partial we are trying to match
   * @return null if it fails or a full path to the jar file if it succeeds
   */
  public static String extractJar(URL path, String partial) {
    String fullPath = null;

    String[] jarNames = path.getPath().split(":");

    if (jarNames.length >= 2) {
      String fileAndOffset = jarNames[1];
      int pos = fileAndOffset.indexOf('!');
      if (pos >= 0) {
        String file = fileAndOffset.substring(0, pos);
        String offset = fileAndOffset.substring(pos + 2);
        int offsetLength = offset.length();

        fullPath = System.getProperty("java.io.tmpdir") + "/" + partial + ".jar";
        JarOutputStream outputJar = null;
        JarFile inputZip = null;
        try {
          outputJar = new JarOutputStream(new FileOutputStream(fullPath));
          inputZip = new JarFile(file);

          Enumeration<JarEntry> entries = inputZip.entries();
          while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            if (entry.getName().startsWith(offset)) {
              try {
                String internalName = entry.getName().substring(offsetLength);
                JarEntry jarEntry = new JarEntry(entry);
                Field f = jarEntry.getClass().getSuperclass().getDeclaredField("name");
                f.setAccessible(true);
                f.set(jarEntry, internalName);
                jarEntry.setCompressedSize(0);

                outputJar.putNextEntry(jarEntry);
                IOUtils.copy(inputZip.getInputStream(entry), outputJar);
                outputJar.closeEntry();

              } catch (Exception ex) {
                log.warning("Cannot copy single JarEntry '" + entry.getName() + "'");
                ex.printStackTrace();
              }
            }
          }

        } catch (Exception ex) {
          log.warning("Failed to copy partial " + partial);
          ex.printStackTrace();
        } finally {
          if (outputJar != null) {
            try {
              outputJar.close();
            } catch (IOException ioEx) {
            }
          }
          if (inputZip != null) {
            try {
              inputZip.close();
            } catch (IOException ioEx) {
            }
          }
          fullPath = null;
        }
      }
    }

    return fullPath;
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
