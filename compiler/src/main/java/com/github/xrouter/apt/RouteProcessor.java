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

/**
 * 路由注解处理器，用于处理 @Route 注解并生成路由表类
 */
@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class RouteProcessor extends AbstractProcessor {

    // 文件写入器，用于生成 Java 文件
    private Filer filer;
    // 元素工具类，用于获取元素的相关信息，如包名
    private Elements elementUtils;
    // 模块名称
    private String moduleName;

    /**
     * 初始化处理器
     * @param processingEnv 处理环境，包含工具类和配置信息
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        // 获取文件写入器
        filer = processingEnv.getFiler();
        // 获取元素工具类
        elementUtils = processingEnv.getElementUtils();

        // 从 options 中获取模块名称
        Map<String, String> options = processingEnv.getOptions();
        moduleName = options.get("moduleName");
        if (moduleName == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Module name not provided in options.");
        }
    }

    /**
     * 声明要处理的注解类型
     * @return 注解类型的全限定名集合
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new HashSet<>();
        annotations.add(Route.class.getCanonicalName());
        return annotations;
    }

    /**
     * 处理注解的核心方法
     * @param annotations 注解类型集合
     * @param roundEnv 本轮处理环境，包含被注解的元素信息
     * @return 是否处理成功
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 如果没有注解，直接返回 false
        if (annotations.isEmpty()) {
            return false;
        }
        // 获取所有被 @Route 注解标注的元素
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Route.class);
        // 存储路由信息的映射，键为路由路径，值为对应的类名
        Map<String, ClassName> routeMap = new HashMap<>();
        // 存储被注解类的包名
        String packageName = null;

        // 遍历所有被注解的元素
        for (Element element : elements) {
            // 将元素转换为类型元素
            TypeElement typeElement = (TypeElement) element;
            // 如果包名还未获取，获取当前类的包名
            if (packageName == null) {
                packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
            }
            // 获取元素上的 @Route 注解
            Route route = typeElement.getAnnotation(Route.class);
            // 获取路由路径
            String path = route.path();
            // 获取类名
            ClassName className = ClassName.get(typeElement);
            // 将路由信息存入映射
            routeMap.put(path, className);
        }

        // 如果获取到了包名和模块名称，生成路由表类
        if (packageName != null && moduleName != null) {
            generateRouteClass(routeMap, packageName, moduleName);
        }
        return true;
    }

    /**
     * 生成路由表类
     * @param routeMap 路由信息映射
     * @param packageName 包名
     */
    private void generateRouteClass(Map<String, ClassName> routeMap, String packageName, String moduleName) {
        // 获取 java.util.Map 类名
        ClassName mapClass = ClassName.get("java.util", "Map");
        // 获取 java.lang.String 类名
        ClassName stringClass = ClassName.get("java.lang", "String");
        // 获取 java.util.HashMap 类名
        ClassName hashMapClass = ClassName.get("java.util", "HashMap");
        // 获取 java.lang.Class 类名
        ClassName classClass = ClassName.get(Class.class);
        ClassName overrideClassAnnotation = ClassName.get(Override.class);
        ClassName autoServiceAnnotation = ClassName.get(AutoService.class);
        ClassName routeTableClass = ClassName.get(RouteTable.class);

        // 使用 WildcardTypeName 表示 Class<?>
        WildcardTypeName wildcard = WildcardTypeName.subtypeOf(Object.class);
        // 生成 Class<?> 类型
        TypeName classWildcardType = ParameterizedTypeName.get(classClass, wildcard);
        // 生成 Map<String, Class<?>> 类型
        TypeName mapType = ParameterizedTypeName.get(mapClass, stringClass, classWildcardType);

        // 构建 getRouteMap 方法
        MethodSpec.Builder getRouteMapMethod =
                MethodSpec.methodBuilder("getRouteMap")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(overrideClassAnnotation)
                        .returns(mapType);

        // 添加创建 Map 的语句
        getRouteMapMethod.addStatement("$T<$T, $T> routeMap = new $T<>()", mapClass, stringClass, classWildcardType, hashMapClass);
        // 遍历路由信息映射，添加 put 语句
        for (Map.Entry<String, ClassName> entry : routeMap.entrySet()) {
            getRouteMapMethod.addStatement("routeMap.put($S, $T.class)", entry.getKey(), entry.getValue());
        }
        // 添加返回语句
        getRouteMapMethod.addStatement("return routeMap");


        // 获取 AutoService 注解类的 ClassName
        ClassName autoServiceClassName = ClassName.get(AutoService.class);
        // 获取 RouterTable 接口类的 ClassName, 构建 AutoService 注解
        AnnotationSpec autoServiceAnnotationSpec =
                AnnotationSpec.builder(autoServiceClassName)
                        .addMember("value", "$T.class", routeTableClass)
                        .build();


        // 构建 RouteTable 类
        TypeSpec routeClass =
                TypeSpec.classBuilder(moduleName + "$RouteTable")
                        .addAnnotation(autoServiceAnnotationSpec)
                        .addSuperinterface(routeTableClass)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addMethod(getRouteMapMethod.build())
                        .build();

        // 构建 Java 文件
        JavaFile javaFile = JavaFile.builder(packageName, routeClass)
                .build();

        try {
            // 将 Java 文件写入磁盘
            javaFile.writeTo(filer);
            // 打印生成成功的日志
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "RouteTable.java generated successfully.");
        } catch (IOException e) {
            // 打印生成失败的错误日志
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate RouteTable.java: " + e.getMessage());
        }
    }
}