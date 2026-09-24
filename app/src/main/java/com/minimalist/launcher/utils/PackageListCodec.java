package com.minimalist.launcher.utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Encodes an ordered list of package names as a comma-separated string.
 * Package names cannot contain commas, so no escaping is needed.
 */
public final class PackageListCodec {

    private PackageListCodec() {
    }

    public static String encode(List<String> packages) {
        return String.join(",", dedupe(packages));
    }

    public static List<String> decode(String encoded) {
        List<String> packages = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) {
            return packages;
        }
        for (String part : encoded.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                packages.add(trimmed);
            }
        }
        return dedupe(packages);
    }

    private static List<String> dedupe(List<String> packages) {
        Set<String> seen = new LinkedHashSet<>();
        for (String pkg : packages) {
            if (pkg != null && !pkg.isEmpty()) {
                seen.add(pkg);
            }
        }
        return new ArrayList<>(seen);
    }
}
