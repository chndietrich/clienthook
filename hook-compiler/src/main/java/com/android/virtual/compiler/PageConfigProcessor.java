package com.android.virtual.compiler;

import com.android.virtual.client.annotation.HookClass;
import com.android.virtual.client.annotation.HookReflectClass;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.android.virtual.util.Logger;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static com.android.virtual.util.Consts.KEY_MODULE_NAME;

/**
 * 页面配置自动生成器
 * @author xuexiang
 */
@AutoService(Processor.class)
@SupportedOptions(KEY_MODULE_NAME)
public class PageConfigProcessor extends AbstractProcessor {
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
    /**
     * 页面配置所在的包名
     */
    private static final String PAGE_CONFIG_PACKAGE_NAME = "com.android.virtual.client.hook";

    private static final String PAGE_CONFIG_CLASS_NAME_SUFFIX = "HookConfig";

    /**
     * 组注册信息
     */
    private Map<String, List<TypeMirror>> rootMap = new TreeMap<>();

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
            moduleName = options.get(KEY_MODULE_NAME);
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
        mLogger.info(">>> PageConfigProcessor init. <<<");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        if (CollectionUtils.isNotEmpty(annotations)) {
            Set<? extends Element> hookClassElements = roundEnvironment.getElementsAnnotatedWith(HookClass.class);
            Set<? extends Element> hookReflectClassElements = roundEnvironment.getElementsAnnotatedWith(HookReflectClass.class);
            try {
                mLogger.info(">>> Found HookClass, start... <<<");
                parseHookClass(hookClassElements, hookReflectClassElements);
            } catch (Exception e) {
                mLogger.error(e);
            }
            return true;
        }
        return false;
    }

    /**
     * 解析页面标注
     */
    private void parseHookClass(Set<? extends Element> hookClassElements, Set<? extends Element> hookReflectClassElements) throws IOException {

        if (CollectionUtils.isNotEmpty(hookClassElements) || CollectionUtils.isNotEmpty(hookReflectClassElements)) {
            mLogger.info(">>> Found hookClass, size is " + (hookClassElements.size() + hookReflectClassElements.size()) + " <<<");

            ClassName pageConfigClassName = ClassName.get(PAGE_CONFIG_PACKAGE_NAME, upperFirstLetter(moduleName) + PAGE_CONFIG_CLASS_NAME_SUFFIX);
            TypeSpec.Builder pageConfigBuilder = TypeSpec.classBuilder(pageConfigClassName);

             /*
               private static PageConfig sInstance;
             */
            FieldSpec instanceField = FieldSpec.builder(pageConfigClassName, "sInstance")
                    .addModifiers(Modifier.PRIVATE)
                    .addModifiers(Modifier.STATIC)
                    .build();

            /*

              ``List<PageInfo>```
             */
            ParameterizedTypeName inputListTypeOfPage = ParameterizedTypeName.get(
                    ClassName.get(List.class),
                    ClassName.get(Class.class)
            );

            TypeMirror tm;
            String group;
            for (Element element : hookClassElements) {
                tm = element.asType();
                mLogger.info(">>> Found Hook: " + tm.toString() + " <<<");

                HookClass hookClass = element.getAnnotation(HookClass.class);
                group = StringUtils.isEmpty(hookClass.group()) ? "hook" : hookClass.group();
                TypeMirror type_Class = mElements.getTypeElement(tm.toString()).asType();
                if(rootMap.containsKey(group)){
                    rootMap.get(group).add(type_Class);
                }else{
                    rootMap.put(group, new ArrayList<>());
                    rootMap.get(group).add(type_Class);
                }
            }
            for (Element element : hookReflectClassElements) {
                tm = element.asType();
                mLogger.info(">>> Found Hook: " + tm.toString() + " <<<");

                HookReflectClass hookClass = element.getAnnotation(HookReflectClass.class);
                group = StringUtils.isEmpty(hookClass.group()) ? "hook" : hookClass.group();
                TypeMirror type_Class = mElements.getTypeElement(tm.toString()).asType();
                if(rootMap.containsKey(group)){
                    rootMap.get(group).add(type_Class);
                }else{
                    rootMap.put(group, new ArrayList<>());
                    rootMap.get(group).add(type_Class);
                }
            }
            if(rootMap.isEmpty())
                return;

            //构造函数
            MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE);

            constructorBuilder.addStatement("mAllHooks = new $T<>()", ClassName.get(ArrayList.class));

            Collection<FieldSpec> FieldSpecs = new ArrayList<>();
            FieldSpecs.add(FieldSpec.builder(inputListTypeOfPage, "mAllHooks")
                    .addModifiers(Modifier.PRIVATE)
                    .build());

            Collection<MethodSpec> MethodSpecs = new ArrayList<>();
            for (String obj : rootMap.keySet()){

                String fieldName = upperFirstLetter(obj + "s");
                FieldSpecs.add(FieldSpec.builder(inputListTypeOfPage, "m" + fieldName)
                        .addModifiers(Modifier.PRIVATE)
                        .build());

                constructorBuilder.addStatement("m" + fieldName + " = new $T<>()", ClassName.get(ArrayList.class));
                for (TypeMirror typeMirror : rootMap.get(obj)){
                    constructorBuilder.addStatement("m" + fieldName + ".add($T.class)", typeMirror);
                    constructorBuilder.addStatement("mAllHooks.add($T.class)", typeMirror);
                }

                MethodSpecs.add(MethodSpec.methodBuilder("get" + fieldName)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(inputListTypeOfPage)
                        .addStatement("return " + "m" + fieldName)
                        .build());
            }
            MethodSpecs.add(MethodSpec.methodBuilder("getAllHooks")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(inputListTypeOfPage)
                    .addStatement("return mAllHooks")
                    .build());

            MethodSpec constructorMethod = constructorBuilder.build();
            MethodSpec instanceMethod = MethodSpec.methodBuilder("getInstance")
                    .addModifiers(Modifier.PUBLIC)
                    .addModifiers(Modifier.STATIC)
                    .returns(pageConfigClassName)
                    .addCode("if (sInstance == null) {\n" +
                            "    synchronized ($T.class) {\n" +
                            "        if (sInstance == null) {\n" +
                            "            sInstance = new $T();\n" +
                            "        }\n" +
                            "    }\n" +
                            "}\n", pageConfigClassName, pageConfigClassName)
                    .addStatement("return sInstance")
                    .build();

            CodeBlock javaDoc = CodeBlock.builder()
                    .add("<p>这是HookConfigProcessor自动生成的类，用以自动进行注册。</p>\n")
                    .add("<p><a href=\"mailto:2721624510@qq.com\">Contact me.</a></p>\n")
                    .add("\n")
                    .add("@author 德友 \n")
                    .add("@date ").add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).add("\n")
                    .build();

            pageConfigBuilder
                    .addJavadoc(javaDoc)
                    .addModifiers(Modifier.PUBLIC)
                    .addField(instanceField)
                    .addFields(FieldSpecs)
                    .addMethod(constructorMethod)
                    .addMethod(instanceMethod)
                    .addMethods(MethodSpecs);

            JavaFile.builder(PAGE_CONFIG_PACKAGE_NAME, pageConfigBuilder.build()).build().writeTo(mFiler);
        }
    }

    /**
     * @return 指定哪些注解应该被注解处理器注册
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(HookClass.class.getCanonicalName());
        types.add(HookReflectClass.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 首字母大写
     *
     * @param s 待转字符串
     * @return 首字母大写字符串
     */
    public static String upperFirstLetter(final String s) {
        if (StringUtils.isEmpty(s) || !Character.isLowerCase(s.charAt(0))) {
            return s;
        }
        return (char) (s.charAt(0) - 32) + s.substring(1);
    }
}
