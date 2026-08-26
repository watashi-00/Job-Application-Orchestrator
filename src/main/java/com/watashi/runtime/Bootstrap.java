package com.watashi.runtime;

import java.util.Arrays;

public class Bootstrap {

    private Bootstrap() {}

    public static void start(String... args) {
        if (args != null && args.length > 0 ) {
            // -p <port>
            // -d debug = true
            // -db <port> = database
            System.out.println(String.join(" ", args));
        }
    }

}
