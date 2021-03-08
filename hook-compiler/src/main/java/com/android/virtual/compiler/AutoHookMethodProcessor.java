package com.android.virtual.compiler;

import com.android.virtual.client.hook.annotation.AutoHookMethod;
import com.android.virtual.util.Consts;
import com.android.virtual.util.Logger;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static javax.lang.model.element.Modifier.PUBLIC;

@AutoService(Processor.class)
@SupportedOptions(Consts.KEY_MODULE_NAME)
public class AutoHookMethodProcessor extends AbstractProcessor {

    /**
     * 文件相关的辅助类
     */
    private Filer mFiler;
    private Types mTypes;
    private Elements mElements;
    /**
     * 日志相关的辅助类
     */
    private Logger mLogger;

    /**
     * Module name, maybe its 'app' or others
     */
    private String moduleName = null;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        mFiler = processingEnv.getFiler();
        mTypes = processingEnv.getTypeUtils();
        mElements = processingEnv.getElementUtils();
        mLogger = new Logger(processingEnv.getMessager());

        // Attempt to get user configuration [moduleName]
        Map<String, String> options = processingEnv.getOptions();
        if (MapUtils.isNotEmpty(options)) {
            moduleName = options.get(Consts.KEY_MODULE_NAME);
        }

        if (StringUtils.isNotEmpty(moduleName)) {
            moduleName = moduleName.replaceAll("[^0-9a-zA-Z_]+", "");

            mLogger.info("The user has configuration the module name, it was [" + moduleName + "]");
        } else {
            mLogger.info("These no module name, at 'build.gradle', like :\n" +
                    "javaCompileOptions {\n" +
                    "    annotationProcessorOptions {\n" +
                    "        arguments = [ moduleName : project.getName() ]\n" +
                    "    }\n" +
                    "}\n");
            //默认是app
            moduleName = "app";
//            throw new RuntimeException("XPage::Compiler >>> No module name, for more information, look at gradle log.");
        }
        mLogger.info(">>> AutoHookMethodProcessor init. <<<");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        if (CollectionUtils.isNotEmpty(annotations)) {
            Set<? extends Element> AutoHookMethodElements = roundEnvironment.getElementsAnnotatedWith(AutoHookMethod.class);
            try {
                mLogger.info(">>> Found AutoHook, start... <<<");
                parseAutoHookMethod(AutoHookMethodElements);
            } catch (Exception e) {
                mLogger.error(e);
            }
            return true;
        }
        return false;
    }

    /**
     * @return 指定哪些注解应该被注解处理器注册
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(AutoHookMethod.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private void parseAutoHookMethod(Set<? extends Element> autoHookMethodElements) throws IOException, IllegalAccessException {

        TypeElement type_ISyringe = mElements.getTypeElement(Consts.ISYRINGE);
        TypeElement hook_MethodBackup = mElements.getTypeElement(Consts.METHODBACKUP);
        TypeElement hook_MethodParams = mElements.getTypeElement(Consts.METHODPARAMS);
        TypeElement hook_Method = mElements.getTypeElement(Consts.METHOD);
        TypeElement hook_Class = mElements.getTypeElement(Consts.CLASS);
        TypeElement hook_AutoThisObject = mElements.getTypeElement(Consts.AUTOTHISOBJECT);
        TypeElement hook_ThisObject = mElements.getTypeElement(Consts.THISOBJECT);
        TypeElement hook_AutoHookBridge = mElements.getTypeElement(Consts.HOOKBRIDGE);
        Map<TypeElement, Map<TypeElement, List<Element>>> parentAndChild = new HashMap<>();
        TypeElement ClassTypeElement;
        TypeMirror MethodTypeMirror;
        Name MethodName;
        for (Element element : autoHookMethodElements) {

            MethodName = element.getSimpleName();
            if(MethodName == null)
                continue;

            MethodTypeMirror = element.asType();
            if(MethodTypeMirror == null)
                continue;

            ClassTypeElement =  (TypeElement) element.getEnclosingElement();
            if(ClassTypeElement == null)
                continue;

            AutoHookMethod autoHookMethod = element.getAnnotation(AutoHookMethod.class);
            if(autoHookMethod == null)
                continue;

            TypeElement targetClassTypeElement = null;
            try {
                autoHookMethod.targetClass();
            } catch (MirroredTypeException mte){
                DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
                targetClassTypeElement = (TypeElement) classTypeMirror.asElement();
            }
            if(targetClassTypeElement == null)
                continue;
            
            Set<Modifier> modifiers = element.getModifiers();
            if(modifiers == null || modifiers.isEmpty())
                continue;

            mLogger.info(">>> AutoHookElement " + ClassTypeElement + "." + element + " <<<");
            if (!element.getModifiers().contains(PUBLIC))
                throw new IllegalAccessException("The AutoHook Method can only 'public'!!! please check Method ["
                        + element.getSimpleName() + "] in class [" + ClassTypeElement.getQualifiedName() + "]");

            ExecutableElement executableElement = (ExecutableElement) element;
            if(executableElement.getParameters().isEmpty() || !executableElement.getParameters().get(0).asType().toString().equals(Consts.AUTOTHISOBJECT))
                throw new IllegalAccessException("The AutoHook Parameter 1 can only 'AutoThisObject'!!! please check Method ["
                        + element.getSimpleName() + "] in class [" + ClassTypeElement.getQualifiedName() + "]");

            if (parentAndChild.containsKey(ClassTypeElement)) { // Has categries

                if(!parentAndChild.get(ClassTypeElement).containsKey(targetClassTypeElement))
                    parentAndChild.get(ClassTypeElement).put(targetClassTypeElement, new ArrayList<>());

                parentAndChild.get(ClassTypeElement).get(targetClassTypeElement).add(element);
            } else {

                parentAndChild.put(ClassTypeElement, new HashMap<>());
                if(!parentAndChild.get(ClassTypeElement).containsKey(targetClassTypeElement))
                    parentAndChild.get(ClassTypeElement).put(targetClassTypeElement, new ArrayList<>());

                parentAndChild.get(ClassTypeElement).get(targetClassTypeElement).add(element);
            }
        }
        if (!MapUtils.isNotEmpty(parentAndChild))
            return;

        CodeBlock javaDoc = CodeBlock.builder()
                .add("<p>这是 *_AutoHookMethod 自动生成的类，用以自动进行注册。</p>\n")
                .add("<p><a href=\"mailto:2721624510@qq.com\">Contact me.</a></p>\n")
                .add("\n")
                .add("@author 德友 \n")
                .add("@date ").add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).add("\n")
                .build();

        for (Map.Entry<TypeElement, Map<TypeElement, List<Element>>> entry : parentAndChild.entrySet()){

            TypeElement parent = entry.getKey();  //封装字段的最里层类
            Map<TypeElement, List<Element>> childElement = entry.getValue();

            //获得包名和文件名
            String qualifiedName = parent.getQualifiedName().toString();
            String packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
            String fileName = parent.getSimpleName() + Consts.SEPARATOR + "AutoHookMethod";
            mLogger.info(">>> handle " + childElement.size() + " field in " + parent + " <<<");
            mLogger.info(">>> create " + packageName + " fileName " + fileName + " <<<");

            //构建自动依赖注入代码的文件
            TypeSpec.Builder injectHelper = TypeSpec.classBuilder(fileName)
                    .addJavadoc(javaDoc)
                    .addSuperinterface(ClassName.get(type_ISyringe))
                    .addModifiers(PUBLIC);

            injectHelper.addField(FieldSpec.builder(ClassName.get(parent), "origObject", Modifier.PUBLIC, Modifier.STATIC)
                    .initializer("null")
                    .build());

            MethodSpec.Builder injectMethodBuilder = MethodSpec.methodBuilder(Consts.METHOD_INJECT)
                    .addAnnotation(Override.class)
                    .addParameter(ParameterSpec.builder(Object.class, "target").build())
                    .addStatement("this.origObject = ($T)target", ClassName.get(parent))
                    .addModifiers(PUBLIC);

            //构造函数
            MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                    .addModifiers(PUBLIC);

            Collection<TypeSpec> TypeSpecs = new ArrayList<>();
            for (Map.Entry<TypeElement, List<Element>> childEntry : childElement.entrySet()){

                TypeElement child_parent = childEntry.getKey();  //封装字段的最里层类
                List<Element> child_Element = childEntry.getValue();

                TypeSpec.Builder childInjectHelper = TypeSpec.classBuilder( child_parent.toString().replace(".","_"))
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addAnnotation(AnnotationSpec.builder(ClassName.get(hook_Class))
                                .addMember("value", "$T.class", ClassName.get(child_parent))
                                .addMember("group", "$S", "autoHook")
                                .build());



                Collection<FieldSpec> FieldSpecs = new ArrayList<>();
                Collection<MethodSpec> MethodSpecs = new ArrayList<>();
                TypeMirror tm;
                for (Element element : child_Element){

                    tm = element.asType();
                    AutoHookMethod autoHookMethod = element.getAnnotation(AutoHookMethod.class);
                    ExecutableElement executableElement = (ExecutableElement) element;
                    AnnotationSpec.Builder Builder = AnnotationSpec.builder(ClassName.get(hook_MethodParams));
                    MethodSpec.Builder MethodBuilder = MethodSpec.methodBuilder(autoHookMethod.methodName() + "_method")
                            .addModifiers(PUBLIC, Modifier.STATIC)
                            .addAnnotation(AnnotationSpec.builder(ClassName.get(hook_Method)).addMember("value", "$S", autoHookMethod.methodName()).build())
                            .returns(TypeName.get(executableElement.getReturnType()))
                            .addException(Throwable.class);

                    Set<Modifier> modifiers = element.getModifiers();
                    if(!modifiers.contains(Modifier.STATIC))
                        MethodBuilder.addParameter(ParameterSpec.builder(Object.class, "thiz").addAnnotation(ClassName.get(hook_ThisObject)).addModifiers(Modifier.FINAL).build());

                    int index = 0;
                    StringBuilder Data = new StringBuilder();
                    for (VariableElement variableElement : executableElement.getParameters()){

                        if(index > 0){

                            Builder.addMember("value", "$T.class", variableElement.asType());
                            MethodBuilder.addParameter(ParameterSpec.builder(TypeName.get(variableElement.asType()), variableElement.getSimpleName().toString()).addModifiers(Modifier.FINAL).build());
                            Data.append(", ").append(variableElement.getSimpleName().toString());
                        }
                        index ++;
                    }

                    String AutoThisObject = "new $T(){\n" +
                            "      @Override\n"+
                            "      public Method getBackup() {\n"+
                            "          return "+autoHookMethod.methodName() + "_method_backup;\n"+
                            "      }\n"+
                            "      @Override\n"+
                            "      public Object getThisObject() {\n"+
                            "          return "+(modifiers.contains(Modifier.STATIC) ? "null" : "thiz")+";\n"+
                            "      }\n"+
                            "      @Override\n"+
                            "      public Object call(Object... args) throws Throwable {\n"+
                            "          if(" + autoHookMethod.methodName() + "_method_backup != null)\n"+
                            "              return $T.get().callOriginByBackup(" + autoHookMethod.methodName() + "_method_backup, " + (modifiers.contains(Modifier.STATIC) ? "null" : "thiz") + ", args);\n"+
                            "          throw new NullPointerException();\n"+
                            "      }\n"+
                            "  }";

                    if(modifiers.contains(Modifier.STATIC)){
                        MethodBuilder.addStatement((executableElement.getReturnType().getKind() == TypeKind.VOID ? "" : "return ") + "$T." + executableElement.getSimpleName() + "(" + AutoThisObject+Data.toString() + ")", ClassName.get(parent), ClassName.get(hook_AutoThisObject), ClassName.get(hook_AutoHookBridge));
                    }else{
                        MethodBuilder.addStatement("  if(origObject != null)\n  " + (executableElement.getReturnType().getKind() == TypeKind.VOID ? "" : "return ") + "origObject." + executableElement.getSimpleName() + "(" + AutoThisObject + Data.toString() + ")", ClassName.get(hook_AutoThisObject), ClassName.get(hook_AutoHookBridge));
                        if(executableElement.getReturnType().getKind() != TypeKind.VOID)
                            MethodBuilder.addStatement("throw new NullPointerException()");
                    }

                    //添加依赖注入的方法
                    FieldSpecs.add(FieldSpec.builder(Method.class, autoHookMethod.methodName() + "_method_backup", Modifier.PUBLIC, Modifier.STATIC)
                            .addAnnotation(AnnotationSpec.builder(ClassName.get(hook_MethodBackup)).addMember("value", "$S", autoHookMethod.methodName()).build())
                            .addAnnotation(Builder.build())
                            .build());

                    MethodSpecs.add(MethodBuilder.addAnnotation(Builder.build()).build());
                }
                childInjectHelper.addFields(FieldSpecs);
                childInjectHelper.addMethods(MethodSpecs);

                TypeSpec typeSpec;
                TypeSpecs.add(typeSpec = childInjectHelper.build());
                mLogger.info(">>> " + typeSpec.name + " <<<");
                constructorBuilder.addStatement("this.mHookClass.add("+typeSpec.name+".class)");
            }

            injectHelper.addField(FieldSpec.builder(ParameterizedTypeName.get(
                    ClassName.get(List.class),
                    ClassName.get(Class.class)
            ),"mHookClass")
                    .addModifiers(Modifier.PRIVATE)
                    .addModifiers(Modifier.STATIC)
                    .initializer("new $T<>()", ClassName.get(ArrayList.class))
                    .build());

            injectHelper.addMethod(MethodSpec.methodBuilder(Consts.METHOD_GET_HOOK_CLASS)
                    .addAnnotation(Override.class)
                    .returns(ParameterizedTypeName.get(
                            ClassName.get(List.class),
                            ClassName.get(Class.class)
                    ))
                    .addStatement("return this.mHookClass")
                    .addModifiers(PUBLIC)
                    .build());

            injectHelper.addMethod(constructorBuilder.build());

            injectHelper.addTypes(TypeSpecs);

            injectHelper.addMethod(injectMethodBuilder.build());

            // 生成自动依赖注入的类文件[ClassName]$$AutoHookMethod
            JavaFile.builder(packageName, injectHelper.build()).build().writeTo(mFiler);
            mLogger.info(">>> " + parent.getSimpleName() + " has been processed, " + fileName + " has been generated. <<<");
        }
    }
}
