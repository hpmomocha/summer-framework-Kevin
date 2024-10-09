package cn.com.kevin.summer.utils;

import cn.com.kevin.summer.annotation.Component;
import cn.com.kevin.summer.exception.BeanDefinitionException;


import java.lang.annotation.Annotation;

public class ClassUtils {
    /**
     * 递归查找Annotation
     *
     * 示例：Annotation A可以直接标注在Class定义:
     *
     * <code>
     * @A
     * public class Hello {}
     * </code>
     *
     * 或者Annotation B标注了A，Class标注了B:
     *
     * <code>
     * @A
     * public @interface B {}
     *
     * @B
     * public class Hello {}
     * </code>
     */
    public static <A extends Annotation> A findAnnotation(Class<?> target, Class<A> annoClass) {
        A annotationA = target.getAnnotation(annoClass);
        for (Annotation anno : target.getAnnotations()) {
            Class<? extends Annotation> annoType = anno.annotationType();
            if (!annoType.getPackageName().equals("java.lang.annotation")) {
                A found = findAnnotation(annoType, annoClass);
                if (found != null) {
                    if (annotationA != null) {
                        throw new BeanDefinitionException("Duplicate @" + annoClass.getSimpleName() + " found on class " + target.getSimpleName());
                    }
                    annotationA = found;
                }
            }
        }

        return annotationA;
    }

    /**
     * Get bean name by:
     *
     * <code>
     * @Component
     * public class Hello {}
     * </code>
     */
    public static String getBeanName(Class<?> clazz) {
        String name = "";
        // 查找@Component:
        Component component = clazz.getAnnotation(Component.class);
        if (component != null) {
            // @Component exist:
            name = component.value();
        } else {
            // 未找到@Component，继续在其他注解中查找@Component:
            for (Annotation anno : clazz.getAnnotations()) {
                if (findAnnotation(anno.annotationType(), Component.class) != null) {
                    try {
                        name = (String) anno.annotationType().getMethod("value").invoke(anno);
                    } catch (ReflectiveOperationException e) {
                        throw new BeanDefinitionException("Cannot get annotation value.", e);
                    }
                }
            }
        }
        if (name.isEmpty()) {
            // default name: "HelloWorld" => "helloWorld"
            name = clazz.getSimpleName();
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }
}
