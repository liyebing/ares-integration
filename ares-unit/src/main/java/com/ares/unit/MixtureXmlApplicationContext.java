package com.ares.unit;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * 同时支持协议 classpath:与 file:
 *
 * @author liyebing created on  15/10/24.
 * @version $Id$
 */
public class MixtureXmlApplicationContext extends AbstractXmlApplicationContext {

    /**
     * Create a new MixtureXmlApplicationContext for bean-style configuration.
     * @see #setConfigLocation
     * @see #setConfigLocations
     * @see #afterPropertiesSet()
     */
    public MixtureXmlApplicationContext() {
    }

    /**
     * Create a new MixtureXmlApplicationContext for bean-style configuration.
     * @param parent the parent context
     * @see #setConfigLocation
     * @see #setConfigLocations
     * @see #afterPropertiesSet()
     */
    public MixtureXmlApplicationContext(ApplicationContext parent) {
        super(parent);
    }

    /**
     * Create a new MixtureXmlApplicationContext, loading the definitions
     * from the given XML file and automatically refreshing the context.
     * @param configLocation file path
     * @throws BeansException if context creation failed
     */
    public MixtureXmlApplicationContext(String configLocation) throws BeansException {
        this(new String[] {configLocation}, true, null);
    }

    /**
     * Create a new MixtureXmlApplicationContext, loading the definitions
     * from the given XML files and automatically refreshing the context.
     * @param configLocations array of file paths
     * @throws BeansException if context creation failed
     */
    public MixtureXmlApplicationContext(String... configLocations) throws BeansException {
        this(configLocations, true, null);
    }

    /**
     * Create a new MixtureXmlApplicationContext with the given parent,
     * loading the definitions from the given XML files and automatically
     * refreshing the context.
     * @param configLocations array of file paths
     * @param parent the parent context
     * @throws BeansException if context creation failed
     */
    public MixtureXmlApplicationContext(String[] configLocations, ApplicationContext parent) throws BeansException {
        this(configLocations, true, parent);
    }

    /**
     * Create a new MixtureXmlApplicationContext, loading the definitions
     * from the given XML files.
     * @param configLocations array of file paths
     * @param refresh whether to automatically refresh the context,
     * loading all bean definitions and creating all singletons.
     * Alternatively, call refresh manually after further configuring the context.
     * @throws BeansException if context creation failed
     * @see #refresh()
     */
    public MixtureXmlApplicationContext(String[] configLocations, boolean refresh) throws BeansException {
        this(configLocations, refresh, null);
    }

    /**
     * Create a new MixtureXmlApplicationContext with the given parent,
     * loading the definitions from the given XML files.
     * @param configLocations array of file paths
     * @param refresh whether to automatically refresh the context,
     * loading all bean definitions and creating all singletons.
     * Alternatively, call refresh manually after further configuring the context.
     * @param parent the parent context
     * @throws BeansException if context creation failed
     * @see #refresh()
     */
    public MixtureXmlApplicationContext(String[] configLocations, boolean refresh, ApplicationContext parent)
            throws BeansException {

        super(parent);
        setConfigLocations(configLocations);
        if (refresh) {
            refresh();
        }
    }


    /**
     * Resolve resource paths as file system paths.
     * <p>Note: Even if a given path starts with a slash, it will get
     * interpreted as relative to the current VM working directory.
     * This is consistent with the semantics in a Servlet container.
     * @param path path to the resource
     * @return Resource handle
     */
    @Override
    protected Resource getResourceByPath(String path) {
        if (path != null && path.startsWith("/")) {
            path = path.substring(1);
        }
        if(StringUtils.startsWithIgnoreCase(path,"file")){
            return new FileSystemResource(path);
        }else {
            return new ClassPathResource(path);
        }
    }

}
