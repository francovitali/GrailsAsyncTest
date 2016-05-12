package com.asyncTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Created by fvitali on 5/6/16.
 */
@Target(ElementType.METHOD)
public @interface UserRequired {
    boolean value();
}
