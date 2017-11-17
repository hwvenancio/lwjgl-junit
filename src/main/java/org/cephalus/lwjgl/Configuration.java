package org.cephalus.lwjgl;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Configuration {

    int profile() default 320;

    int width() default 640;

    int height() default 480;

    int fps() default 60;

    int iterations() default 120;
}
