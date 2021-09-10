package com.hewie.blog;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestPasswordEncoder {
    public static void main(String[] args) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode("123456");
        System.out.println("password == >" + encode);

        //验证登录
        String originalPassword = "123456";
        boolean matches = passwordEncoder.matches(originalPassword, "$2a$10$/siAxBTA/5Wyzc5kgZmoZe0W/XdhRuJOrazCKGZEVDVE.sL0bGFre");
        System.out.println("isMatch == >" + matches);
    }
}
