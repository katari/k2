/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.maven;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ArrayList;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;

import java.io.File;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.DependencyResolutionRequiredException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/** Goal that generates the database ddl from a k2 application.
 *
 * This plugin generates a schema.ddl file from a k2 application. This plugin
 * needs the class name of an instance of Application.
 *
 * run with:
 *
 *   mvn com.github.katari:k2-maven-plugin:generateDdl
 *   -Dk2.applicationClassName=com.k2.hibernate.HibernateTest\$TestApplication
 */
@Mojo(name = "generateDdl", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
   requiresDependencyResolution = ResolutionScope.TEST)
public class GenerateDdlMojo extends AbstractMojo {

  /** The maven project where the plugin is run, never null.
   */
  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  /** The file where this plugin saves the ddl, defaults to target.
   */
  @Parameter(defaultValue = "${project.build.directory}",
      property = "outputDirectory", required = true)
  private File outputDirectory;

  /** The application class name, never null.
   */
  @Parameter(property = "k2.applicationClassName", required = true)
  private String applicationClassName;

  /** A list of system properties to be passed.
   *
   * Note: as the execution is not forked, some system properties required by
   * the JVM cannot be passed here. Use MAVEN_OPTS instead.
   */
  @Parameter private Map<String, String> systemProperties;

  /** Runs this mojo.
   *
   * @throws MojoExecutionException in case of error.
   */
  public void execute() throws MojoExecutionException {

    Properties originalSystemProperties = null;
    ClassLoader oldClassLoader = null;
    try {
      originalSystemProperties = setSystemProperties();

      oldClassLoader = configureClassLoader();

      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      Class<?> appClass = loader.loadClass(applicationClassName);
      Constructor<?> constructor = appClass.getConstructor();
      Object application = constructor.newInstance();

      Method run = appClass.getMethod("run", String[].class);
      run.invoke(application, new Object[]{new String[]{}});

      Class<?> hibernateClass = loader.loadClass("com.k2.hibernate.Hibernate");
      Method getBean = appClass.getMethod("getBean", Class.class,
          String.class, Class.class);
      Object schema = getBean.invoke(application,
          new Object[] {hibernateClass, "schema", Object.class});

      Method generate = schema.getClass().getMethod("generate", String.class);
      generate.invoke(schema, outputDirectory.getAbsolutePath()
          + "/schema.ddl");
    } catch (MojoExecutionException e) {
      throw e;
    } catch (ClassNotFoundException e) {
      throw new MojoExecutionException(applicationClassName
          + " class not found.", e);
    } catch (Exception e) {
      throw new MojoExecutionException("Cannot instantiate "
          + applicationClassName, e);
    } finally {
      if (originalSystemProperties != null) {
        System.setProperties(originalSystemProperties);
      }
      Thread.currentThread().setContextClassLoader(oldClassLoader);
    }
  }

  /** Creates a class loader that includes the project test classpath and
   * configures it as the current thread context class loader.
   *
   * @return the old class loader. May be null if none was configured.
   *
   * @throws MojoExecutionException in case of error.
   */
  @SuppressWarnings("unchecked")
  private ClassLoader configureClassLoader() throws MojoExecutionException {
    List<String> classpathElements = null;
    try {
      classpathElements = project.getTestClasspathElements();
    } catch (DependencyResolutionRequiredException e) {
      throw new MojoExecutionException("Dependency resolution failed", e);
    }
    List<URL> projectClasspathList = new ArrayList<URL>();
    for (String element : classpathElements) {
      try {
        projectClasspathList.add(new File(element).toURI().toURL());
      } catch (MalformedURLException e) {
        throw new MojoExecutionException(element
            + " is an invalid classpath element", e);
      }
    }

    URLClassLoader loader;
    loader = new URLClassLoader(projectClasspathList.toArray(new URL[0]));

    ClassLoader old = Thread.currentThread().getContextClassLoader();

    Thread.currentThread().setContextClassLoader(loader);
    return old;
  }

  /** Pass any given system properties to the java system properties.
   *
   * @return the original system properties, never null.
   */
  private Properties setSystemProperties() {
    Properties properties = null;
    if (systemProperties != null) {
      properties = System.getProperties();
      for (Map.Entry<String, String> systemProperty
          : systemProperties.entrySet()) {
        System.setProperty(systemProperty.getKey(), systemProperty.getValue());
      }
    }
    return properties;
  }
}

