package org.avaje.agentloader.load;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.spi.AttachProvider;

import sun.tools.attach.BsdVirtualMachine;
import sun.tools.attach.LinuxVirtualMachine;
import sun.tools.attach.SolarisVirtualMachine;
import sun.tools.attach.WindowsVirtualMachine;

/**
 * Helper class with imports to "sun.tools.attach.XXXVirtualMachine";
 * 
 * Keep AgentLoader class clean from these imports. This is required to be jdk9+
 * compatible, because otherwise the JVM will fail with a NoClassDefFoundError.
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public class EmbeddedHelp {

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

  private static final boolean isWindows() {
    return File.separatorChar == '\\';
  }

  /**
   * Returns a VM from the embedded java classes. This may work if the attach.dll/attach.so
   * match to the classes and no security manager forbids this.
   */
  static VirtualMachine getVirtualMachineImplementationFromEmbeddedOnes(String pid) {
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
      throw new IllegalStateException("Native library for Attach API not available in this JRE (probably no JDK on classpath)", e);
    }

    return null;
  }
}
