package com.authguard.rest;

import com.authguard.config.ConfigContext;
import com.authguard.rest.exceptions.InitializationException;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthGuardCli {
    private final static Logger log = LoggerFactory.getLogger(AuthGuardCli.class);

    private final ConfigurationLoader configurationLoader;
    private final ServerRunner serverRunner;

    public AuthGuardCli(final ConfigurationLoader configurationLoader, final ServerRunner serverRunner) {
        this.configurationLoader = configurationLoader;
        this.serverRunner = serverRunner;
    }

    public int execute(final String[] args) {
        final Options options = cliOption();

        final CommandLine cmd = parseCommandLineOptions(options, args);

        if (cmd == null) {
            return 1;
        }

        // help
        if (cmd.hasOption("help")) {
            new HelpFormatter().printHelp("authguard", options);
            return 0;
        }

        // config
        final ConfigContext configContext;

        if (cmd.hasOption("config")) {
            configContext = configurationLoader.loadFromFile(cmd.getOptionValue("config"));
        } else {
            configContext = configurationLoader.loadFromResources();
        }

        log.info("Initialized configuration context");
        log.debug("Loaded configuration: {}", configContext);

        // run the server
        try {
            serverRunner.run(configContext);
        } catch (final InitializationException e) {
            System.err.println("Failed to initialize the server. Error: " + e.getMessage());
            return 1;
        }

        return 0;
    }

    private Options cliOption() {
        return new Options()
                .addOption("c", "config", true, "The path to the configuration file")
                .addOption("h", "help", false, "Show help and usage");
    }

    private CommandLine parseCommandLineOptions(final Options options, final String[] args) {
        final DefaultParser parser = new DefaultParser();

        try {
            return parser.parse(options, args);
        } catch (final ParseException e) {
            System.err.println("Invalid arguments: " + e.getMessage());

            new HelpFormatter().printHelp("authguard", options);

            return null;
        }
    }
}
