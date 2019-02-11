package org.avaje.agentloader.find;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation to find jar files and extract them, if neccessary.
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public abstract class JarFinder {

  private static final Logger log = LoggerFactory.getLogger(JarFinder.class);

  /**
   * Buffer size used when extracting the agent (when it is embedded).
   */
  private static final int IO_BUFFER_SIZE = 1024 * 4;

  /**
   * Tries to find the jar, that matches the query.
   */
  public FindResult find(String query, ClassLoader cl) {
    log.debug("searching for {}", query);
    List<URL> candidates = findCandidates(query, cl);
    if (candidates.isEmpty()) {
      log.debug("Could not find {}.", query);
      return null;
    } else if (candidates.size() > 1) {
      log.warn("Query for {} is ambiguous. Using first candidate of {}", query, candidates);
    }
    try {
      URL url = candidates.get(0);
      // We have found the agent jar in the classpath
      FindResult jar = null;
      if (isJarInJar(url)) {
        // extract the agent jar into a tmp directory for use
        jar = extractJar(url, query);
      } else {
        jar = new FindResult(new File(url.toURI()), false);
      }
      return jar;
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Finds potential candiadtes matching on that query.
   */
  protected abstract List<URL> findCandidates(String query, ClassLoader cl);

  /**
   * Return true if the agent jar is embedded. In this case we extract it out into
   * a tmp directory.
   */
  protected boolean isJarInJar(URL url) {
    return url.getProtocol().equals("jar") && url.getPath().contains("!/");
  }

  /**
   * Check to see if this url/jar matches our agent name.
   */
  protected boolean isMatch(URL url, String partial) {
    log.trace("isMatch('{}','{}')", url, partial);
    String fullPath = url.getFile();
    int lastJar = fullPath.lastIndexOf(".jar");
    if (lastJar < 0) {
      return false;
    }
    int lastSlash = fullPath.lastIndexOf('/', lastJar);
    if (lastSlash < 0) {
      return false;
    }

    String jarName = fullPath.substring(lastSlash + 1, lastJar + 4); // only use the last part of the URL
    return jarName.startsWith(partial);
  }

  /**
   * This method will extract agent JAR file from URL path. Due to the package
   * implementation, this method will cover two cases as below: Embedded Class
   * Files: jar:file:path-to-filename.war!/WEB-INF/jar/jar-file/ Embedded Jar
   * Files: jar:file:path-to-filename.war!/WEB-INF/jar/jar-file!/
   *
   * @param path      is full url entry in the classpath
   * @param agentName is the agent name that we are trying to match
   * @return null if it fails or a full path to the jar file if it succeeds
   */
  protected FindResult extractJar(URL path, String agentName) {
    File fullPath = null;
    log.debug("Extracting agent {} from source {}", agentName, path.getPath());

    try {
      fullPath = File.createTempFile(agentName, ".jar");
      try (InputStream is = path.openStream();
          OutputStream os = new BufferedOutputStream(new FileOutputStream(fullPath))) {
        copyBytes(is, os);
        log.debug("Extracted jar to {}", fullPath);
      }
    } catch (Exception ex) {
      log.error("Failed to extract jar {}", path, ex);
    }

    return fullPath == null ? null : new FindResult(fullPath, true);
  }

  /**
   * Copy the bytes from input to output streams (using a 4K buffer).
   */
  protected long copyBytes(InputStream input, OutputStream output) throws IOException {
    byte[] buffer = new byte[IO_BUFFER_SIZE];

    long count = 0;
    int n;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }
}
