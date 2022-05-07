package com.nexblocks.authguard.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;

class JwtConfigParserTest {

    @Test
    void parseHmac256() {
        final String privateKeyPath = "file:src/test/resources/hmac256.pem";

        final Algorithm algorithm = JwtConfigParser.parseAlgorithm("HMAC256", null, privateKeyPath);

        final String jwt = JWT.create().withClaim("claim", "value").sign(algorithm);

        algorithm.verify(JWT.decode(jwt));
    }

    @Test
    void parseHmac256NoFile() {
        final String privateKeyPath = "Rkt2TDExREk1cndndDUxUWdCM0NWb2Izb1dZckZOQnpGMFJPdnU5WWFqOA==";

        final Algorithm algorithm = JwtConfigParser.parseAlgorithm("HMAC256", null, privateKeyPath);

        final String jwt = JWT.create().withClaim("claim", "value").sign(algorithm);

        algorithm.verify(JWT.decode(jwt));
    }

    @Test
    void parseRsa512() {
        final String publicKeyPath = "file:src/test/resources/rsa512-public.pem";
        final String privateKeyPath = "file:src/test/resources/rsa512-private.pem";

        final Algorithm algorithm = JwtConfigParser.parseAlgorithm("RSA512", publicKeyPath, privateKeyPath);

        final String jwt = JWT.create().withClaim("claim", "value").sign(algorithm);

        algorithm.verify(JWT.decode(jwt));
    }

