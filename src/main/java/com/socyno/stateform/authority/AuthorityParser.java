package com.socyno.stateform.authority;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.socyno.base.bscmixutil.StringUtils;
import com.socyno.base.bscmodel.ObjectMap;
import com.socyno.base.bscsqlutil.AbstractDao;
import com.socyno.base.bscsqlutil.AbstractDao.ResultSetProcessor;
import com.socyno.stateform.field.OptionSystemAuth;
import com.socyno.stateform.field.OptionSystemAuth.AuthType;
import com.socyno.base.bscsqlutil.SqlQueryUtil;
import com.socyno.webbsc.ctxutil.ContextUtil;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class AuthorityParser implements ApplicationListener<ContextRefreshedEvent> {
	
	private String checkedClassStartsWith = "";
    
    private final static Map<String, OptionSystemAuth> authorities = new ConcurrentHashMap<>();
    private final static Map<String, OptionSystemAuth> authoritiesByMethod = new ConcurrentHashMap<>();
    
    public void onApplicationEvent(ContextRefreshedEvent event)  {
        // 判断SPRING容器是否加载完成
        if (event.getApplicationContext().getParent() == null) {
            parseAndCheckAuthority(event.getApplicationContext());
        }
    }
    
    public static OptionSystemAuth getByMethod(String clazz, String method) {
        return authoritiesByMethod.get(String.format("%s::%s", clazz, method));
    }
    
    private static AbstractDao getDao() {
        return ContextUtil.getBaseDataSource();
    }
    
    private void parseAndCheckAuthority(ApplicationContext context) {
        /* 获取所有注册接口信息 */
        Map<RequestMappingInfo, HandlerMethod> requestMappings = context.getBean(RequestMappingHandlerMapping.class)
                                                    .getHandlerMethods();
        
        List<String> errorMessages = new ArrayList<>();
		for (Map.Entry<RequestMappingInfo, HandlerMethod> mapping : requestMappings.entrySet()) {
			Method method = mapping.getValue().getMethod();
			String methodClass = method.getDeclaringClass().getName();
			if (StringUtils.isNotBlank(getCheckedClassStartsWith())
					&& !methodClass.startsWith(getCheckedClassStartsWith())) {
				continue;
			}
            String controllerMethod = String.format("%s::%s", methodClass, method.getName());
            Authority authority;
            if ((authority = method.getAnnotation(Authority.class)) == null || (authority.value().checkScopeId()
                    && AuthorityScopeIdNoopParser.class.equals(authority.parser()))) {
                errorMessages.add(String.format("接口(%s)未明确定义授权信息, 或未实现 parser 解析器。", controllerMethod));
                continue;
            }
            String scopeType = authority.value().name();
            RequestMappingInfo info = mapping.getKey();
            PatternsRequestCondition p = info.getPatternsCondition();
            
            StringBuilder requestMethods = new StringBuilder();
            RequestMethodsRequestCondition methodsCondition = info.getMethodsCondition();
            for (RequestMethod requestMethod : methodsCondition.getMethods()) {
                requestMethods.append(requestMethod.toString()).append(",");
            }
            if (requestMethods.length() <= 0) {
                errorMessages.add(String.format("接口(%s/%s)未明确定义请求方法。",
                                            methodClass, method.getName()));
                continue;
            }
            for (String url : p.getPatterns()) {
                OptionSystemAuth auth = new OptionSystemAuth();
                auth.setAuth(url);
                auth.setType(AuthType.Interfaze.getCode());
                auth.setScopeType(scopeType);
                auth.setControllerMethod(controllerMethod);
                auth.setRequestMethods(requestMethods.toString());
                if (authorities.containsKey(url)) {
                    OptionSystemAuth duplicated = authorities.get(url);
                    errorMessages.add(String.format("接口(%s)声明的访问地址与接口（%s）重名。",
                            controllerMethod, duplicated.getControllerMethod()));
                    continue;
                }
                authorities.put(url, auth);
                authoritiesByMethod.put(controllerMethod, auth);
            }
            try {
                AbstractDao dao = getDao();
                dao.executeTransaction(new ResultSetProcessor() {
                    @Override
                    public void process(ResultSet arg0, Connection arg1) throws Exception {
                        dao.executeUpdate("UPDATE system_interfaze SET deleted_at = NOW()"
                                                + " WHERE deleted_at IS NULL");
                        List<ObjectMap> kvPairs = new ArrayList<>();
                        for (OptionSystemAuth auth : authorities.values()) {
                            kvPairs.add(new ObjectMap().put("auth", auth.getAuth())
                                    .put("scope_type", auth.getScopeType())
                                    .put("request_methods", auth.getRequestMethods())
                                    .put("deleted_at", null));
                        }
                        dao.executeUpdate(SqlQueryUtil.pairs2InsertQuery(
                                "system_interfaze", kvPairs,
                                new ObjectMap().put("?scope_type", "scope_type")
                                    .put("?request_methods", "request_methods")
                                    .put("deleted_at", null)));
                    }
                });
            } catch(Exception e) {
                errorMessages.add(StringUtils.stringifyStackTrace(e));
            }
        }
        
        if (errorMessages.size() > 0) {
            throw new Error(StringUtils.join(errorMessages.toArray(), "\t\n"));
        }
    }
}
