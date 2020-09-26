package com.authguard.rest;

public class Application {
    public static void main(final String[] args) {
        final int exitCode = new AuthGuardCli(new ConfigurationLoader(), new ServerRunner())
                .execute(args);

        // if the status code is 0 then the Javalin server started correctly and we leave it be
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
