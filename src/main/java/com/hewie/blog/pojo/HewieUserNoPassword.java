package com.hewie.blog.pojo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "tb_user")
public class HewieUserNoPassword {

    public HewieUserNoPassword() {

    }

    public HewieUserNoPassword(String id,
                               String userName,
                               String roles,
                               String avatar,
                               String email,
                               String sign,
                               String state,
                               String regIp,
                               String loginIp,
                               Date createTime,
                               Date updateTime,
                               String sex,
                               String workspace,
                               String position,
                               String skills) {
        this.id = id;
        this.userName = userName;
        this.roles = roles;
        this.avatar = avatar;
        this.email = email;
        this.sign = sign;
        this.state = state;
        this.regIp = regIp;
        this.loginIp = loginIp;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.sex = sex;
        this.workspace = workspace;
        this.position = position;
        this.skills = skills;
    }

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "roles")
    private String roles;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "email")
    private String email;

    @Column(name = "sign")
    private String sign;

    @Column(name = "state")
    private String state;

    @Column(name = "reg_ip")
    private String regIp;

    @Column(name = "login_ip")
    private String loginIp;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @Column(name = "sex")
    private String sex = "2";

    @Column(name = "workspace")
    private String workspace;

    @Column(name="position")
    private String position;

    @Column(name="skills")
    private String skills;

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getRegIp() {
        return regIp;
    }

    public void setRegIp(String regIp) {
        this.regIp = regIp;
    }

    public String getLoginIp() {
        return loginIp;
    }

    public void setLoginIp(String loginIp) {
        this.loginIp = loginIp;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
