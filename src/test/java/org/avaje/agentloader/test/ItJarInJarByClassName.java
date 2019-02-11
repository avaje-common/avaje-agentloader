package org.avaje.agentloader.test;

import org.avaje.agentloader.AgentLoader;

/**
 * Tests the classname load (in an uber-jar). Test is invoked by src/it/jar-in-jar-test
 */
public class ItJarInJarByClassName extends AgentLoadBaseTest {

  /**
   * Main method for manual test run in embedded jar.
   */
  public static void main(String[] args) throws Exception {
    new ItJarInJarByClassName().test();
  }

  private void test() throws Exception {
    testBeforeAgentLoad();

    // NOTE: we can only perform ONE test per JVM launch.
    // AgentLoader.loadAgentFromClasspath("ebean-agent", "packages=org.avaje;debug=1");
    AgentLoader.loadAgentByMainClass("io.ebean.enhance.Transformer", "packages=org.avaje;debug=1");
    testAfterAgentLoad();
  }


}
