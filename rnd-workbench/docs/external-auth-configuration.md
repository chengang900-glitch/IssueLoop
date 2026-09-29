# 外部认证部署配置

外部认证的授权跳转、回调、授权码换 token、用户信息读取、身份绑定和本地 JWT 会话已经包含在应用代码中。部署时不需要修改 Java 或前端代码，只需按实际企业应用填写环境变量，并在对应开放平台登记回调地址。

回调地址统一为：

```text
${EXTERNAL_AUTH_PUBLIC_BASE_URL}/api/v1/auth/{provider}/callback
```

其中 `{provider}` 为 `keycloak`、`feishu`、`dingtalk` 或 `wecom`。如果没有单独填写某个平台的 `*_REDIRECT_URI`，后端会按这个规则生成回调地址。

## 开关与公共地址

```text
EXTERNAL_AUTH_PUBLIC_BASE_URL=https://work.example.com
KEYCLOAK_LOGIN_ENABLED=true|false
FEISHU_LOGIN_ENABLED=true|false
DINGTALK_LOGIN_ENABLED=true|false
WECOM_LOGIN_ENABLED=true|false
```

应用只会在平台开关打开且该平台的参数完整时，把入口返回给登录页。后台管理中的“集成第三方协同 APP 登录”及飞书、钉钉、企微复选框继续控制是否展示协同平台入口。

## Keycloak / 企业统一认证

```text
KEYCLOAK_AUTHORIZATION_URI=https://sso.example.com/realms/{realm}/protocol/openid-connect/auth
KEYCLOAK_TOKEN_URI=https://sso.example.com/realms/{realm}/protocol/openid-connect/token
KEYCLOAK_USER_INFO_URI=https://sso.example.com/realms/{realm}/protocol/openid-connect/userinfo
KEYCLOAK_CLIENT_ID=...
KEYCLOAK_CLIENT_SECRET=...
KEYCLOAK_REDIRECT_URI=https://work.example.com/api/v1/auth/keycloak/callback
KEYCLOAK_SCOPE=openid profile email
```

## 飞书

飞书使用企业自建应用的网页授权流程。需要填写网页授权地址、授权码 token 地址、应用 access token 地址和用户信息地址：

```text
FEISHU_AUTHORIZATION_URI=https://open.feishu.cn/open-apis/authen/v1/authorize
FEISHU_TOKEN_URI=https://open.feishu.cn/open-apis/authen/v1/access_token
FEISHU_APP_ACCESS_TOKEN_URI=https://open.feishu.cn/open-apis/auth/v3/app_access_token/internal
FEISHU_USER_INFO_URI=https://open.feishu.cn/open-apis/authen/v1/user_info
FEISHU_APP_ID=...
FEISHU_APP_SECRET=...
FEISHU_REDIRECT_URI=https://work.example.com/api/v1/auth/feishu/callback
FEISHU_SCOPE=contact:user.base:readonly
FEISHU_USE_PKCE=false
```

## 钉钉

```text
DINGTALK_AUTHORIZATION_URI=...
DINGTALK_TOKEN_URI=...
DINGTALK_USER_INFO_URI=...
DINGTALK_CLIENT_ID=...
DINGTALK_CLIENT_SECRET=...
DINGTALK_REDIRECT_URI=https://work.example.com/api/v1/auth/dingtalk/callback
DINGTALK_SCOPE=openid
```

钉钉授权码交换按 `clientId`、`clientSecret`、`code`、`grantType=authorization_code` 的 JSON 请求发送；用户信息请求使用返回的 access token。

## 企业微信

```text
WECOM_AUTHORIZATION_URI=https://open.weixin.qq.com/connect/oauth2/authorize
WECOM_TOKEN_URI=https://qyapi.weixin.qq.com/cgi-bin/gettoken
WECOM_USER_INFO_URI=https://qyapi.weixin.qq.com/cgi-bin/user/getuserinfo?access_token={access_token}&code={code}
WECOM_CORP_ID=...
WECOM_APP_SECRET=...
WECOM_REDIRECT_URI=https://work.example.com/api/v1/auth/wecom/callback
WECOM_SCOPE=snsapi_base
```

企业微信流程会先用 `corpid`、`corpsecret` 获取 access token，再用授权 code 查询用户身份。部署时仍需在企业微信应用中登记完全一致的 HTTPS 回调地址和可信域名。

## 首次登录行为

外部平台返回的稳定用户标识会保存到 `external_identities`。如果尚未关联本地账号，登录页显示一次“关联已有工作台账号”，通过原有账号密码校验后建立绑定；后续登录直接签发本系统 JWT。系统不会按姓名、邮箱自动合并账号。

密钥只通过部署环境变量或密钥管理系统注入，不写入仓库。启用某个平台前，应先在对应开放平台创建应用并登记回调地址；这属于部署配置，不影响本次代码交付。
