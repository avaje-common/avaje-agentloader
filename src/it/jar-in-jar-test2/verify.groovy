import java.io.*;
File file1 = new File( basedir, "target/avaje-agentloader-jar-in-jar-test2-" + projectVersion + "-classname.jar" );
File file2 = new File( basedir, "target/avaje-agentloader-jar-in-jar-test2-" + projectVersion + "-prefix.jar" );

assert file1.isFile();
assert file2.isFile();

// check the "uber-jar" file size
assert file1.length() < 50000000;
assert file2.length() < 50000000;


// 
jar1:{
  def command = "java -jar " + file1;
  def process = command.execute();
  process.waitFor();

  def output = process.in.text
  def err = process.err.text
  // and check if the enhancer has kicked in
  assert err.equals("") : (err + output)
  assert output.contains("public java.lang.Object misc.domain.Entity2._ebean_newInstance()")
  assert output.contains("searching for io.ebean.enhance.Transformer")
}

// load by prefix does not work in this combination, because "com.simontuffs.onejar.JarClassLoader"
// of the onejar plugin is no UrlClassLoader, so we cannot get all the jars to scan.

//jar2:{
//  // launch the second jar
//  def command = "java -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005  -jar " + file2;
//  def process = command.execute();
//  process.waitFor();
//
//  def output = process.in.text
//  def err = process.err.text
//  // and check if the enhancer has kicked in
//  assert err.equals("") : (err + output)
//  assert output.contains("public java.lang.Object misc.domain.Entity2._ebean_newInstance()")
//  assert output.contains("searching for ebean-agent")
//}