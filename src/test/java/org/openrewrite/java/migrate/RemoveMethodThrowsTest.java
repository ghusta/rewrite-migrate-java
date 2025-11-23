package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveMethodThrowsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveMethodThrows("A foo(..)", "java.io.IOException", true));
    }

    @Test
    @DocumentExample
    void removeSingleException() {
        rewriteRun(
          //language=java
          java(
            """
              import java.io.IOException;

              class A {
                  public void foo() throws IOException {
                      // no-op
                  }
              }
              """
            ,
            """
              class A {
                  public void foo() {
                      // no-op
                  }
              }
              """
          ));
    }

    @Test
    void removeSingleExceptionOverrides() {
        rewriteRun(
          spec -> spec.recipe(new RemoveMethodThrows("Itf foo(..)", "java.io.IOException", true)),
          //language=java
          java(
            """
              import java.io.IOException;

              interface Itf {
                  void foo();
              }

              class A implements Itf {
                  @Override
                  public void foo() throws IOException {
                      // no-op
                  }
              }
              """
            ,
            """
              interface Itf {
                  void foo();
              }

              class A implements Itf {
                  @Override
                  public void foo() {
                      // no-op
                  }
              }
              """
          ));
    }

    @Test
    void removeExceptionWithMultipleExceptions() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  public void foo() throws java.io.IOException, java.lang.IllegalArgumentException {
                      // no-op
                  }
              }
              """
            ,
            """
              class A {
                  public void foo() throws java.lang.IllegalArgumentException {
                      // no-op
                  }
              }
              """
          ));
    }

    @Test
    void noThrows() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  public void foo() {
                      // no-op
                  }
              }
              """
          ));
    }

    @Test
    void noMatchingExceptionInThrows() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  public void foo() throws java.lang.IllegalArgumentException {
                      // no-op
                  }
              }
              """
          ));
    }

}
