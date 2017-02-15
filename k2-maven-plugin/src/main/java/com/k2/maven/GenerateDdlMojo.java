/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.maven;

import java.util.List;
import java.util.ArrayList;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;

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

import java.io.File;

/** Goal that generates the database ddl from a k2 application.
 *
 * This plugin generates a schema.ddl file from a k2 application. This plugin
 * needs the class name of an instance of Application.
 *
 * run with:
 *
 *   mvn com.github.katari:k2-maven-plugin:generateDdl
 *   -Dapplication=com.k2.hibernate.HibernateTest\$TestApplication
 */
@Mojo(name = "generateDdl", defaultPhase = LifecyclePhase.PROCESS_SOURCES,
   requiresDependencyResolution = ResolutionScope.TEST)
public class GenerateDdlMojo extends AbstractMojo {

  /** The maven project where the plugin is run, never null.
   */
  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  /** The file where this plugin saves the ddl, defaults to target.
   */
  @Parameter(defaultValue = "${project.build.directory}",
      property = "outputDir", required = true)
  private File outputDirectory;

  /** The application class name, never null.
   */
  @Parameter(property = "application", required = true)
  private String applicationClassName;

  /** Runs this mojo.
   *
   * @throws MojoExecutionException in case of error.
   */
  @SuppressWarnings("unchecked")
  public void execute() throws MojoExecutionException {

    List<String> classpathElements = null;
    try {
      classpathElements = project.getTestClasspathElements();
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

      Thread.currentThread().setContextClassLoader(loader);

      Class<?> appClass = loader.loadClass(applicationClassName);
      Constructor<?> constructor = appClass.getConstructor();
      Object application = constructor.newInstance();

      Method run = appClass.getMethod("run", String[].class);
      run.invoke(application, new Object[]{new String[]{}});

      Class<?> hibernateClass = loader.loadClass("com.k2.hibernate.Hibernate");
      Method getBean = appClass.getMethod("getBean", Class.class,
          String.class);
      Object schema = getBean.invoke(application,
          new Object[] {hibernateClass, "schema"});

      Method generate = schema.getClass().getMethod("generate");
      generate.invoke(schema);

    } catch (ClassNotFoundException e) {
      throw new MojoExecutionException(applicationClassName
          + " class not found.", e);
    } catch (DependencyResolutionRequiredException e) {
      throw new MojoExecutionException("Dependency resolution failed", e);
    } catch (Exception e) {
      throw new MojoExecutionException("Cannot instantiate "
          + applicationClassName, e);
    }
  }
}

