package jang.worktogether.basic.basic_class;


import java.io.Serializable;

public class User implements Serializable {

    private String id;
    private String name;
    private String status;
    private String profile;
    public enum Relation {Friend, Myself, Requester, Requestee, NoRelation}
    private Relation relation;

    public User(String id, String name, String status, String profile, Relation relation){
        this.id = id;
        this.name = name;
        this.status = status;
        this.profile = profile;
        this.relation = relation;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public void setRelation(Relation relation) {
        this.relation = relation;
    }

    public Relation getRelation() {
        return relation;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getProfile() {
        return profile;
    }

    public String getStatus() {
        return status;
    }


}
