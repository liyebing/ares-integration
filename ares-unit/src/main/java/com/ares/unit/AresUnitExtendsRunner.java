package com.ares.unit;

import mockit.internal.startup.Startup;
import org.databene.benerator.Generator;
import org.databene.benerator.anno.AnnotationMapper;
import org.databene.benerator.anno.ThreadPoolSize;
import org.databene.benerator.engine.BeneratorContext;
import org.databene.benerator.engine.DefaultBeneratorContext;
import org.databene.benerator.wrapper.ProductWrapper;
import org.databene.commons.ConfigurationError;
import org.databene.commons.Period;
import org.databene.commons.converter.AnyConverter;
import org.databene.feed4junit.*;
import org.databene.feed4junit.scheduler.DefaultFeedScheduler;
import org.databene.platform.java.Entity2JavaConverter;
import org.databene.script.DatabeneScriptParser;
import org.databene.script.Expression;
import org.junit.Test;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 1、整合feed4junit提供的参数化测试能力
 * 2、手工启动Spring运行时环境，获得Spring IOC能力，同时也获得Spring的运行时环境
 * 3、整合Mock框架JMockit,使得可以对外部依赖的接口进行Mock,一般来说就是DAO接口与RPC远程调用接口(thrift等)
 *
 * @author liyebing created on 15/9/22.
 * @version $Id$
 */
public class AresUnitExtendsRunner extends Feeder {

    public static final long DEFAULT_TIMEOUT = Period.WEEK.getMillis();

    private static ApplicationContext applicationContext;

    static {
        Startup.initializeIfPossible();
        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
    }

    private Feed4JUnitConfig config;
    private BeneratorContext context;
    private AnnotationMapper annotationMapper;

    private List<FrameworkMethod> children;
    private RunnerScheduler scheduler;

    public AresUnitExtendsRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected String testName(FrameworkMethod method) {
        return (method instanceof FrameworkMethodWithParameters ? method.toString() : super.testName(method));
    }

    @Override
    public void setScheduler(RunnerScheduler scheduler) {
        this.scheduler = scheduler;
        super.setScheduler(scheduler);
    }

