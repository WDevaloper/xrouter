package com.github.xrouter.apt;

import com.github.core.RouterService;
import com.github.core.annotation.Service;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
public class RouteServiceProcessor extends AbstractProcessor {
    private String moduleName;
    private String packageName;
    private Elements elementUtils;
    private Filer filer;

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
        Set<String> types = new HashSet<>();
        types.add(Service.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<String, TypeMirror> serviceMap = new HashMap<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(Service.class)) {
            TypeElement typeElement = (TypeElement) element;
            if (packageName == null) {
                packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
            }

            Service service = element.getAnnotation(Service.class);
            String path = service.path();
            serviceMap.put(path, element.asType());
        }

        if (!serviceMap.isEmpty()) {
            generateRouterService(serviceMap);
        }

        return true;
    }

    private void generateRouterService(Map<String, TypeMirror> serviceMap) {
        ClassName routerServiceInterface = ClassName.get(RouterService.class);

        ClassName autoServiceClassName = ClassName.get(AutoService.class);


        ClassName classClass = ClassName.get(Class.class);
        WildcardTypeName wildcard = WildcardTypeName.subtypeOf(Object.class);
        TypeName classWildcardType = ParameterizedTypeName.get(classClass, wildcard);
        ArrayTypeName arrayTypeName = ArrayTypeName.of(classWildcardType);

        ClassName value = ClassName.get(Object.class);
        ArrayTypeName arrayValue = ArrayTypeName.of(value);


        ParameterizedTypeName parameterizedTypeName =
                ParameterizedTypeName.get(ClassName.get(Map.class),
                        ClassName.get(String.class),
                        ClassName.get(Class.class));

        AnnotationSpec autoServiceAnnotationSpec =
                AnnotationSpec.builder(autoServiceClassName)
                        .addMember("value", "$T.class",
                                ClassName.get(RouterService.class))
                        .build();

        FieldSpec fieldSpec =
                FieldSpec.builder(parameterizedTypeName, "services")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .initializer("new $T<>()", HashMap.class)
                        .build();

        MethodSpec constructorSpec = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addCode(initServiceMapCode(serviceMap))
                .build();

        // 定义 RouterServiceImpl 类
        TypeSpec routerServiceImpl =
                TypeSpec.classBuilder("Service_" + moduleName + "$RouterService")
                        .addAnnotation(autoServiceAnnotationSpec)
                        .addModifiers(Modifier.PUBLIC)
                        .addSuperinterface(routerServiceInterface)
                        .addField(fieldSpec)
                        .addMethod(constructorSpec)
                        .addMethod(
                                MethodSpec.methodBuilder("newServiceInstance")
                                        .addModifiers(Modifier.PUBLIC)
                                        .addTypeVariable(TypeVariableName.get("T"))
                                        .returns(TypeVariableName.get("T"))
                                        .addParameter(String.class, "path")
                                        .addParameter(arrayTypeName, "parameterTypes")
                                        .varargs()
                                        .addParameter(arrayValue, "initargs")
                                        .addCode(getServiceCode(new String[]{"parameterTypes", "initargs"}))
                                        .build()
                        )
                        .build();

        try {
            JavaFile.builder(packageName, routerServiceImpl)
                    .build()
                    .writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String initServiceMapCode(Map<String, TypeMirror> serviceMap) {
        StringBuilder code = new StringBuilder();
        for (Map.Entry<String, TypeMirror> entry : serviceMap.entrySet()) {
            code.append("services.put(\"").append(entry.getKey()).append("\", ").append(entry.getValue()).append(".class);\n");
        }
        return code.toString();
    }

    private String getServiceCode(String[] parameterName) {
        return "Class<?> cl = services.get(path);\n" +
                "if (cl != null) {\n" +
                "    try {\n" +
                "        return (T) cl.getConstructor(" + parameterName[0] + ").newInstance(" + parameterName[1] + ");\n" +
                "    } catch (Exception e) {\n" +
                "        e.printStackTrace();\n" +
                "    }\n" +
                "}\n" +
                "return null;";
    }
}