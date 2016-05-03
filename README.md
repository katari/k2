# K2, a spring based modular application framework.

With K2 it is easy to write maintainable, production-grade, modular spring
based applications. You can write web apps, rest services or simple stand alone
applications.

In K2 there are two main concepts: application and module. An application
simply serves as a module container. Modules are isolated pieces of
functionality that touch other modules and the application through very
specific 'contact surfaces'.

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

