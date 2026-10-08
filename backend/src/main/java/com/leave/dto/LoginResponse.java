package com.leave.dto;

import com.leave.vo.LoginUserInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应：token + 用户信息
 */
@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private LoginUserInfo user;
}
