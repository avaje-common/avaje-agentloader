package org.avaje.agentloader.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import io.ebean.bean.EntityBean;

/**
 * Basic test, to check if the ebean-agent could be loaded.
 * (we just test the enhancement here, so no ebean server is started)
 * 
 * IMPORTANT:
 * we must not import the entities, because they might be loaded before the enhancer.
 */
public abstract class AgentLoadBaseTest {

   /**
   * The agent is not loaded, this is the first access to Entity1, so it shouldn't
   * get enhanced.
   */
  public void testBeforeAgentLoad() throws Exception {
    Object e1 = Class.forName("misc.domain.Entity1").newInstance();
    assertFalse(e1 instanceof EntityBean);
  }

  /**
   * Now, the agent is loaded. While Entity1 is still not enhanced (because it is
   * cached), it should enhance Entity2, because it is the first access.
   */
  public void testAfterAgentLoad() throws Exception {
    Object e1 = Class.forName("misc.domain.Entity1").newInstance();
    assertFalse(e1 instanceof EntityBean);

    Object e2 = Class.forName("misc.domain.Entity2").newInstance();
    assertTrue(e2 instanceof EntityBean);
    
    System.out.println("=== Methods of not enhanced class ===");
    for (Method m : e1.getClass().getDeclaredMethods()) {
      System.out.println(m);
    }
    
    System.out.println("=== Methods of enhanced class ===");
    for (Method m : e2.getClass().getDeclaredMethods()) {
      System.out.println(m);
    }
  }
}
