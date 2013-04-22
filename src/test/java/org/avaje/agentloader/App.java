package org.avaje.agentloader;

import java.util.List;

import junit.framework.TestCase;

import org.avaje.agentloader.AgentLoader;

import com.avaje.ebean.BeanState;
import com.avaje.ebean.Ebean;

/**
 * Hello world!
 * 
 */
public class App extends TestCase {
  
  public void testWithEbeanAgent() {
    
    AgentLoader.loadAgentFromClasspath("avaje-ebeanorm-agent","debug=1");
      
    System.out.println("Hello World!");
    
    BaseDomain dom1 = new BaseDomain();
    BeanState beanState = Ebean.getBeanState(dom1);
    System.out.println(beanState);

    List<BaseDomain> list = Ebean.find(BaseDomain.class).findList();
    System.out.println(list);
  }
}
