package org.avaje.agentloader.test;

import static org.junit.Assert.*;

import org.avaje.agentloader.find.ByClassNameJarFinder;
import org.avaje.agentloader.find.FindResult;
import org.avaje.agentloader.find.JarFinder;
import org.junit.Test;

/**
 * Test for TestByClassNameJarFinder.
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public class TestByClassNameJarFinder {

  JarFinder finder = new ByClassNameJarFinder();
  
  @Test
  public void testResolverSuccess() throws Exception {
    FindResult result = finder.find("io.ebean.enhance.Transformer", getClass().getClassLoader());
    assertNotNull(result);
    assertFalse(result.isTemp());
    assertEquals("ebean-agent-11.34.1.jar", result.getFile().getName());
    
  }
  
  @Test
  public void testResolverFail() throws Exception {
    FindResult result = finder.find("io.ebean.enhance.NonExistent", getClass().getClassLoader());
    assertNull(result);
  }
}
