package com.socyno.stateform.service;

import java.util.Collections;
import java.util.List;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.socyno.base.bscsqlutil.AbstractDao;
import com.socyno.webbsc.ctxutil.ContextUtil;

public class FeatureBasicService {
    
    private static AbstractDao getDao() {
        return ContextUtil.getBaseDataSource();
    }
    
    
    /**
     * SELECT DISTINCT
     *     a.feature_id 
     * FROM
     *     system_feature_auth a,
     *     system_tenant_feature f,
     *     system_tenant t
     * WHERE
     *     f.feature_id = a.feature_id
     * AND
     *     t.id = f.tenant_id
     * AND
     *     t.code = ?
     * AND
     *     a.auth_key IN (%s)
     */
    @Multiline
    private static final String SQL_QUERY_AUTH_FEATURES = "X";
    
    /**
     * 包含指定接口或操作的功能列表
     */
    public static List<Long> getAuthTenantFeatures(String tenant, String ...authKeys) throws Exception {
        if (authKeys == null || authKeys.length <= 0 || StringUtils.isBlank(tenant)) {
            return Collections.emptyList();
        }
        return getDao().queryAsList(Long.class,
                String.format(SQL_QUERY_AUTH_FEATURES, StringUtils.join("?", authKeys.length, ",")),
                ArrayUtils.addAll(new Object[] { tenant }, (Object[])authKeys));
    }
    
    /**
     * SELECT DISTINCT
     *     f.feature_id 
     * FROM
     *     system_feature f,
     *     system_tenant  t,
     *     system_tenant_feature tf
     * WHERE
     *     t.id = tf.tenant_id
     * AND
     *     f.id = tf.feature_id
     * AND
     *     t.code = ?
     */
    @Multiline
    private static final String SQL_QUERY_TENANT_FEATURES = "X";
    
    /**
     * 获取租户的功能清单
     */
    public static List<Long> getTenantFeatures(String tenant) throws Exception {
        if (StringUtils.isBlank(tenant)) {
            return Collections.emptyList();
        }
        return getDao().queryAsList(Long.class, SQL_QUERY_TENANT_FEATURES, new Object[] { tenant });
    }
    
    /**
     * SELECT DISTINCT
     *     a.auth_key 
     * FROM
     *     system_feature f,
     *     system_tenant  t,
     *     system_tenant_feature tf,
     *     system_feature_auth a
     * WHERE
     *     t.id = tf.tenant_id
     * AND
     *     f.id = tf.feature_id
     * AND
     *     a.feature_id = f.id
     * AND
     *     t.code = ?
     */
    @Multiline
    private static final String SQL_QUERY_TENANT_AUTHS = "X";
    
    /**
     * 获取租户的在给定功能中的所有接口清单 
     */
    public static List<String> getTenantAuths(String tenant, Long... features) throws Exception {
        if (StringUtils.isBlank(tenant) || features == null || features.length <= 0) {
            return Collections.emptyList();
        }
        return getDao().queryAsList(String.class,
                String.format("%s AND tf.feature_id IN (%s)", SQL_QUERY_TENANT_AUTHS,
                        StringUtils.join("?", features.length, ",")),
                ArrayUtils.addAll(new Object[] { tenant }, (Object[])features));
    }

    /**
     * 获取租户的所有授权操作清单 
     */
    public static List<String> getTenantAllAuths(String tenant) throws Exception {
        if (StringUtils.isBlank(tenant)) {
            return Collections.emptyList();
        }
        return getDao().queryAsList(String.class, SQL_QUERY_TENANT_AUTHS, new Object[] { tenant });
    }
    
    /**
     * 获取租户下是否拥有指定的授权操作
     */
    public static boolean checkTenantAuth(String tenant, String authKey) throws Exception {
        if (StringUtils.isBlank(tenant) || StringUtils.isBlank(authKey)) {
            return false;
        }
        return getDao().queryAsList(String.class,
                String.format("%s AND a.auth_key = ?", SQL_QUERY_TENANT_AUTHS),
                new Object[] { tenant, authKey }).size() > 0;
    }
}
