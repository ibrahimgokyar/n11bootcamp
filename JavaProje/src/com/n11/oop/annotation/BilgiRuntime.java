package com.n11.oop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @Retention(RetentionPolicy.RUNTIME) @interface BilgiRuntime {
    String bilgi();
    String tarih();
    String yazar();
}
