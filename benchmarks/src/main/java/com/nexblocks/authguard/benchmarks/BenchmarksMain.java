package com.nexblocks.authguard.benchmarks;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;

import java.security.Security;

@BenchmarkMode(Mode.All)
public class BenchmarksMain {
    public static void main(String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        org.openjdk.jmh.Main.main(args);
    }
}
