package cn.com.kevin.summer.context;

import cn.com.kevin.summer.annotation.Component;
import cn.com.kevin.summer.exception.BeanCreationException;
import cn.com.kevin.summer.exception.BeanDefinitionException;
import cn.com.kevin.summer.io.PropertyResolver;
import cn.com.kevin.summer.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AnnotationConfigApplicationContext {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final PropertyResolver propertyResolver;
    protected final Map<String, BeanDefinition> beans;


    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
        // 扫描获取所有Bean的Class类型:
        final Set<String> beanClassNames = scanForClassNames(configClass);
        // 创建Bean的定义:
        this.beans = createBeanDefinitions(beanClassNames);
    }

    Map<String, BeanDefinition> createBeanDefinitions(Set<String> beanClassNames) {
        Map<String, BeanDefinition> defs = new HashMap<>();
        for(String beanClassName: beanClassNames) {
            // 获取Class:
            Class<?> clazz = null;
            try {
                clazz = Class.forName(beanClassName);
            } catch (ClassNotFoundException e) {
                throw new BeanCreationException(e);
            }

            if (clazz.isAnnotation() || clazz.isEnum() || clazz.isInterface() || clazz.isRecord() ) {
                continue;
            }

            // 是否标注@Component?
            Component component = ClassUtils.findAnnotation(clazz, Component.class);
            if (component != null) {
                logger.atDebug().log("found component: {}", clazz.getName());
                // 获取类/接口修饰符
                int mod = clazz.getModifiers();
                if (Modifier.isAbstract(mod)) {
                    throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be abstract.");
                }
                if (Modifier.isPrivate(mod)) {
                    throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be private.");
                }

                String beanName = ClassUtils.getBeanName(clazz);
                new BeanDefinition(beanName, clazz, get)
            }
        }

        return defs;
    }

    /**
     * Do component scan and return class names.
     */
    protected Set<String> scanForClassNames(Class<?> configClass) {
        
    }

    /**
     * Get public constructor or non-public constructor as fallback.
     */
    Constructor<?> getSuitableConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length == 0) {
            Constructor<?> declaredConstructor = clazz.getDeclaredConstructor();
        }
    }
}
