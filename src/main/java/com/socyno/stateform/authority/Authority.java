package com.socyno.stateform.authority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Authority {
    AuthorityScopeType value() ;
    int paramIndex() default -1;
    Class<? extends AuthorityScopeIdParser> parser() default AuthorityScopeIdNoopParser.class;
    Class<? extends AuthoritySpecialChecker> checker() default AuthoritySpecialNoopChecker.class;
    Class<? extends AuthoritySpecialRejecter> rejecter() default AuthoritySpecialNoopRejecter.class;
}
