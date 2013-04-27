avaje-agentloader
=================

Provides the ability to load java agents to a running java process

Maven dependency
----------------

    <dependency>
      <groupId>org.avaje</groupId>
      <artifactId>avaje-agentloader</artifactId>
      <version>[1.1.1]</version>
    </dependency>
   
    
Example usage 
-------------
<pre>
  public void someBootupMethod() {
  
   // Load the avaje-ebeanorm-agent into the running process
   // ... assumes avaje-ebeanorm-agent jar is in the classpath
   
   AgentLoader.loadAgentFromClasspath("avaje-ebeanorm-agent","debug=1");
   ...
   
  }
</pre>  
