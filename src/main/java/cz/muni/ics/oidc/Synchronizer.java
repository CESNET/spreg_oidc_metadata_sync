package cz.muni.ics.oidc;

import cz.muni.ics.oidc.models.SyncResult;
import cz.muni.ics.oidc.props.ConfProperties;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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
    private final ConfProperties confProperties;
    private boolean interactive = false;
    private String test = "test";

    @Autowired
    public Synchronizer(ToPerunSynchronizer toPerunSynchronizer,
                        ToOidcSynchronizer toOidcSynchronizer,
                        ConfProperties confProperties)
    {
        this.toPerunSynchronizer = toPerunSynchronizer;
        this.toOidcSynchronizer = toOidcSynchronizer;
        this.confProperties = confProperties;
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
        if (StringUtils.hasText(confProperties.getProbeOutputFileLocation())) {
            String output;
            if (syncResult.getErrors() > 0) {
                output = "NOK";
            } else {
                output = "OK";
            }
            Date date = new Date(System.currentTimeMillis());
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            output += (';' + format.format(date));
            try (OutputStreamWriter osw = new OutputStreamWriter(
                    new FileOutputStream(confProperties.getProbeOutputFileLocation())))
            {
                osw.write(output);
                osw.flush();
            } catch (IOException e) {
                log.warn("Failed to write output", e);
            }
        }
    }

}
