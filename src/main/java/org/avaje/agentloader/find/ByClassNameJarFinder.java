package org.avaje.agentloader.find;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Searches a jar file by a class name, that is contained in the jar. (e.g. the
 * main class of an agent)
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public class ByClassNameJarFinder extends JarFinder {

  private static final Logger log = LoggerFactory.getLogger(ByClassNameJarFinder.class);

  private URL stripUrl(URL url, String classFile) throws MalformedURLException {
    if (url == null) {
      return null;
    }
    String tmp = url.toString();
    if (!tmp.endsWith(classFile)) {
      return null;
    }
    tmp = tmp.substring(0, tmp.length() - classFile.length()); // remove

    if (tmp.startsWith("jar:")) {
      if (tmp.endsWith("!/")) { // cut away the last "!/"
        tmp = tmp.substring(0, tmp.length() - 2);
      }
      if (tmp.contains("!/")) {
        // path contains additional !/ -> this means jar in jar
      } else if (tmp.startsWith("jar:file:")) {
        tmp = tmp.substring(4); // else this means resource in jar
      }
    }
    return new URL(url, tmp, null);
  }

  @Override
  protected List<URL> findCandidates(final String query, ClassLoader cl) {
    String classFile = query.replace('.', '/') + ".class";
    List<URL> candidates = new ArrayList<>();
    try {
      // we use only the first candidate here (className should be unique)
      URL url = cl.getResource(classFile);
      url = stripUrl(url, classFile);
      if (url != null) {
        candidates.add(url);
      } else {
        try {
          // we did not find the jar, so try to resolve via codeSource
          Class<?> clazz = cl.loadClass(query);
          ProtectionDomain pd = clazz.getProtectionDomain();
          CodeSource cs = pd == null ? null : pd.getCodeSource();
          if (cs != null) {
            candidates.add(cs.getLocation());
          }
        } catch (ClassNotFoundException cnf) {
          // NOP
        } catch (Exception e) {
          log.warn("Cannot find code-source for {}", query, e);
        }
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return candidates;
  }
}
