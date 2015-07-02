avaje-agentloader
=================
[![Maven Central : avaje-agentloader](https://maven-badges.herokuapp.com/maven-central/org.avaje/avaje-agentloader/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.avaje/avaje-agentloader)

Provides the ability to load java agents to a running java process


Maven Dependency
================
    <dependency>
      <groupId>org.avaje</groupId>
      <artifactId>avaje-agentloader</artifactId>
      <version>2.1.1</version>
    </dependency>
    
Example Usage
=============
<pre>
    public void someApplicationBootupMethod() {
    
      // Load the agent into the running JVM process
      AgentLoader.loadAgentFromClasspath("avaje-ebeanorm-agent","debug=1");
      
    }
</pre>
