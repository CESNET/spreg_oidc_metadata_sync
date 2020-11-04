package cz.muni.ics.oidc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
@Slf4j
public class Application implements CommandLineRunner {

    private final Synchronizer synchronizer;

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
        synchronizer.sync();
    }

}
