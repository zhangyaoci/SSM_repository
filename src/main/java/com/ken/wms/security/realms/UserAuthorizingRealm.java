package com.ken.wms.security.realms;

import com.ken.wms.domain.UserInfoDTO;
import com.ken.wms.exception.UserInfoServiceException;
import com.ken.wms.security.service.Interface.UserInfoService;
import com.ken.wms.security.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;

/**
 * 用户的认证与授权
 *
 */
public class UserAuthorizingRealm extends AuthorizingRealm {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 对用户进行角色授权
     *
     * @param principalCollection 用户信息
     * @return 返回用户授权信息
     */
    @SuppressWarnings("unchecked")
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        // 创建存放用户角色的 Set
        Set<String> roles = new HashSet<>();

        System.out.println("用户的角色设定");

        //获取用户角色
        Subject currentSubject = SecurityUtils.getSubject();
        Session session = currentSubject.getSession();
        UserInfoDTO userInfo = (UserInfoDTO) session.getAttribute("userInfo");
        roles.addAll(userInfo.getRole());

        return new SimpleAuthorizationInfo(roles);
    }

    /**
     * 对用户进行认证
     *
     * @param authenticationToken 用户凭证
     * @return 返回用户的认证信息
     * @throws AuthenticationException 用户认证异常信息
     */
    @SuppressWarnings("unchecked")
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws
            AuthenticationException {

        String realmName = getName();
        String credentials = "";

        System.out.println("@@@@@用户认证和授权完毕+realName"+realmName);
        System.out.println("进行用户的认证");


        try {
            // 获取用户名对应的用户账户信息
            UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) authenticationToken;

            //获取唯一标示
            String principal = usernamePasswordToken.getUsername();

            if (!StringUtils.isNumeric(principal))
                throw new AuthenticationException();

            Integer userID = Integer.valueOf(principal);

            //从数据库中添加
            UserInfoDTO userInfoDTO = userInfoService.getUserInfo(userID);

            if (userInfoDTO != null) {
                Subject currentSubject = SecurityUtils.getSubject();
                Session session = currentSubject.getSession();

                // 设置用户信息到 Session
                session.setAttribute("userInfo", userInfoDTO);

                // 结合验证码对密码进行处理
                String checkCode = (String) session.getAttribute("checkCode");
                String password = userInfoDTO.getPassword();
                if (checkCode != null && password != null) {
                    System.out.println("从数据库中得到的密码"+password);
                    checkCode = checkCode.toUpperCase();
                    credentials = MD5Util.MD5(password + checkCode);
                }

                // 清除 session 中的 userInfo 密码敏感信息
                userInfoDTO.setPassword(null);
            }

            // 返回封装的认证信息
            return new SimpleAuthenticationInfo(principal, credentials, realmName);

        } catch (UserInfoServiceException e) {
            throw new AuthenticationException();
        }
    }
}
