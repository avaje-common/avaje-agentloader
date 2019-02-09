package org.avaje.agentloader.test;

import static org.junit.Assert.*;

import org.avaje.agentloader.find.ByPrefixJarFinder;
import org.avaje.agentloader.find.FindResult;
import org.avaje.agentloader.find.JarFinder;
import org.junit.Test;

/**
 * Test for ByPrefixJarFinder.
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public class TestByPrefixJarFinder {

  JarFinder finder = new ByPrefixJarFinder();
  
  @Test
  public void testResolverSuccess() throws Exception {
    FindResult result = finder.find("ebean-agent", getClass().getClassLoader());
    assertNotNull(result);
    assertFalse(result.isTemp());
    assertEquals("ebean-agent-11.34.1.jar", result.getFile().getName());
    
  }
  
  @Test
  public void testResolverFail() throws Exception {
    FindResult result = finder.find("does.not.exist", getClass().getClassLoader());
    assertNull(result);
  }
}
