package com.auction.model.user;
import com.auction.model.base.Entity;

public abstract class User extends Entity {
    private String username;
    private String email;
    private String avatarPath;

    public User() {}

    public User(String id, String username, String email) {
        super(id);
        this.username = username;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }
}
