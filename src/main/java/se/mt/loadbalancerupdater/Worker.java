package se.mt.loadbalancerupdater;

public class Worker {
    private String name;

    private String url;

    public Worker(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

}
