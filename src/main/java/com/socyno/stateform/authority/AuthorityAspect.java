package com.socyno.stateform.authority;

import com.socyno.base.bscexec.MissingUserException;
import com.socyno.base.bscmixutil.ClassUtil;
import com.socyno.base.bscmodel.SessionContext;
import com.socyno.stateform.authority.Authority;
import com.socyno.stateform.field.OptionSystemAuth;
import com.socyno.stateform.service.PermissionService;
import com.socyno.webbsc.exception.NoAuthorityException;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

@Slf4j
@Aspect
public class AuthorityAspect {
    
    @Around(value = "@annotation(authority)")
    public Object aroundAuthority(ProceedingJoinPoint joinPoint, Authority authority) throws Throwable {
        checkAuthority(joinPoint.getArgs(), ((MethodSignature) joinPoint.getSignature()).getMethod());
        return joinPoint.proceed();
    }
    
    /**
     * 校验权限注解
     */
    private void checkAuthority(Object[] args, Method method) throws Exception {
        
        Authority authority;
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();
        if ((authority = method.getAnnotation(Authority.class)) == null) {
            log.error("未获取请求方法的授权注解实例信息({}::{})。", className, methodName);
            throw new NoAuthorityException("无法获取到授权配置，拒绝访问！");
        }
        OptionSystemAuth systemAuth;
        if ((systemAuth = AuthorityParser.getByMethod(className, methodName)) == null) {
            log.error("未获取请求方法的授权注解缓存数据({}::{})。", className, methodName);
            throw new NoAuthorityException("无法获取到授权配置，拒绝访问！");
        }
        
        /* 匿名接口，允许访问 */
        AuthorityScopeType scopeType = null;
        if ((scopeType = authority.value()).equals(AuthorityScopeType.Guest)) {
            return;
        }
        
        /* 否则，匿名用户拒绝 */
        if ((SessionContext.getUserId()) == null) {
            throw new MissingUserException(String.format("接口禁止匿名访问： %s", systemAuth));
        }
        
        /* 提取访问授权标的 */
        Object scopeId = null;
        if (scopeType.checkScopeId()) {
            int paramIndex = authority.paramIndex();
            if (paramIndex < 0 || paramIndex >= args.length
                    || (scopeId = args[paramIndex]) == null
                    || ((scopeId = ClassUtil.getSingltonInstance(authority.parser()).getAuthorityScopeId(scopeId)) == null)) {
                throw new NoAuthorityException("无法获取到授权标的，拒绝访问！");
            }
        }
        if (PermissionService.hasPermission(systemAuth.getAuth(), scopeType, (Long) scopeId)) {
            return;
        }
        throw new NoAuthorityException("授权校验失败，拒绝访问！");
    }
}
