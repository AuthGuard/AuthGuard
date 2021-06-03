package com.nexblocks.authguard.rest;

import com.google.inject.CreationException;
import com.google.inject.ProvisionException;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.rest.exceptions.InitializationException;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
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
        } catch (final ProvisionException | CreationException e) {
            if (e.getCause() != null) {
                if (e.getCause().getMessage() != null) {
                    log.error("Failed to initialize the server. Error: {}", e.getCause().getMessage());
                } else {
                    log.error("Failed to initialize the server. Error: ", e.getCause());
                }
            } else {
                log.error("Failed to initialize the server. Error: ", e);
            }

            return 1;
        } catch (final ConfigurationException e) {
            log.error("Configuration error: {}", e.getMessage());
            return 2;
        } catch (final InitializationException e) {
            log.error("Failed to initialize the server. Error: {}", e.getMessage());
            return 3;
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
