package com.bigo.project.bigo.captcha.enums;

public enum EmailConfEnum {


    EMAIL_01("noreply@red-carpet.cc","mietczxyflfavvpq","RedCarpet Exchange"),
    EMAIL_02("no_reply@red-carpet.cc","frmguonjyxrkmjsp","RedCarpet Exchange"),
    EMAIL_03("do-not-reply@red-carpet.cc","bwadejueyscvnner","RedCarpet Exchange"),
    EMAIL_04("not-reply@red-carpet.cc","lcfhadecrbfugltu","RedCarpet Exchange"),
    ;

    /**
     * 变更类型
     */
    private String userName;
    /**
     * 变更类型名称
     */
    private String password;

    private String sendName;

    EmailConfEnum(String userName, String password, String sendName){
        this.userName = userName;
        this.password = password;
        this.sendName = sendName;
    }

    public String getUserName() {
        return userName;
    }



    public String getPassword() {
        return password;
    }

    public String getSendName() {
        return sendName;
    }

    public void setSendName(String sendName) {
        this.sendName = sendName;
    }

    public static EmailConfEnum randomUser(EmailConfEnum[] values){
        return values[(int)(Math.random()*values.length)];
    }
}
