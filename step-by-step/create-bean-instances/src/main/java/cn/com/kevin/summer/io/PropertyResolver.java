package cn.com.kevin.summer.io;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.*;
import java.util.function.Function;

/**
 * 为了注入配置，我们用PropertyResolver保存所有配置项，对外提供查询功能。
 * 支持3种查询方式：
 *  1. 按配置的key查询，例如：getProperty("app.title")
 *  2. 以${abc.xyz}形式的查询，例如，getProperty("${app.title}")，常用于@Value("${app.title}")注入；
 *  3. 带默认值的，以${abc.xyz:defaultValue}形式的查询，
 *     例如，getProperty("${app.title:Summer}")，常用于@Value("${app.title:Summer}")注入。
 */
public class PropertyResolver {
    Logger logger = LoggerFactory.getLogger(getClass());

    Map<String, String> properties = new HashMap<>();

    Map<Class<?>, Function<String, Object>> converters = new HashMap<>();

    public PropertyResolver(Properties props) {
        this.properties.putAll(System.getenv());
        props.stringPropertyNames().forEach(name -> {
            this.properties.put(name, props.getProperty(name));
        });

        if (logger.isDebugEnabled()) {
            List<String> keys = new ArrayList<>(this.properties.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                logger.debug("PropertyResolver: {} = {}", key, this.properties.get(key));
            }
        }
        // register converters:
        converters.put(String.class, s -> s);
        converters.put(boolean.class, s -> Boolean.parseBoolean(s));
        converters.put(Boolean.class, s -> Boolean.valueOf(s));

        converters.put(byte.class, s -> Byte.parseByte(s));
        converters.put(Byte.class, s -> Byte.valueOf(s));

        converters.put(short.class, s -> Short.parseShort(s));
        converters.put(Short.class, s -> Short.valueOf(s));

        converters.put(int.class, s -> Integer.parseInt(s));
        converters.put(Integer.class, s -> Integer.valueOf(s));

        converters.put(long.class, s -> Long.parseLong(s));
        converters.put(Long.class, s -> Long.valueOf(s));

        converters.put(float.class, s -> Float.parseFloat(s));
        converters.put(Float.class, s -> Float.valueOf(s));

        converters.put(double.class, s -> Double.parseDouble(s));
        converters.put(Double.class, s -> Double.valueOf(s));

        converters.put(LocalDate.class, s -> LocalDate.parse(s));
        converters.put(LocalTime.class, s -> LocalTime.parse(s));
        converters.put(LocalDateTime.class, s -> LocalDateTime.parse(s));
        converters.put(ZonedDateTime.class, s -> ZonedDateTime.parse(s));
        converters.put(Duration.class, s -> Duration.parse(s));
        converters.put(ZoneId.class, s -> ZoneId.of(s));
    }

    /**
     * 判断是否存在某个属性
     * @param key
     * @return
     */
    public boolean containsProperty(String key) {
        return this.properties.containsKey(key);
    }

    /**
     * 获取属性值
     * key 可能的形式：
     *  1. ${abc.xyz}
     *  2. ${abc.xyz: defaultValue}
     *  3. abc.xyz
     * @param key
     * @return
     */
    @Nullable
    public String getProperty(String key) {
        PropertyExpr keyExpr = parsePropertyExpr(key);
        if (keyExpr != null) {
            if (keyExpr.defaultValue() != null) {
                return getProperty(keyExpr.key(), keyExpr.defaultValue());
            } else {
                return getRequiredProperty(keyExpr.key());
            }
        }
        String value = this.properties.get(key);
        if (value != null) {
            return parseValue(value);
        }
        return null;
    }

    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value == null? parseValue(defaultValue) : value;
    }

    @Nullable
    public <T> T getProperty(String key, Class<T> targetType) {
        String value = getProperty(key);
        if (value == null) {
            return null;
        }
        return convert(targetType, value);
    }

    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return convert(targetType, value);
    }

    <T> T convert(Class<?> clazz, String value) {
        Function<String, Object> fn = this.converters.get(clazz);
        if (fn == null) {
            throw new IllegalArgumentException("Unsupported value type: " + clazz.getName());
        }
        return (T) fn.apply(value);
    }

    /**
     * 支持表达式嵌套的情况
     * ${app.title:${APP_NAME:Summer}}
     * @param value
     * @return
     */
    String parseValue(String value) {
        PropertyExpr expr = parsePropertyExpr(value);
        // value 不是嵌套的情况
        if (expr == null) {
            return value;
        }
        if (expr.defaultValue() != null) {
            return getProperty(expr.key(), expr.defaultValue());
        } else {
            return getRequiredProperty(expr.key());
        }
    }

    private String getRequiredProperty(String key) {
        String value = getProperty(key);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }

    public <T> T getRequiredProperty(String key, Class<T> targetType) {
        T value = getProperty(key, targetType);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }

    PropertyExpr parsePropertyExpr(String key) {
        if (key.startsWith("${") && key.endsWith("}")) {
            int n = key.indexOf(':');
            if (n == -1) {
                // no default value: ${key}
                String k = notEmpty(key.substring(2, key.length() - 1));
                return new PropertyExpr(k, null);
            } else {
                // has default value: ${key:default}
                String k = notEmpty(key.substring(2, n));
                return new PropertyExpr(k, key.substring(n + 1, key.length() - 1));
            }
        }
        return null;
    }

    String notEmpty(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Invalid key: " + key);
        }
        return key;
    }

    record PropertyExpr(String key, String defaultValue) {}
}
