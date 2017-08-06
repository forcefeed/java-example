package ir.forcefeed;

/**
 * @author Mostafa Asgari
 * @since 8/6/17
 */
public class Event {

    private String id;
    private String event;
    private String data;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