    @Test
    void parseRsa512NoFile() {
        final String publicKeyPath = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAo4+hlvEGVdSHRV4M61HD" +
                "58oF4h6sZ5vbFc32PuS46Fr1mSf41ywnuXdMYhjy6G0FUtDItZAXJwEU/Dw79azC" +
                "YD4+xB7dHzazxH7mRQuQg4XG/4sF0RxSMqMbTRbK8a8b5Sh4vLt4vyRKES6+gNRc" +
                "K50CQIxTdyGNHUJUqa9SMk6eQno7eK5RKAGhRYCIT9jHF6U9UQVquer6wmXk0mxq" +
                "8Az4s2RCuyfU0RgHH13WFpuL/R0nrRvjs9E7ZHCwRPLN2DVcVgLccJQVIgxd8x1L" +
                "1/WKqxhSj45xSGFlPFk/701x65lGsKB+ZEX/DyZ+5MBC7QRIAm/g24MNwYxkCa+d" +
                "N4OAUnCkkp21SBMe8/aNNXctF1lKvljh1eW8HtznK0P2ZmL4yUOcfW9thLKl/a/R" +
                "htoONpt4i7E+cYJ/XnPY/47dcK5qIEwVH6J2m0/PMPOypFFwX6Uw09JzoEIXzlpY" +
                "n3QWnO4UPeWrUYGCj3K6mGcQFeHsZpMUVOw2sK2Ca0Mh9J8MKSdl+FTShH1dZEUM" +
                "gLfyrexdDb1Y8RFU+MEwFsGGAPZPh/DOCDNr3ewvwqkx+m6wwZvJMH6s0cU3pG0N" +
                "qEDugZRhffpQ6MpG4F9DkpQnvEFi1yNoWSbP0RWlUraUi97Mgwmiyq5u2Avai6AA" +
                "A7rpbu2cXQ1GbQwTEAkw+SUCAwEAAQ==";
        final String privateKeyPath = "MIIJQgIBADANBgkqhkiG9w0BAQEFAASCCSwwggkoAgEAAoICAQCjj6GW8QZV1IdF" +
                "XgzrUcPnygXiHqxnm9sVzfY+5LjoWvWZJ/jXLCe5d0xiGPLobQVS0Mi1kBcnART8" +
                "PDv1rMJgPj7EHt0fNrPEfuZFC5CDhcb/iwXRHFIyoxtNFsrxrxvlKHi8u3i/JEoR" +
                "Lr6A1FwrnQJAjFN3IY0dQlSpr1IyTp5Cejt4rlEoAaFFgIhP2McXpT1RBWq56vrC" +
                "ZeTSbGrwDPizZEK7J9TRGAcfXdYWm4v9HSetG+Oz0TtkcLBE8s3YNVxWAtxwlBUi" +
                "DF3zHUvX9YqrGFKPjnFIYWU8WT/vTXHrmUawoH5kRf8PJn7kwELtBEgCb+Dbgw3B" +
                "jGQJr503g4BScKSSnbVIEx7z9o01dy0XWUq+WOHV5bwe3OcrQ/ZmYvjJQ5x9b22E" +
                "sqX9r9GG2g42m3iLsT5xgn9ec9j/jt1wrmogTBUfonabT88w87KkUXBfpTDT0nOg" +
                "QhfOWlifdBac7hQ95atRgYKPcrqYZxAV4exmkxRU7DawrYJrQyH0nwwpJ2X4VNKE" +
                "fV1kRQyAt/Kt7F0NvVjxEVT4wTAWwYYA9k+H8M4IM2vd7C/CqTH6brDBm8kwfqzR" +
                "xTekbQ2oQO6BlGF9+lDoykbgX0OSlCe8QWLXI2hZJs/RFaVStpSL3syDCaLKrm7Y" +
                "C9qLoAADuulu7ZxdDUZtDBMQCTD5JQIDAQABAoICAFP3LFE8sq/lRvxJaAB2HUgk" +
                "4hhO/trBnBr5fOyUpohCCcryRkDQHiSMJd5GSI0hSpZVMHRk4D3ZxFgo4+8fHToj" +
                "Oj2cSo/3mRnKu5O+eBXM23fcesP68gekzCMrDEw+ROfDexgIddhKXOutP4cLfbW2" +
                "CK3yW/bQdo0KvEPQCTZiPcSK03UEqA0NqSjw7wluk+aDoZKyYTPIearKfEm59rv9" +
                "LL+LPOavcAVXfHCRz5ITkC4EhZXMt3xccU0CvLromtvfqONO3LO+kYrFJoXkCEd5" +
                "ehKTje68hVNPDJWKi9PhwhXcvfl2quv3MxUoAgSU3samaAE4RgrqoGk4FTMTwHUp" +
                "JpUve7z0E/WIV5qYfFQZ+MCMRNLiDFomozX/3+NZLzTXVGC2J/Mr+3l+5L6dtD2y" +
                "6zkqUkUP1VAtQ60HePbzRdmIb5UVbhTbyJS/YJzUeQ9dfY+94zPC5tYicqIf2GKP" +
                "WxSD4lV6Yc/svSTXepa+wnUOxQlITfzKASu0TLgi5aAQW2L5zxR1Q9b0WwF4tgq0" +
                "y/pPwMmG7AtG4kQBTUvXal9An/TqNJYcbl1Ri/4Y4kKNY0E5dAmwKzkiF7yWLWIB" +
                "qkcjOe7qELKW0ZPFo//Ozld9O0c6fmN5FgvXSzZ3Kjmn5/syLZjcbxOZp+kEzhG8" +
                "eAeAFXCXuUiJCrlQwQSRAoIBAQDOU27kvkyTuAt41l3S7VYm7605cMq1ZwvbXjy8" +
                "GIJP0Q4EACgy+XFHzut2QI+9qPR2QxIFgiRQm33pLlpl1TKNibCKWZxteOYZ2/lK" +
                "OQhrVnCKZWgkeDst0ybY7nqPva15acidSJeKlkuWDxJTYQ8NHcnetIQoWxCFdDvh" +
                "+HGRciCfYXIyFf/n12aDcahmacaj2k+uRHjUSIuAiJXrmb7xiktNNVRIEFCwjE8D" +
                "ag+QN/9bX3atjBbIY0rhbXmJOcTKg85z7MwnGzFI1ECFs68g3bovi53pRPce3pu/" +
                "d8saQBMzk5vANmfoKr08mv/QMXpGTk/sw+l6lRRtbrq+CG8HAoIBAQDK8HR39Oj/" +
                "HdJgqK6b+lrl28FwQPRoeCuQ8iJDtBCFN2iyLMQrUxTEzZS+Zza3PhB+cuB86u8b" +
                "hynBhiPlyxRYnMqnin4MbFyFbX3k6wNkXrrN2oC2yLcIcDHA/q6A1sPHYJ4lla0E" +
                "Ee523zDf7L16gRM9TinXPBrLhPY6x9GU+NKFzdAW7cvF4hjFKOWhZsWP1DKRh2QA" +
                "XceUKkKbBJMgn2gGbRlBH9dXAspKjeHPh+jgkQGqO/sXbtzFaFrvMiYA7i22f7JA" +
                "S+4sSR625ZYPUd+mQfXT3B3Tw0XadhtkMzqINUIIEg7gYYGG0EAkPn4su3mkT9kZ" +
                "irhyLhm70N9zAoIBAAWviujGzblsYvSLg75iR/N+u1lP4GYrVspOyIGuczjb2/UE" +
                "RBdThGmkLBzwHoXtd/8iTgbU1UdbYZbhbiBMRb5cwv6tBYCGymCADVicRb0ffq5x" +
                "qPMIVSkoHnPv7nSzl2o3Hg4nh/WLur2B4NFnZVDJN1zpwJKUH9ptn5DUldyaNoft" +
                "2YXD0W+EIhEROsDHvW+afoOg42uGLEH4gZkifX6OfxC7nXz25iJXW34OmCszP9g2" +
                "w0B5Ec+n3NJlf+nmK6QhCblsFxwkxbDqGHUWxIa1VYIl6M4a1myFWelm1bP9fAn5" +
                "0Vr0pNxjASAAbXhBRMtXMSCnLXHxVcTGPmMqPFUCggEAR9vZAdjg3+UJZ1yTZ3vA" +
                "z/9+gWcepBdZJdv3Mg4Cg4lZMy4S0Fx80CsTblBR2ZTKdlvrDZCK1i6IJJSBBY9h" +
                "RvdN8wFhHLkKEdzxZSuqadH5R4cFaLOty/keRP5pgAmMDX8ywJA9UWGgFMt3HPNR" +
                "LJ2j2GNjAWmw+zu7jJjQp3Vr5iE4e8X8D7e9maKfnQUtE37J4SoVyONsFhTYvNdj" +
                "2XtYdF4RQTQrJg5A3yFaQggX99ygwEy43lVNK0GGYYhaWJ395c9VqNq6HUhO7ehh" +
                "uE7/aHmWuEwK678Lbw2/KT3SjgR9uynZxq4AFWKMM8lFGEXSDtKPRzINmbClQBH3" +
                "7QKCAQEAjoZyHh1U3rWrmewbD/iN6IcaTD9j3TN09VDozD3laqmRTizq1cWcgej+" +
                "uTz3BRmPRihiTyhC3fd1IrfrUAvp6b0mBVxxjme7tQxKsQJuy1W9b+OHbNQDIpxp" +
                "YKzohWZip13zPxIZj3lG5Yf2s+dFsAj0so8yzgIJWAkL8Q1S/rXRJBVSwQqmlwYO" +
                "npEQnjlk7WzWCsQqxp77+7n5HvPAqN3BNoverxa4wMOUnRvr7VdRxvyqR19jv4eM" +
                "ydZKkhTEJk5/ns7lXWWcwbSw8tYNAJNdVylshFeEQa3UhcA4r2HklmMJS51nw/Lg" +
                "zz0xHBD6hjp0F987aZSDZIPjZjEa0A==";

        final Algorithm algorithm = JwtConfigParser.parseAlgorithm("RSA512", publicKeyPath, privateKeyPath);

        final String jwt = JWT.create().withClaim("claim", "value").sign(algorithm);

        algorithm.verify(JWT.decode(jwt));
    }

