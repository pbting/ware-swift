package com.ware.swift.hot.data;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Sink {

    /**
     * @return
     */
    String name() default "";

    /**
     * @return
     */
    String desc() default "";
}
