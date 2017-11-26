package org.cephalus.lwjgl;

import static org.cephalus.lwjgl.Swap.Type.MANUAL;

public @interface Swap {

    Type value() default MANUAL;

    enum Type {
        AUTO
        , MANUAL
    }
}
