package com.hotelsystem.audit;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {
    // 可选动作描述
    String action() default "";
}
