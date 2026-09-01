package com.watashi.core.domain.discovery;

import junit.framework.TestCase;

public class JobQueryTest extends TestCase {

    public void testNullQueryAndCategory() {
        JobQuery query = new JobQuery(null, null, 10);
        assertEquals("", query.query());
        assertEquals("", query.category());
        assertEquals(10, query.limit());
    }

    public void testLimitFallbackForZeroAndNegative() {
        JobQuery queryZero = new JobQuery("java", "dev", 0);
        assertEquals(50, queryZero.limit());

        JobQuery queryNegative = new JobQuery("java", "dev", -10);
        assertEquals(50, queryNegative.limit());
    }

    public void testOfSoftwareDevFactory() {
        JobQuery query = JobQuery.ofSoftwareDev();
        assertEquals("", query.query());
        assertEquals("software-dev", query.category());
        assertEquals(50, query.limit());
    }
}
