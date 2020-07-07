package young.httpd.processor;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

import young.httpd.annotation.PathVariable;
import young.httpd.annotation.RequestHeader;
import young.httpd.annotation.RequestMapping;
import young.httpd.annotation.RequestParam;
import young.httpd.exception.NotFoundException;
import young.httpd.handler.HttpdHandler;


@AutoService(Processor.class)
public class HttpdProcessor extends AbstractProcessor {

    private final static String CLASS_NAME = "GeneratedHttpdHandlerImpl";
    private final static String PACKAGE_NAME = "young.httpd";
    private final static String METHOD_DISPOSE_REQUEST = "disposeRequest";
    private final static String VOID = "void";
    private final static List<Integer> HASHS = new ArrayList<>();
    private MethodSpec.Builder mDisposeRequest;
    private MethodSpec.Builder mConstructor;
    private TypeSpec.Builder mTypeSpec;
    private int mUrlMappingCount;


    private static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mDisposeRequest = MethodSpec.methodBuilder(METHOD_DISPOSE_REQUEST)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(String.class, "url")
                .addParameter(ParameterizedTypeName.get(Map.class, String.class, String.class), "params")
                .addParameter(ParameterizedTypeName.get(Map.class, String.class, String.class), "files")
                .addParameter(ParameterizedTypeName.get(Map.class, String.class, String.class), "paths")
                .addParameter(ParameterizedTypeName.get(Map.class, String.class, String.class), "headers")
                .beginControlFlow("switch (hash(url))")
                .returns(Object.class);
        mConstructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        mTypeSpec = TypeSpec.classBuilder(CLASS_NAME)
                .addModifiers(Modifier.FINAL)
                .superclass(HttpdHandler.class)
                .addJavadoc("Generated code, do not modify.")
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "{$S, $S}", "deprecation", "unchecked").build());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(RequestMapping.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            return processImpl(annotations, roundEnv);
        } catch (Exception e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            fatalError(writer.toString());
            return true;
        }
    }

    private boolean processImpl(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) throws IOException {
        processRequestMapping(annotations, roundEnv);
        if (annotations.size() > 0) {
            mTypeSpec.addMethod(mConstructor.build());
            mDisposeRequest.endControlFlow();
            mDisposeRequest.addStatement("throw new $T()", NotFoundException.class);
            mTypeSpec.addMethod(mDisposeRequest.build());
            JavaFile.builder(PACKAGE_NAME, mTypeSpec.build()).build().writeTo(processingEnv.getFiler());
        }
        return true;
    }

    private void processRequestMapping(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(RequestMapping.class);
        for (Element element : elements) {
            try {
                switch (element.getKind()) {
                    case METHOD:
                        handleMethod(element);
                        break;
                    case CLASS:
                        handleClass(element);
                        break;
                }
            } catch (Exception e) {
                continue;
            }
        }
    }

    private void handleClass(Element element) throws Exception {
        if (element.getKind() != ElementKind.CLASS) throw new Exception();
        TypeElement typeElement = (TypeElement) element;
        String var = generateVariable(typeElement.getQualifiedName().toString());
        TypeName typeName = TypeName.get(typeElement.asType());
        mTypeSpec.addField(typeName, var, Modifier.PRIVATE, Modifier.FINAL);
        mConstructor.addStatement(var + " = new $T()", typeName);
    }

    private void handleMethod(Element element) throws Exception {
        if (element.getKind() != ElementKind.METHOD) throw new Exception();
        ExecutableElement executableElement = (ExecutableElement) element;
        if (!executableElement.getModifiers().contains(Modifier.PUBLIC)) throw new Exception();
        TypeElement typeElement = (TypeElement) executableElement.getEnclosingElement();
        RequestMapping executableMapping = executableElement.getAnnotation(RequestMapping.class);
        RequestMapping typeMapping = typeElement.getAnnotation(RequestMapping.class);
        StringBuilder builder = new StringBuilder();
        builder.append("/").append(typeMapping.value()).append("/").append(executableMapping.value());

        List<? extends VariableElement> parameters = executableElement.getParameters();
        StringBuilder sb = new StringBuilder();
        List<Object> list = new ArrayList<>();
        String r = executableElement.getReturnType().toString().toLowerCase();
        if (!VOID.equals(r)) {
            sb.append("case $N: return $N.$L(");
        } else {
            sb.append("case $N: $N.$L(");
        }

        for (int i = 0; i < parameters.size(); i++) {
            if (i != 0) sb.append(",");
            VariableElement variableElement = parameters.get(i);

            ClassName className = null;
            TypeName typeName = null;
            try {
                className = ClassName.bestGuess(variableElement.asType().toString().replaceAll("<.+>", ""));
            } catch (Exception e) {
                typeName = ClassName.get(variableElement.asType());
            }

            RequestParam requestParam = variableElement.getAnnotation(RequestParam.class);
            if (requestParam != null) {
                sb.append("getRequestParam($S, params, files, $T.class)");
                list.add(requestParam.value());
                list.add(className == null ? typeName : className);
                continue;
            }

            PathVariable pathVariable = variableElement.getAnnotation(PathVariable.class);
            if (pathVariable != null) {
                sb.append("getPathVariable($S, paths, $T.class)");
                list.add(pathVariable.value());
                list.add(className == null ? typeName : className);
                continue;
            }

            RequestHeader requestHeader = variableElement.getAnnotation(RequestHeader.class);
            if (requestHeader != null) {
                sb.append("getRequestHeader($S, headers, $T.class)");
                list.add(requestHeader.value());
                list.add(className == null ? typeName : className);
                continue;
            }

            sb.append("getInject($T.class)");
            list.add(className == null ? typeName : className);
        }
        if (VOID.equals(r)) {
            sb.append("); return null");
        } else {
            sb.append(")");
        }


        String url = builder.toString().replaceAll("/+", "/").replaceAll("^/|/$", "");
        int hashCode = hash(url);
        if (HASHS.contains(hashCode)) {
            fatalError(" mapping: " + url + " repetitive!");
        }
        HASHS.add(hashCode);
        String name = getUrlVarName(url);
        mTypeSpec.addField(
                FieldSpec.builder(int.class, name, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$L", hashCode).build()
        );
        mConstructor.addStatement("registerUrl($S)", url);


        String classVar = generateVariable(typeElement.getQualifiedName().toString());
        String methodName = executableElement.getSimpleName().toString();
        list.add(0, methodName);
        list.add(0, classVar);
        list.add(0, name);

        mDisposeRequest.addStatement(sb.toString(), list.toArray(new Object[]{}));
    }

    private String getUrlVarName(String url) {
        return "URL_MAPPING_" + (mUrlMappingCount++);
    }


    private String generateVariable(String str) {
        if (str == null || str.length() <= 0) {
            return "";
        }
        return str.replaceAll("\\.|/", "_").toLowerCase();
    }

    private void log(String msg) {
        if (processingEnv.getOptions().containsKey("debug"))
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
    }

    private void fatalError(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "FATAL ERROR: " + msg);
    }
}
