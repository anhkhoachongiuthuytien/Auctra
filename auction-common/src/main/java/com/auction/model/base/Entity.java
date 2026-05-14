package com.auction.model.base;

public abstract class Entity implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private String id;

    public Entity() {}

    public Entity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
}
