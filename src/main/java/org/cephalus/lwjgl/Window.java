package org.cephalus.lwjgl;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Window {
    int width();
    int height();
}
