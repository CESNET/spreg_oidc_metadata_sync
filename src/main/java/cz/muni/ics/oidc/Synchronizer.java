package cz.muni.ics.oidc;

import cz.muni.ics.oidc.models.SyncResult;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Setter
public class Synchronizer {

    public static final String YES = "yes";
    public static final String Y = "y";
    public static final String DO_YOU_WANT_TO_PROCEED = "Do you want to proceed? (YES/no)";
    public static final String SPACER = "--------------------------------------------";

    private final ToPerunSynchronizer toPerunSynchronizer;
    private final ToOidcSynchronizer toOidcSynchronizer;
    private boolean interactive = false;

    @Autowired
    public Synchronizer(ToPerunSynchronizer toPerunSynchronizer, ToOidcSynchronizer toOidcSynchronizer) {
        this.toPerunSynchronizer = toPerunSynchronizer;
        this.toOidcSynchronizer = toOidcSynchronizer;
    }

    public void syncToPerun() {
        log.info("Started synchronization to PERUN");
        SyncResult syncResult = toPerunSynchronizer.syncToPerun(interactive);
        log.info("Finished syncing TO PERUN:\n Created {}, Updated: {}, Deleted {}, errors: {}",
                syncResult.getCreated(), syncResult.getUpdated(), syncResult.getDeleted(), syncResult.getErrors());
    }

    public void syncToOidc() {
        log.info("Started synchronization to OIDC DB");
        SyncResult syncResult = toOidcSynchronizer.syncToOidc(interactive);
        log.info("Finished syncing TO OIDC:\n Created {}, Updated: {}, Deleted {}, errors: {}",
                syncResult.getCreated(), syncResult.getUpdated(), syncResult.getDeleted(), syncResult.getErrors());
    }

}
