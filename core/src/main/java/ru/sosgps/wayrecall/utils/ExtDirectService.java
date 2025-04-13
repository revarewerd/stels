package ru.sosgps.wayrecall.utils;

import java.lang.annotation.*;
import org.springframework.stereotype.Component;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ExtDirectService {
    String value() default "";
}
