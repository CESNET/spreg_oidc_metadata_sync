package cz.muni.ics.oidc.models;

public class ResultCounter {

    private int created = 0;
    private int updated = 0;
    private int deleted = 0;
    private int errors = 0;

    public void incCreated() {
        this.created++;
    }

    public void incUpdated() {
        this.updated++;
    }

    public void incDeleted() {
        this.deleted++;
    }

    public void incDeleted(int d) {
        this.deleted += d;
    }

    public void incErrors() {
        this.errors++;
    }

    @Override
    public String toString() {
        return "Results - " +
                "created: " + created +
                ", updated: " + updated +
                ", deleted: " + deleted +
                ", errors: " + errors;
    }

}
