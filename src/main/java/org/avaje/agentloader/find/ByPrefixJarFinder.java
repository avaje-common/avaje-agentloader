package org.avaje.agentloader.find;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds a jar by it's prefix. E.g. "ebean-agent" to find jar with name
 * "ebean-agent-x.x.x.jar"
 * 
 * Searches all jars on classpath and returns the full path. 
 * 
 * <b>NOTE:</b> This may not work on all VMs, better use ByClassNameJarFinder.
 *
 * @author Roland Praml, FOCONIS AG
 */
public class ByPrefixJarFinder extends JarFinder {

  private static final Logger log = LoggerFactory.getLogger(ByPrefixJarFinder.class);

  /**
   * Return the classpathurls where to search for jars.
   */
  private Set<URL> getClasspathUrls(ClassLoader cl) {
    Set<URL> ret = new LinkedHashSet<>();
    if (cl instanceof URLClassLoader) {
      for (URL url : ((URLClassLoader) cl).getURLs()) {
        ret.add(url);
      }
    }
    // java 9+ fallback. cl is an instance of
    // jdk.internal.loader.ClassLoaders$AppClassLoader
    // It is not easy to get the URLs from the classloader (only with
    // reflection/unsafe) See https://stackoverflow.com/questions/46519092
    // unfortunately, this means also, we have to drop embedded support
    String[] cpFiles = System.getProperty("java.class.path").split(File.pathSeparator);
    try {
      for (int i = 0; i < cpFiles.length; i++) {
        ret.add(new File(cpFiles[i]).toURI().toURL());
      }
    } catch (MalformedURLException mfe) {
      log.error("Cannot determine classpathUrls", mfe);
    }
    return ret;
  }

  @Override
  protected List<URL> findCandidates(String query, ClassLoader cl) {
    List<URL> candidates = new ArrayList<>();
    Set<URL> urls = getClasspathUrls(cl);
    for (URL url : urls) {
      if (isMatch(url, query)) {
        if (!candidates.contains(url)) { // avoid duplicates
          candidates.add(url);
        }
      }
    }
    if (candidates.isEmpty()) {
      if (log.isTraceEnabled()) {
        log.debug("Urls in classpath:");
        for (URL url : urls) {
          log.trace("  url: {}", url);
        }
      }
    }
    return candidates;
  }

}
