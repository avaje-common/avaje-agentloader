package org.avaje.agentloader.load;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.spi.AttachProvider;

/**
 * Contains the direct loader code. This class has also a main method, so that
 * it can be launched as seaparate process. (Must not have dependencies to
 * SLF4J)
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public class DirectLoader implements Loader {

  public static final int LOAD_SUCCESS = 42;

  /**
   * Main method will be invoked by AgentLoader.loadAgentExternal
   * 
   * @param args
   *             <ol>
   *             <li>the jarFilePath</li>
   *             <li>the PID</li>
   *             <li>the agent arguments</li>
   *             </ol>
   * @return {@value #LOAD_SUCCESS}
   */
  public static void main(String args[]) throws Exception {
    new DirectLoader().loadAgent(args[0], args[1], args[2]);
    System.exit(LOAD_SUCCESS);
  }

  /**
   * Load an agent with direct attach.
   */
  @Override
  public void loadAgent(String jarFilePath, String pid, String params) throws Exception {
    VirtualMachine vm;
    if (AttachProvider.providers().isEmpty()) {
      vm = EmbeddedHelp.getVirtualMachineImplementationFromEmbeddedOnes(pid);
    } else {
      vm = VirtualMachine.attach(pid);
    }
    vm.loadAgent(jarFilePath, params);
    vm.detach();
  }

}
