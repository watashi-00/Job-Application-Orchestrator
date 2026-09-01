package com.watashi.core.domain.common;

import java.math.BigDecimal;
import junit.framework.TestCase;

public class ValueObjectsTest extends TestCase {

    public void testSkillCreationAndEquality() {
        Skill s1 = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 5);
        Skill s2 = new Skill("java", SkillCategory.LANGUAGES_FRAMEWORKS, 3);
        assertEquals("java", s1.nameLower());
        assertTrue(s1.matchesName("JAVA"));
    }

    public void testSalaryRangeCoversMinimum() {
        SalaryRange range = new SalaryRange(new BigDecimal("10000"), new BigDecimal("15000"), "BRL");
        assertTrue(range.coversMinimum(new BigDecimal("12000")));
        assertFalse(range.coversMinimum(new BigDecimal("18000")));
    }

    public void testSeniorityLevelDistance() {
        assertEquals(3, SeniorityLevel.INTERN.distanceTo(SeniorityLevel.SENIOR));
        assertEquals(3, SeniorityLevel.SENIOR.distanceTo(SeniorityLevel.INTERN));
        assertEquals(1, SeniorityLevel.INTERN.getLevel());
    }

    public void testSkillDefaultsAndValidations() {
        Skill s = new Skill(" Python ", null, 2);
        assertEquals("Python", s.name());
        assertEquals(SkillCategory.OTHER, s.category());

        try {
            new Skill("", SkillCategory.DATABASE, 1);
            fail("Should throw IllegalArgumentException on empty name");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            new Skill(null, SkillCategory.DATABASE, 1);
            fail("Should throw IllegalArgumentException on null name");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, -1);
            fail("Should throw IllegalArgumentException on negative yearsExperience");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testSalaryRangeDefaultsAndValidations() {
        SalaryRange range = new SalaryRange(new BigDecimal("5000"), new BigDecimal("8000"), null);
        assertEquals("USD", range.currency());

        try {
            new SalaryRange(new BigDecimal("-1000"), new BigDecimal("5000"), "USD");
            fail("Should throw IllegalArgumentException on negative min salary");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            new SalaryRange(new BigDecimal("1000"), new BigDecimal("-5000"), "USD");
            fail("Should throw IllegalArgumentException on negative max salary");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            new SalaryRange(new BigDecimal("10000"), new BigDecimal("5000"), "USD");
            fail("Should throw IllegalArgumentException when min > max salary");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testWorkModeEnum() {
        assertEquals(3, WorkMode.values().length);
        assertEquals(WorkMode.REMOTE, WorkMode.valueOf("REMOTE"));
    }

    public void testNullHandling() {
        try {
            SeniorityLevel.SENIOR.distanceTo(null);
            fail("Should throw IllegalArgumentException on null SeniorityLevel in distanceTo");
        } catch (IllegalArgumentException e) {
            // expected
        }

        Skill skill = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 5);
        assertFalse(skill.matchesName(null));

        SalaryRange range = new SalaryRange(new BigDecimal("10000"), new BigDecimal("15000"), "USD");
        assertTrue(range.coversMinimum(null));

        SalaryRange openEndedRange = new SalaryRange(new BigDecimal("10000"), null, "USD");
        assertTrue(openEndedRange.coversMinimum(new BigDecimal("20000")));
        assertTrue(openEndedRange.coversMinimum(null));
    }
}
