package cn.com.kevin.summer.exception;

public class BeanNotOfRequiredTypeException extends BeansException {
    public BeanNotOfRequiredTypeException() {
    }

    public BeanNotOfRequiredTypeException(String message) {
        super(message);
    }
}
