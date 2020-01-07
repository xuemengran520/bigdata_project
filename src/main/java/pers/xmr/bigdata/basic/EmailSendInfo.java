package pers.xmr.bigdata.basic;

import java.util.Properties;

public class EmailSendInfo {
    // 发送邮件的服务器的IP和端口
    private String mailServerHost;
    private String mailServerPort = "25";
    // 邮件发送者的地址
    private String fromAddress;
    // 邮件接收者的地址
    private String toAddress;
    // 邮件抄送者的地址
    private String ccAddress;
    // 登陆邮件发送服务器的用户名和密码
    private String userName;
    private String password;
    // 是否需要身份验证
    private boolean validate = false;
    // 邮件主题
    private String subject;
    // 邮件的文本内容
    private String content;
    // 邮件附件的文件名
    private String[] attachFileNames;

    /**
     * 获得邮件会话属性
     */
    Properties getProperties() {
        Properties p = new Properties();
        p.put("mail.smtp.host", this.mailServerHost);
        p.put("mail.smtp.port", this.mailServerPort);
        p.put("mail.smtp.auth", validate ? "true" : "false");
        return p;
    }

    public String getMailServerHost() {
        return mailServerHost;
    }

    /**
     *
     * @param mailServerHost 邮件服务器ip
     */
    public void setMailServerHost(String mailServerHost) {
        this.mailServerHost = mailServerHost;
    }

    public String getMailServerPort() {
        return mailServerPort;
    }

    /**
     *
     * @param mailServerPort 邮件服务器端口
     */
    public void setMailServerPort(String mailServerPort) {
        this.mailServerPort = mailServerPort;
    }

    boolean isValidate() {
        return validate;
    }

    /**
     *
     * @param validate 是否需要身份验证
     */
    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public String[] getAttachFileNames() {
        return attachFileNames;
    }

    /**
     *
     * @param fileNames 文件名
     */
    public void setAttachFileNames(String[] fileNames) {
        this.attachFileNames = fileNames;
    }

    String getFromAddress() {
        return fromAddress;
    }

    /**
     *
     * @param fromAddress 邮件发送者的地址
     */
    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    String getPassword() {
        return password;
    }

    /**
     *
     * @param password 登陆邮件发送服务器的密码
     */
    public void setPassword(String password) {
        this.password = password;
    }

    String getToAddress() {
        return toAddress;
    }

    /**
     *
     * @param toAddress 邮件接收者的地址
     */
    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    String getUserName() {
        return userName;
    }

    /**
     *
     * @param userName 登陆邮件发送服务器的用户名
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    String getSubject() {
        return subject;
    }

    /**
     *
     * @param subject 邮件主题
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    String getContent() {
        return content;
    }

    /**
     *
     * @param textContent 邮件的文本内容
     */
    public void setContent(String textContent) {
        this.content = textContent;
    }

    String getCcAddress() {
        return ccAddress;
    }

    public void setCcAddress(String ccAddress) {
        this.ccAddress = ccAddress;
    }
}
