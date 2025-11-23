package org.openrewrite.java.migrate;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.DeclaresMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.NameTree;

import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveMethodThrows extends Recipe {

    @Option(displayName = "Method pattern",
            example = "com.example.MyClass myMethod(..)")
    String methodPattern;

    @Option(displayName = "Exception type",
            description = "Fully qualified name of the exception to remove (e.g. `java.io.IOException`).",
            example = "java.io.IOException")
    String exceptionType;

    @Option(displayName = "Match overriden methods",
            description = "Whether to match overridden forms of the method on subclasses of typeMatcher. Default is true.",
            required = false)
    @Nullable
    Boolean matchOverrides;

    @Override
    public String getDisplayName() {
        return "Remove a specific exception from a method's throws clause";
    }

    @Override
    public String getDescription() {
        return "Remove a specific exception from method throws declarations.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(MethodMatcher.validate(methodPattern));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        boolean varMatchOverrides = matchOverrides != null ? matchOverrides : true;
        MethodMatcher methodMatcher = new MethodMatcher(methodPattern, varMatchOverrides);
        return Preconditions.check(new DeclaresMethod<>(methodMatcher), new JavaIsoVisitor<ExecutionContext>() {

                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                        J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                        J.ClassDeclaration cd = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);

                        if (methodMatcher.matches(m, cd)) {

                            if (m.getThrows() == null) {
                                return m; // no throws to modify
                            }

                            List<NameTree> updatedThrows = m.getThrows().stream()
                                    .filter(t -> {
                                        // Keep only exception types that are not the target
                                        String fqn = t.getType() != null ? t.getType().toString() : null;
                                        return fqn == null || !fqn.equals(exceptionType);
                                    })
                                    .collect(Collectors.toList());

                            maybeRemoveImport(exceptionType);

                            if (updatedThrows.isEmpty()) {
                                // Remove the entire throws clause
                                return m.withThrows(null);
                            }

                            // Replace with filtered throws list
                            return m.withThrows(updatedThrows);
                        }
                        return m;
                    }

                }
        );
    }
}
