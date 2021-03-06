/*
 * Copyright (C) 2017 The Dagger Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.christianbahl.conductor.processor;

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.Optional;

import javax.annotation.processing.Messager;
import javax.inject.Qualifier;
import javax.inject.Scope;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

import dagger.Module;

import static com.google.auto.common.AnnotationMirrors.getAnnotatedAnnotations;
import static com.google.auto.common.AnnotationMirrors.getAnnotationValue;
import static com.google.auto.common.MoreElements.getAnnotationMirror;
import static com.google.auto.common.MoreElements.isAnnotationPresent;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.ABSTRACT;

import com.christianbahl.conductor.ContributesControllerInjector;

/**
 * A descriptor of a generated {@link Module} and {@link dagger.Subcomponent} to be generated from a
 * {@link ContributesControllerInjector} method.
 */
@AutoValue
abstract class AndroidInjectorDescriptor {
    /**
     * The type to be injected; the return type of the {@link ContributesControllerInjector} method.
     */
    abstract ClassName injectedType();

    /**
     * The base injected Conductor type of {@link #injectedType()}. That is {@code com.bluelinelabs.conductor.Controller}.
     */
    abstract ClassName conductorType();

    /**
     * Scopes to apply to the generated {@link dagger.Subcomponent}.
     */
    abstract ImmutableSet<AnnotationSpec> scopes();

    /**
     * @see ContributesControllerInjector#modules()
     */
    abstract ImmutableSet<ClassName> modules();

    /**
     * The {@link Module} that contains the {@link ContributesControllerInjector} method.
     */
    abstract ClassName enclosingModule();

    /**
     * Simple name of the {@link ContributesControllerInjector} method.
     */
    abstract String methodName();

    /**
     * The {@link dagger.MapKey} annotation that groups {@link #conductorType()}s, e.g.
     * {@code @ActivityKey(MyActivity.class)}.
     */
    AnnotationSpec mapKeyAnnotation() {
        return AnnotationSpec.builder(ClassName.get("com.christianbahl.conductor", conductorType().simpleName() + "Key"))
                .addMember("value", "$T.class", injectedType())
                .build();
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder injectedType(ClassName injectedType);

        abstract ImmutableSet.Builder<AnnotationSpec> scopesBuilder();

        abstract ImmutableSet.Builder<ClassName> modulesBuilder();

        abstract Builder conductorType(ClassName conductorType);

        abstract Builder enclosingModule(ClassName enclosingModule);

        abstract Builder methodName(String methodName);

        abstract AndroidInjectorDescriptor build();
    }

    static final class Validator {
        private final Types types;
        private final Elements elements;
        private final Messager messager;

        Validator(Types types, Elements elements, Messager messager) {
            this.types = types;
            this.elements = elements;
            this.messager = messager;
        }

        /**
         * Validates a {@link ContributesControllerInjector} method, returning an {@link
         * AndroidInjectorDescriptor} if it is valid, or {@link Optional#empty()} otherwise.
         */
        Optional<AndroidInjectorDescriptor> createIfValid(ExecutableElement method) {
            ErrorReporter reporter = new ErrorReporter(method, messager);

            if (!method.getModifiers().contains(ABSTRACT)) {
                reporter.reportError("@ContributesControllerInjector methods must be abstract");
            }

            if (!method.getParameters().isEmpty()) {
                reporter.reportError("@ContributesControllerInjector methods cannot have parameters");
            }

            Builder builder = new AutoValue_AndroidInjectorDescriptor.Builder();
            builder.methodName(method.getSimpleName().toString());
            TypeElement enclosingElement = MoreElements.asType(method.getEnclosingElement());
            if (!isAnnotationPresent(enclosingElement, Module.class)) {
                reporter.reportError("@ContributesControllerInjector methods must be in a @Module");
            }
            builder.enclosingModule(ClassName.get(enclosingElement));

            TypeMirror injectedType = method.getReturnType();
            Optional<TypeMirror> maybeConductorType =
                    ConductorMapKeys.annotationsAndConductorTypes(elements)
                            .values()
                            .stream()
                            .filter(conductorType -> types.isAssignable(injectedType, conductorType))
                            .findFirst();
            if (maybeConductorType.isPresent()) {
                builder.conductorType((ClassName) TypeName.get(maybeConductorType.get()));
                if (MoreTypes.asDeclared(injectedType).getTypeArguments().isEmpty()) {
                    builder.injectedType(ClassName.get(MoreTypes.asTypeElement(injectedType)));
                } else {
                    reporter.reportError(
                            "@ContributesControllerInjector methods cannot return parametrized types");
                }
            } else {
                reporter.reportError(String.format("%s is not a sub-class of Controller", injectedType));
            }

            AnnotationMirror annotation =
                    getAnnotationMirror(method, ContributesControllerInjector.class).get();
            for (TypeMirror module :
                    getAnnotationValue(annotation, "modules").accept(new AllTypesVisitor(), null)) {
                if (isAnnotationPresent(MoreTypes.asElement(module), Module.class)) {
                    builder.modulesBuilder().add((ClassName) TypeName.get(module));
                } else {
                    reporter.reportError(String.format("%s is not a @Module", module), annotation);
                }
            }

            for (AnnotationMirror scope : getAnnotatedAnnotations(method, Scope.class)) {
                builder.scopesBuilder().add(AnnotationSpec.get(scope));
            }

            for (AnnotationMirror qualifier : getAnnotatedAnnotations(method, Qualifier.class)) {
                reporter.reportError(
                        "@ContributesControllerInjector methods cannot have qualifiers", qualifier);
            }

            return reporter.hasError ? Optional.empty() : Optional.of(builder.build());
        }

        // TODO(ronshapiro): use ValidationReport once it is moved out of the compiler
        private static class ErrorReporter {
            private final Element subject;
            private final Messager messager;
            private boolean hasError;

            ErrorReporter(Element subject, Messager messager) {
                this.subject = subject;
                this.messager = messager;
            }

            void reportError(String error) {
                hasError = true;
                messager.printMessage(Kind.ERROR, error, subject);
            }

            void reportError(String error, AnnotationMirror annotation) {
                hasError = true;
                messager.printMessage(Kind.ERROR, error, subject, annotation);
            }
        }
    }

    private static final class AllTypesVisitor
            extends SimpleAnnotationValueVisitor8<ImmutableSet<TypeMirror>, Void> {
        @Override
        public ImmutableSet<TypeMirror> visitArray(List<? extends AnnotationValue> values, Void aVoid) {
            return ImmutableSet.copyOf(
                    values.stream().flatMap(v -> v.accept(this, null).stream()).collect(toList()));
        }

        @Override
        public ImmutableSet<TypeMirror> visitType(TypeMirror a, Void aVoid) {
            return ImmutableSet.of(a);
        }

        @Override
        protected ImmutableSet<TypeMirror> defaultAction(Object o, Void aVoid) {
            throw new AssertionError(o);
        }
    }
}