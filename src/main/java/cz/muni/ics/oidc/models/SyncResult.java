package cz.muni.ics.oidc.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class SyncResult {
    int created = 0;
    int updated = 0;
    int deleted = 0;
    int errors = 0;

    public void incCreated() {
        this.created++;
    }

    public void incUpdated() {
        this.updated++;
    }

    public void incDeleted() {
        this.deleted++;
    }

    public void incDeleted(int amount) {
        this.deleted += amount;
    }

    public void incErrors() {
        this.errors++;
    }
}
