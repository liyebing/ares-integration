//package com.ares.demo.spring.factory;
//
////import org.mockito.Mockito;
//import org.springframework.beans.factory.FactoryBean;
//
///**
// *  自定义工厂，整合JMockit与Spring Runtime Environment
// *
// * @author liyebing created on  15/9/22.
// * @version $Id$
// */
//public class MockitoFactoryBean<T> implements FactoryBean<T> {
//
//    private Class<T> classToBeMocked;
//
//
//    public MockitoFactoryBean(Class<T> classToBeMocked) {
//        this.classToBeMocked = classToBeMocked;
//    }
//
//    public T getObject() throws Exception {
//        //return Mockito.mock(classToBeMocked);
//        return null;
//    }
//
//    public Class<?> getObjectType() {
//        return classToBeMocked;
//    }
//
//    public boolean isSingleton() {
//        return true;
//    }
//}
