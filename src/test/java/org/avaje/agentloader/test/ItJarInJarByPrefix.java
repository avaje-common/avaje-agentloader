package org.avaje.agentloader.test;

import org.avaje.agentloader.AgentLoader;

/**
 * Tests the prefix load (in an uber-jar). Test is invoked by src/it/jar-in-jar-test
 */
public class ItJarInJarByPrefix extends AgentLoadBaseTest {

  /**
   * Main method for manual test run in embedded jar.
   */
  public static void main(String[] args) throws Exception {
    new ItJarInJarByPrefix().test();
  }

  private void test() throws Exception {
    testBeforeAgentLoad();
    AgentLoader.loadAgentFromClasspath("ebean-agent", "packages=org.avaje;debug=1");
    testAfterAgentLoad();
  }

}
