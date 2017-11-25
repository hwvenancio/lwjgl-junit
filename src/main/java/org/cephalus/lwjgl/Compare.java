package org.cephalus.lwjgl;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Compare {
    String reference() default "";
    float maxDivergence() default 0.01f;
}
