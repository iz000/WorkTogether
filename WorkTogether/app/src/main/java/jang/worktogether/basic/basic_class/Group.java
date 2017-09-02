package jang.worktogether.basic.basic_class;

public class Group {

    private String id;
    private String name;
    private String content;
    private String chief_id;

    public Group(String id, String name, String content, String chief_id) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.chief_id = chief_id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getChief_id() {
        return chief_id;
    }

    public String getContent() {
        return content;
    }
}
