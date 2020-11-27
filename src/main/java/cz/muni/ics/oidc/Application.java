package cz.muni.ics.oidc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@SpringBootApplication
@Slf4j
public class Application implements CommandLineRunner {

    private final Synchronizer synchronizer;

    public static final String PARAM_MODE = "--mode=";
    public static final String PARAM_INTERACTIVE = "--interactive=";

    public static final String VAL_TRUE = "TRUE";
    public static final String VAL_MODE_TO_OIDC = "TO_OIDC";
    public static final String VAL_MODE_TO_PERUN = "TO_PERUN";

    public static final String[] MODES = { VAL_MODE_TO_OIDC, VAL_MODE_TO_PERUN };

    public static final int MODE_UNKNOWN = -1;
    public static final int MODE_TO_OIDC = 1;
    public static final int MODE_TO_PERUN = 2;

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
                if (!StringUtils.hasText(str)) {
                    continue;
                }
                if (str.startsWith(PARAM_MODE)) {
                    mode = processModeParam(str);
                } else if (str.startsWith(PARAM_INTERACTIVE)) {
                    processInteractiveParam(str);
                }
            }
        }
        if (mode == MODE_TO_OIDC) {
            synchronizer.syncToOidc();
        } else if (mode == MODE_TO_PERUN) {
            synchronizer.syncToPerun();
        } else {
            throw new IllegalArgumentException("Unrecognized SYNC mode, valid options are: " + Arrays.toString(MODES));
        }
    }

    private void processInteractiveParam(String str) {
        String interactiveStr = str.replace(PARAM_INTERACTIVE, "");
        synchronizer.setInteractive(VAL_TRUE.equalsIgnoreCase(interactiveStr));
    }

    private int processModeParam(String param) {
        String modeStr = param.replace(PARAM_MODE, "");
        switch (modeStr.toUpperCase()) {
            case VAL_MODE_TO_OIDC:
                return MODE_TO_OIDC;
            case VAL_MODE_TO_PERUN:
                return MODE_TO_PERUN;
            default:
                return MODE_UNKNOWN;
        }
    }

}
