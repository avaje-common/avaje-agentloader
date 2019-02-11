package org.avaje.agentloader.test;

import org.avaje.agentloader.AgentLoader;
import org.junit.Test;

/**
 * Standard junit test
 */
public class TestAgentLoad extends AgentLoadBaseTest {

  @Test
  public void testWithEbeanAgent() throws Exception {
    testBeforeAgentLoad();

    // NOTE: we can only perform ONE test per JVM launch.
    // AgentLoader.loadAgentFromClasspath("ebean-agent",
    // "packages=org.avaje;debug=1");
    AgentLoader.loadAgentByMainClass("io.ebean.enhance.Transformer", "packages=org.avaje;debug=1");
    testAfterAgentLoad();
  }

}