    @Test
    void parseEc256() {
        final String publicKeyPath = "file:src/test/resources/ec256-public.pem";
        final String privateKeyPath = "file:src/test/resources/ec256-private.pem";

        final Algorithm algorithm = JwtConfigParser.parseAlgorithm("EC256", publicKeyPath, privateKeyPath);

        final String jwt = JWT.create().withClaim("claim", "value").sign(algorithm);

        algorithm.verify(JWT.decode(jwt));
    }

    @Test
    void parseEc256NoFile() {
        final String publicKeyPath = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEegUJ+njnR3jvwvyVLm3sOzOCxssp" +
                "4y1Sk5pb+7tLRUDpeGCmJf+P4xv3nDp+8CUq0bdSc8iVHQRotJ4AJrmMlQ==";
        final String privateKeyPath = "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg8WxnDc0Hdg6yUXiV" +
                "pC4/MJDZeav+iyPKgF2pNGCqSwuhRANCAAR6BQn6eOdHeO/C/JUubew7M4LGyynj" +
                "LVKTmlv7u0tFQOl4YKYl/4/jG/ecOn7wJSrRt1JzyJUdBGi0ngAmuYyV";

        final Algorithm algorithm = JwtConfigParser.parseAlgorithm("EC256", publicKeyPath, privateKeyPath);

        final String jwt = JWT.create().withClaim("claim", "value").sign(algorithm);

        algorithm.verify(JWT.decode(jwt));
    }
}