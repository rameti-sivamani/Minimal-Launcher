package com.minimalist.launcher.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class PackageListCodecTest {

    @Test
    public void roundTripKeepsOrder() {
        String encoded = PackageListCodec.encode(Arrays.asList("com.c", "com.a", "com.b"));
        assertEquals("com.c,com.a,com.b", encoded);
        assertEquals(Arrays.asList("com.c", "com.a", "com.b"), PackageListCodec.decode(encoded));
    }

    @Test
    public void decodeHandlesEmptyAndNull() {
        assertTrue(PackageListCodec.decode(null).isEmpty());
        assertTrue(PackageListCodec.decode("").isEmpty());
        assertTrue(PackageListCodec.decode(",, ,").isEmpty());
    }

    @Test
    public void removesDuplicatesKeepingFirst() {
        assertEquals(Arrays.asList("com.a", "com.b"),
                PackageListCodec.decode("com.a, com.b ,com.a"));
        assertEquals("com.a,com.b", PackageListCodec.encode(Arrays.asList("com.a", "com.b", "com.a", "")));
    }
}
