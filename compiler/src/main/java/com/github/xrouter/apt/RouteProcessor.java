package com.github.xrouter.apt;

import com.github.core.RouteTable;
import com.github.core.annotation.Route;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class RouteProcessor extends AbstractProcessor {

    private Filer filer;
    private Elements elementUtils;
    private String moduleName;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        elementUtils = processingEnv.getElementUtils();
        Map<String, String> options = processingEnv.getOptions();
        moduleName = options.get("moduleName");
        if (moduleName == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Module name not provided in options.");
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new HashSet<>();
        annotations.add(Route.class.getCanonicalName());
        return annotations;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Route.class);
        Map<String, Map<String, ClassName>> groupRouteMap = new HashMap<>();
        String packageName = null;

        for (Element element : elements) {
            TypeElement typeElement = (TypeElement) element;
            if (packageName == null) {
                packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
            }
            Route route = typeElement.getAnnotation(Route.class);
            String path = route.path();
            String group = route.group();
            ClassName className = ClassName.get(typeElement);

            if (!groupRouteMap.containsKey(group)) {
                groupRouteMap.put(group, new HashMap<>());
            }
            groupRouteMap.get(group).put(path, className);
        }

        if (packageName != null && moduleName != null) {
            for (Map.Entry<String, Map<String, ClassName>> groupEntry : groupRouteMap.entrySet()) {
                String group = groupEntry.getKey();
                Map<String, ClassName> groupRoutes = groupEntry.getValue();
                generateRouteClass(groupRoutes, packageName, moduleName, group);
            }
        }
        return true;
    }

    private void generateRouteClass(Map<String, ClassName> routeMap, String packageName, String moduleName, String group) {
        ClassName mapClass = ClassName.get("java.util", "Map");
        ClassName stringClass = ClassName.get("java.lang", "String");
        ClassName hashMapClass = ClassName.get("java.util", "HashMap");
        ClassName classClass = ClassName.get(Class.class);
        ClassName overrideClassAnnotation = ClassName.get(Override.class);
        ClassName routeTableClass = ClassName.get(RouteTable.class);

        WildcardTypeName wildcard = WildcardTypeName.subtypeOf(Object.class);
        TypeName classWildcardType = ParameterizedTypeName.get(classClass, wildcard);
        TypeName mapType = ParameterizedTypeName.get(mapClass, stringClass, classWildcardType);

        MethodSpec.Builder getRouteMapMethod =
                MethodSpec.methodBuilder("getRouteMap")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(overrideClassAnnotation)
                        .returns(mapType);

        getRouteMapMethod.addStatement("$T<$T, $T> routeMap = new $T<>()", mapClass, stringClass, classWildcardType, hashMapClass);
        for (Map.Entry<String, ClassName> entry : routeMap.entrySet()) {
            getRouteMapMethod.addStatement("routeMap.put($S, $T.class)", entry.getKey(), entry.getValue());
        }
        getRouteMapMethod.addStatement("return routeMap");

        ClassName autoServiceClassName = ClassName.get(AutoService.class);
        AnnotationSpec autoServiceAnnotationSpec =
                AnnotationSpec.builder(autoServiceClassName)
                        .addMember("value", "$T.class", routeTableClass)
                        .build();

        TypeSpec routeClass =
                TypeSpec.classBuilder(moduleName + "$RouteTable_" + group)
                        .addAnnotation(autoServiceAnnotationSpec)
                        .addSuperinterface(routeTableClass)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addMethod(getRouteMapMethod.build())
                        .build();

        JavaFile javaFile = JavaFile.builder(packageName, routeClass)
                .build();

        try {
            javaFile.writeTo(filer);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "RouteTable_" + group + ".java generated successfully.");
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate RouteTable_" + group + ".java: " + e.getMessage());
        }
    }
}