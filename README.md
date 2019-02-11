avaje-agentloader
=================
[![Maven Central : avaje-agentloader](https://maven-badges.herokuapp.com/maven-central/org.avaje/avaje-agentloader/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.avaje/avaje-agentloader)

Provides the ability to load java agents to a running java process


Maven Dependency
================
    <dependency>
      <groupId>org.avaje</groupId>
      <artifactId>avaje-agentloader</artifactId>
      <version>3.0.1</version>
    </dependency>
    
Example Usage
=============
<pre>
    public void someApplicationBootupMethod() {
    
      // Load the agent into the running JVM process
      
      // preferred method:
      AgentLoader.loadAgentByMainClass("io.ebean.enhance.Transformer", "debug=1");
      
      // No longer recommended (does not work reliable on jar-in-jar)
      AgentLoader.loadAgentFromClasspath("ebean-agent","debug=1");
      
    }
</pre>
