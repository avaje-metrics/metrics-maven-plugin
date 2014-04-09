package com.avaje.metric.enhance.maven;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.avaje.metric.agent.Transformer;
import org.avaje.metric.agent.offline.OfflineFileTransform;

/**
 * A Maven Plugin that can enhance beans adding timed metric collection.
 * <p>
 * You can use this plugin as part of your build process to enhance beans
 * etc.
 * </p>
 * <p>
 * The parameters are:
 * <ul>
 * <li><b>classSource</b> This is the root directory where the .class files are
 * found. If this is left out then this defaults to
 * ${project.build.outputDirectory}.</li>
 * <li><b>classDestination</b> This is the root directory where the .class files
 * are written to. If this is left out then this defaults to the
 * <b>classSource</b>.</li>
 * <li><b>packages</b> A comma delimited list of packages that is searched for
 * classes that need to be enhanced. If the package ends with ** or * then all
 * subpackages are also searched.</li>
 * <li><b>transformArgs</b> Arguments passed to the transformer. Typically a
 * debug level in the form of debug=1 etc.</li>
 * </ul>
 * </p>
 * 
 * @execute phase="process-classes"
 * @goal enhance
 * @phase process-classes
 */
public class MavenEnhanceTask extends AbstractMojo {

  /**
   * The maven project.
   * 
   * @parameter expression="${project}"
   * @required
   */
  protected MavenProject project;

//  /**
//   * the classpath used to search for e.g. inherited classes
//   * 
//   * @parameter
//   */
//  private String classpath;

  /**
   * Set the directory holding the class files we want to transform.
   * 
   * @parameter default-value="${project.build.outputDirectory}"
   */
  private String classSource;

  /**
   * Set the destination directory where we will put the transformed classes.
   * <p>
   * This is commonly the same as the classSource directory.
   * </p>
   * 
   * @parameter
   */
  String classDestination;

  /**
   * Set the arguments passed to the transformer.
   * 
   * @parameter
   */
  String transformArgs;

  /**
   * Set the package name to search for classes to transform.
   * <p>
   * If the package name ends in "/**" then this recursively transforms all sub
   * packages as well.
   * </p>
   * 
   * @parameter
   */
  String packages;

  public void execute() throws MojoExecutionException {

    final Log log = getLog();
    if (classSource == null) {
      classSource = "target/classes";
    }

    if (classDestination == null) {
      classDestination = classSource;
    }

    File f = new File("");
    log.info("Current Directory: " + f.getAbsolutePath());

//    StringBuilder extraClassPath = new StringBuilder();
//    extraClassPath.append(classSource);
//    if (classpath != null) {
//      if (!extraClassPath.toString().endsWith(";")) {
//        extraClassPath.append(";");
//      }
//      extraClassPath.append(classpath);
//    }

    ClassLoader cl = buildClassLoader();

    Transformer t = new Transformer(transformArgs, cl);
    
    log.info("classSource=" + classSource + "  transformArgs=" + transformArgs + "  classDestination="
        + classDestination + "  packages=" + packages);

    OfflineFileTransform ft = new OfflineFileTransform(t, cl, classSource, classDestination);
    ft.process(packages);
  }
  
  private ClassLoader buildClassLoader() {
    
    URL[] urls = buildClassPath();

    return URLClassLoader.newInstance(urls, Thread.currentThread().getContextClassLoader());
  }

  @SuppressWarnings({ "unchecked" })
  private URL[] buildClassPath() {
    try {
      List<URL> urls = new ArrayList<URL>();
      
      URL projectOut = new File(project.getBuild().getOutputDirectory()).toURI().toURL();
      urls.add(projectOut);

      Set<Artifact> artifacts = (Set<Artifact>) project.getArtifacts();
      for (Artifact a : artifacts) {
        urls.add(a.getFile().toURI().toURL());
      }
      
      getLog().debug("ClassPath URLs: "+urls);
      
      return urls.toArray(new URL[urls.size()]);
      
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}