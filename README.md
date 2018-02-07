# K2, a spring based modular application framework.

With K2 it is easy to write maintainable, production-grade, modular spring
based applications. You can write web apps, rest services or simple stand alone
applications.

In K2 there are two main concepts: application and module. An application
simply serves as a module container. Modules are isolated pieces of
functionality that touch other modules and the application through very
specific 'contact surfaces'.

## Tools

We use travis and sonar:

- https://travis-ci.org/katari/k2

- https://sonarqube.com/dashboard?id=com.github.katari%3Ak2

## The main structure

We strongly recommend that you create a simple application launcher that
contains your main operation:

    public class ApplicationLauncher {

      public static void main(final String[] args) {
        new MyApplication().run(args);
      }
    }

Write your application:

    public class MyApplication extends Application {

      public TestApplication() {
        super(new Module1());
      }

      public static void main(final String ... args) {
        Application application = new TestApplication();
        application.run(new String[0]);
      }
    }

and write your modules:

    @Component("testmodule")
    public static class Module1 {

      @Bean public String testBean() {
        return new "Module 1 private bean";
      }
    };

## Using archetypes

K2 provides two archetypes: k2-archetype-application, to create a sample
application with one module, and k2-archetype-module to add more modules to
your application.

This is the command we used to generate the k2-shiro module:

    mvn -B archetype:generate -DarchetypeGroupId=com.github.katari \
      -DarchetypeArtifactId=k2-archetype-module -DarchetypeCatalog=local \
      -DgroupId=com.github.katari -DartifactId=k2-shiro \
      -Dpackage=com.k2.shiro -DclassPrefix=Shiro -Dversion=0.1-SNAPSHOT

## Developing k2

You need java 8 and maven 3.3.

Most modules can be started with mvn spring-boot:run. You can play with, for
example, k2-swagger by:

    cd k2-swagger
    mvn spring-boot:run

and then go to the browser an point it to:

    http://localhost:8081/swagger/

