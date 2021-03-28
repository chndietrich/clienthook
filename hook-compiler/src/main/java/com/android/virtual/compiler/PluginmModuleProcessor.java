package com.android.virtual.compiler;

import com.android.virtual.client.hook.annotation.AutoHookMethod;
import com.android.virtual.client.hook.annotation.PluginmModule;
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
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static javax.lang.model.element.Modifier.PUBLIC;

@AutoService(Processor.class)
@SupportedOptions(Consts.KEY_MODULE_NAME)
public class PluginmModuleProcessor extends AbstractProcessor {

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
        mLogger.info(">>> PluginmModuleProcessor init. <<<");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        if (CollectionUtils.isNotEmpty(annotations)) {
            Set<? extends Element> pluginmModuleElements = roundEnvironment.getElementsAnnotatedWith(PluginmModule.class);
            try {
                mLogger.info(">>> Found PluginmModule, start... <<<");
                parsePluginmModule(pluginmModuleElements);
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
        types.add(PluginmModule.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private void parsePluginmModule(Set<? extends Element> pluginmModuleElements)  throws IOException, IllegalAccessException {

        if (CollectionUtils.isNotEmpty(pluginmModuleElements)) {
            mLogger.info(">>> Found PluginmModule, size is " + pluginmModuleElements.size() + " <<<");

            ClassName pageConfigClassName = ClassName.get(Consts.PAGE_CONFIG_PACKAGE_NAME, Consts.upperFirstLetter(moduleName) + "PluginmModule" + Consts.PAGE_CONFIG_CLASS_NAME_SUFFIX);
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
                    ClassName.get(Map.class),
                    ClassName.get(String.class),
                    ParameterizedTypeName.get(
                            ClassName.get(Map.class),
                            ClassName.get(String.class),
                            ParameterizedTypeName.get(
                                    ClassName.get(List.class),
                                    ClassName.get(Class.class)
                            )
                    )
            );

            Map<String, Map<String, List<Element>>> rootMap = new HashMap<>();
            TypeMirror tm;
            for (Element element : pluginmModuleElements) {
                tm = element.asType();
                mLogger.info(">>> Found PluginmModule: " + tm.toString() + " <<<");

                PluginmModule pluginmModule = element.getAnnotation(PluginmModule.class);
                String packageName = pluginmModule.value();
                String processName = StringUtils.isEmpty(pluginmModule.processName()) ? packageName : pluginmModule.processName();
                if(rootMap.containsKey(packageName)){
                    if(!rootMap.get(packageName).containsKey(processName))
                        rootMap.get(packageName).put(processName, new ArrayList<>());

                    rootMap.get(packageName).get(processName).add(element);
                }else{
                    rootMap.put(packageName, new HashMap<>());
                    if(!rootMap.get(packageName).containsKey(processName))
                        rootMap.get(packageName).put(processName, new ArrayList<>());

                    rootMap.get(packageName).get(processName).add(element);
                }
            }
            if(rootMap.isEmpty())
                return;

            //构造函数
            MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE);

//            constructorBuilder.addStatement("mAllPluginmModuleHooks = new $T<>()", ClassName.get(HashMap.class));
            Collection<FieldSpec> FieldSpecs = new ArrayList<>();
            FieldSpecs.add(FieldSpec.builder(inputListTypeOfPage, "mAllPluginmModuleHooks")
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .initializer("new HashMap<String, Map<String, List<Class>>>()")
                    .build());

            Collection<MethodSpec> MethodSpecs = new ArrayList<>();
            int index = 0;
            for (Map.Entry<String, Map<String, List<Element>>> entry : rootMap.entrySet()){

                String packageName = entry.getKey();  //封装字段的最里层类
                Map<String, List<Element>> parentAndChild = entry.getValue();

                String ChildMap = "mChildMap_" + index;
                constructorBuilder.addStatement("Map<String, List<Class>> " + ChildMap + " = new $T<>()", ClassName.get(HashMap.class));
                for (Map.Entry<String, List<Element>> entryChild : parentAndChild.entrySet()){

                    String processName = entryChild.getKey();  //封装字段的最里层类
                    constructorBuilder.addStatement(ChildMap + ".put($S, new $T<>())", processName, ClassName.get(ArrayList.class));
                    List<Element> elements = entryChild.getValue();
                    TypeMirror tmChild;
                    for (Element element : elements){
                        tmChild= element.asType();
                        constructorBuilder.addStatement(ChildMap + ".get($S).add($T.class)", processName, tmChild);
                    }
                }
                constructorBuilder.addStatement("mAllPluginmModuleHooks.put($S, " + ChildMap + ")", packageName);
                index++;
            }
            MethodSpecs.add(MethodSpec.methodBuilder("getAllPluginmModuleHooks")
                    .addModifiers(PUBLIC)
                    .returns(inputListTypeOfPage)
                    .addStatement("return mAllPluginmModuleHooks")
                    .build());

            MethodSpec constructorMethod = constructorBuilder.build();
            MethodSpec instanceMethod = MethodSpec.methodBuilder("getInstance")
                    .addModifiers(PUBLIC)
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
                    .add("<p>这是 AppPluginmModuleHookConfig 自动生成的类，用以自动进行注册。</p>\n")
                    .add("<p><a href=\"mailto:2721624510@qq.com\">Contact me.</a></p>\n")
                    .add("\n")
                    .add("@author 德友 \n")
                    .add("@date ").add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).add("\n")
                    .build();

            pageConfigBuilder
                    .addJavadoc(javaDoc)
                    .addModifiers(PUBLIC)
                    .addField(instanceField)
                    .addFields(FieldSpecs)
                    .addMethod(constructorMethod)
                    .addMethod(instanceMethod)
                    .addMethods(MethodSpecs);

            JavaFile.builder(Consts.PAGE_CONFIG_PACKAGE_NAME, pageConfigBuilder.build()).build().writeTo(mFiler);
        }
    }

}
