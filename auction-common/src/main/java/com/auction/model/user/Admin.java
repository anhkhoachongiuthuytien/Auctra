package com.auction.model.user;

public class Admin extends User {
    private String department;

    public Admin(){}

    public Admin(String id, String username, String email){
        super(id, username, email);
    }

    public Admin(String id, String username, String email, String department) {
        super(id, username, email);
        this.department = department;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
