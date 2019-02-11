package org.avaje.agentloader.load;

/**
 * Loader interface.
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public interface Loader {
  /**
   * Loads the agent to the specified VM
   * 
   * @param jarFilePath the agent-jar
   * @param pid         the PID of the VM where the agent should be attached
   * @param params      the agent parameters
   * @throws Exception
   */
  void loadAgent(String jarFilePath, String pid, String params) throws Exception;
}