    @Override
    protected Statement methodInvoker(FrameworkMethod method, Object test) {
        return super.methodInvoker(method, test);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected Object createTest() throws Exception {
        Object testObject = super.createTest();
        for (FrameworkField attribute : getTestClass().getAnnotatedFields(org.databene.benerator.anno.Source.class)) {
            if ((attribute.getField().getModifiers() & Modifier.PUBLIC) == 0)
                throw new ConfigurationError("Attribute '" + attribute.getField().getName() + "' must be public");
            Generator<?> generator = getAnnotationMapper().createAndInitAttributeGenerator(attribute.getField(), getContext());
            if (generator != null) {
                ProductWrapper wrapper = new ProductWrapper();
                wrapper = generator.generate(wrapper);
                if (wrapper != null)
                    attribute.getField().set(testObject, wrapper.unwrap());
            }
        }

        try {
            // 启动spring 运行时环境
            boolean isPresent = testObject.getClass().isAnnotationPresent(SpringContextConfiguration.class);
            if (isPresent) {
                String[] springConfigs = testObject.getClass().getAnnotation(SpringContextConfiguration.class).locations();
                System.out.println(springConfigs[0]);
                if (applicationContext == null) {
                    applicationContext = new MixtureXmlApplicationContext(springConfigs);
                }
                System.out.println("spring runtime environment init success.");
            }
        } catch (Exception e) {
            throw new RuntimeException("spring runtime environment init error.", e);
        }

        // 获取spring容器托管的bean,将其中的属性赋值给testObject
        Object springBean = applicationContext.getBean(testObject.getClass());
        Field[] fields = springBean.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Resource.class) || field.isAnnotationPresent(Autowired.class)) {
                Object fService = field.get(springBean);
                Class<?> fieldClazz = field.getType();
                if (fService != null) {
                    Field[] innerFields = testObject.getClass().getDeclaredFields();
                    for (Field innerField : innerFields) {
                        innerField.setAccessible(true);
                        if (innerField.isAnnotationPresent(Resource.class) || innerField.isAnnotationPresent(Autowired.class)) {
                            if (fieldClazz.equals(innerField.getType())) {
                                innerField.set(testObject, fService);
                            }
                        }
                    }
                }
            }
        }

        return testObject;
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        if (children == null) {
            children = new ArrayList<FrameworkMethod>();
            TestClass testClass = getTestClass();
            AnnotationMapper annotationMapper = getAnnotationMapper();
            for (FrameworkMethod method : testClass.getAnnotatedMethods(Test.class)) {
                if (method.getMethod().getParameterTypes().length == 0) {
                    // standard JUnit test method
                    children.add(method);
                    continue;
                } else {
                    // parameterized Feed4JUnit test method
                    BeneratorContext context = getContext();
                    context.setGeneratorFactory(config.createDefaultGeneratorFactory());
                    annotationMapper.parseClassAnnotations(testClass.getAnnotations(), context);
                    List<? extends FrameworkMethod> parameterizedTestMethods;
                    parameterizedTestMethods = computeParameterizedTestMethods(method.getMethod(), context);
                    children.addAll(parameterizedTestMethods);
                }
            }
        }
        return children;
    }

    @Override
    protected void validateTestMethods(List<Throwable> errors) {
        validatePublicVoidMethods(Test.class, false, errors);
    }

    // test execution
    // --------------------------------------------------------------------------------------------------

    protected Statement childrenInvoker(final RunNotifier notifier) {
        return new Statement() {
            @Override
            public void evaluate() {
                runChildren(notifier);
            }
        };
    }

    private void runChildren(final RunNotifier notifier) {
        RunnerScheduler scheduler = getScheduler();
        for (FrameworkMethod method : getChildren())
            scheduler.schedule(new ChildRunner(this, method, notifier));
        scheduler.finished();
    }

    @Override
    public void runChild(FrameworkMethod method, RunNotifier notifier) {
        super.runChild(method, notifier);
    }

    public RunnerScheduler getScheduler() {
        if (scheduler == null)
            scheduler = createDefaultScheduler();
        return scheduler;
    }

    protected RunnerScheduler createDefaultScheduler() {
        TestClass testClass = getTestClass();
        Scheduler annotation = testClass.getJavaClass().getAnnotation(Scheduler.class);
        if (annotation != null) {
            String spec = annotation.value();
            Expression<?> bean = DatabeneScriptParser.parseBeanSpec(spec);
            return (RunnerScheduler) bean.evaluate(null);
        } else {
            return new DefaultFeedScheduler(1, DEFAULT_TIMEOUT);
        }
    }

    // helpers
    // ---------------------------------------------------------------------------------------------------------

    private void validatePublicVoidMethods(Class<? extends Annotation> annotation, boolean isStatic, List<Throwable> errors) {
        List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(annotation);
        for (FrameworkMethod eachTestMethod : methods)
            eachTestMethod.validatePublicVoid(isStatic, errors);
    }

    private List<FrameworkMethodWithParameters> computeParameterizedTestMethods(Method method, BeneratorContext context) {
        Integer threads = getThreadCount(method);
        long timeout = getTimeout(method);
        List<FrameworkMethodWithParameters> result = new ArrayList<FrameworkMethodWithParameters>();
        Class<?>[] parameterTypes = method.getParameterTypes();
        AnnotationMapper annotationMapper = getAnnotationMapper();
        TestInfoProvider infoProvider = getConfig().getInfoProvider();
        try {
            Generator<Object[]> paramGenerator = annotationMapper.createAndInitMethodParamsGenerator(method, context);
            Class<?>[] expectedTypes = parameterTypes;
            ProductWrapper<Object[]> wrapper = new ProductWrapper<Object[]>();
            int count = 0;
            while ((wrapper = paramGenerator.generate(wrapper)) != null) {
                Object[] generatedParams = wrapper.unwrap();
                if (generatedParams.length > expectedTypes.length) // imported
                                                                   // data may
                                                                   // have more
                                                                   // columns
                                                                   // than the
                                                                   // method
                                                                   // parameters,
                                                                   // ...
                    generatedParams = Arrays.copyOfRange(generatedParams, 0, expectedTypes.length); // ...so
                                                                                                    // cut
                                                                                                    // them
                for (int i = 0; i < generatedParams.length; i++) {
                    generatedParams[i] = Entity2JavaConverter.convertAny(generatedParams[i]);
                    generatedParams[i] = AnyConverter.convert(generatedParams[i], parameterTypes[i]);
                }
                // generated params may be to few, e.g. if an XLS row was
                // imported with trailing nulls,
                // so create an array of appropriate size
                Object[] usedParams = new Object[parameterTypes.length];
                System.arraycopy(generatedParams, 0, usedParams, 0, Math.min(generatedParams.length, usedParams.length));
                String info = infoProvider.testInfo(method, usedParams);
                result.add(new FrameworkMethodWithParameters(method, usedParams, threads, timeout, info));
                count++;
            }
            if (count == 0)
                throw new RuntimeException("No parameter values available for method: " + method);
        } catch (Exception e) {
            // LOGGER.error("Error creating test parameters", e);
            String info = infoProvider.errorInfo(method, e);
            result.add(new ErrorReportingFrameworkMethod(method, e, info));
        }
        return result;
    }

    private Integer getThreadCount(Method method) {
        ThreadPoolSize methodAnnotation = method.getAnnotation(ThreadPoolSize.class);
        if (methodAnnotation != null)
            return methodAnnotation.value();
        Class<?> testClass = method.getDeclaringClass();
        ThreadPoolSize classAnnotation = testClass.getAnnotation(ThreadPoolSize.class);
        if (classAnnotation != null)
            return classAnnotation.value();
        return null;
    }

    private long getTimeout(Method method) {
        return DEFAULT_TIMEOUT;
    }

    private Feed4JUnitConfig getConfig() {
        if (this.config == null)
            init();
        return this.config;
    }

    public BeneratorContext getContext() {
        if (this.config == null)
            init();
        return this.context;
    }

    private AnnotationMapper getAnnotationMapper() {
        if (this.config == null)
            init();
        return this.annotationMapper;
    }

    private void init() {
        this.config = new Feed4JUnitConfig();
        this.context = new DefaultBeneratorContext();
        this.annotationMapper = new AnnotationMapper(context.getDataModel(), config.getPathResolver());
    }
}
