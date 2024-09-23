package cn.com.kevin.summer.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * 函数式接口只能有一个抽象方法
 * @param <T>
 */
@FunctionalInterface
public interface InputStreamCallback<T> {
    T doWithInputStream(InputStream stream) throws IOException;
}
