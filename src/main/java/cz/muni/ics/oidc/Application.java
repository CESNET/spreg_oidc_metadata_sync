package cz.muni.ics.oidc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.util.StringUtils;

@SpringBootApplication
@Slf4j
public class Application implements CommandLineRunner {

    private final Synchronizer synchronizer;

    public static final String INTERACTIVE = "interactive";

    public static final String MODE_TO_OIDC = "TO_OIDC";
    public static final String MODE_TO_PERUN = "TO_PERUN";

    @Autowired
    public Application(Synchronizer synchronizer) {
        this.synchronizer = synchronizer;
    }

    public static void main(String[] args) {
        log.info("Starting application");
        new SpringApplicationBuilder(Application.class)
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .run(args);
        log.info("Closing application");
    }

    @Override
    public void run(String... args) throws Exception {
        int mode = 0;
        if (args != null && args.length > 0) {
            for (String str : args) {
                if (StringUtils.hasText(str) && str.contains("--mode=")) {
                    String modeStr = str.replace("--mode=", "");
                    switch (modeStr.toUpperCase()) {
                        case MODE_TO_OIDC: mode = 0; break;
                        case MODE_TO_PERUN: mode = 1; break;
                        default: mode = -1; break;
                    }
                }
            }
            if (args.length > 1 && INTERACTIVE.equalsIgnoreCase(args[1])) {
                synchronizer.setInteractive(true);
            }
        }
        if (mode == 0) {
            synchronizer.syncToOidc();
        } else if (mode == 1) {
            synchronizer.syncToPerun();
        } else {
            throw new IllegalArgumentException("Unrecognized SYNC mode, valid options are: ");
        }
    }

}
