package com.watashi.core.domain.candidate;

import com.watashi.core.domain.common.SalaryRange;
import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.WorkMode;
import java.math.BigDecimal;
import java.util.Set;
import junit.framework.TestCase;

public class CandidatePreferencesTest extends TestCase {

    public void testCandidatePreferencesCreation() {
        SalaryRange salary = new SalaryRange(new BigDecimal("15000"), new BigDecimal("20000"), "BRL");
        CandidatePreferences prefs = new CandidatePreferences(
                salary, Set.of(WorkMode.REMOTE), Set.of(SeniorityLevel.SENIOR), Set.of("Remote"));

        assertEquals(salary, prefs.desiredSalary());
        assertTrue(prefs.preferredWorkModes().contains(WorkMode.REMOTE));
    }
}
