package com.workproof.util;

import com.workproof.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Server-side validation is the only validation that counts: anything the browser
 * checks can be bypassed with curl. Every rule used by the registration, project,
 * task and submission forms is pinned down here.
 */
class ValidationUtilTest {

    @Nested
    @DisplayName("required text")
    class RequiredText {

        @Test
        void trimsAndAccepts() {
            assertEquals("Complaint Portal", ValidationUtil.require("  Complaint Portal  ", "Project name"));
        }

        @Test
        void rejectsNullBlankAndTooShort() {
            assertThrows(ValidationException.class, () -> ValidationUtil.require(null, "Project name"));
            assertThrows(ValidationException.class, () -> ValidationUtil.require("   ", "Project name"));
            assertThrows(ValidationException.class,
                    () -> ValidationUtil.require("ab", "Project name", 3, 150));
        }

        @Test
        void rejectsTooLong() {
            assertThrows(ValidationException.class,
                    () -> ValidationUtil.require("x".repeat(151), "Project name", 3, 150));
        }

        @Test
        void optionalReturnsNullForBlank() {
            assertNull(ValidationUtil.optional("   ", 100, "Notes"));
            assertEquals("hello", ValidationUtil.optional("  hello ", 100, "Notes"));
            assertThrows(ValidationException.class, () -> ValidationUtil.optional("x".repeat(101), 100, "Notes"));
        }
    }

    @Nested
    @DisplayName("email")
    class Email {

        @Test
        void lowersTheCaseSoLoginsAreNotCaseSensitive() {
            assertEquals("priya.manager@workproof.local",
                    ValidationUtil.requireEmail("  Priya.Manager@WorkProof.Local "));
        }

        @Test
        void rejectsMalformedAddresses() {
            assertThrows(ValidationException.class, () -> ValidationUtil.requireEmail("not-an-email"));
            assertThrows(ValidationException.class, () -> ValidationUtil.requireEmail("missing@domain"));
            assertThrows(ValidationException.class, () -> ValidationUtil.requireEmail("@workproof.local"));
            assertThrows(ValidationException.class, () -> ValidationUtil.requireEmail(null));
        }
    }

    @Nested
    @DisplayName("password policy")
    class PasswordPolicy {

        @Test
        void acceptsAPasswordWithLettersAndDigits() {
            assertEquals("Member@123", ValidationUtil.requirePassword("Member@123", "Password"));
        }

        @Test
        void rejectsShortPasswords() {
            assertThrows(ValidationException.class, () -> ValidationUtil.requirePassword("Ab1", "Password"));
            assertThrows(ValidationException.class, () -> ValidationUtil.requirePassword("", "Password"));
        }

        @Test
        void rejectsPasswordsWithoutALetterOrWithoutADigit() {
            assertThrows(ValidationException.class, () -> ValidationUtil.requirePassword("12345678", "Password"));
            assertThrows(ValidationException.class, () -> ValidationUtil.requirePassword("passwordonly", "Password"));
        }

        @Test
        void rejectsPasswordsBeyondTheBcryptLimit() {
            assertThrows(ValidationException.class,
                    () -> ValidationUtil.requirePassword("a1".repeat(40), "Password"));
        }

        @Test
        void confirmationMustMatch() {
            ValidationUtil.requireMatch("Member@123", "Member@123", "Passwords do not match.");
            assertThrows(ValidationException.class,
                    () -> ValidationUtil.requireMatch("Member@123", "Member@124", "Passwords do not match."));
            assertThrows(ValidationException.class,
                    () -> ValidationUtil.requireMatch(null, "Member@123", "Passwords do not match."));
        }
    }

    @Nested
    @DisplayName("project code")
    class ProjectCode {

        @Test
        void uppercasesAndAcceptsLettersDigitsHyphens() {
            assertEquals("CMP-2026", ValidationUtil.requireProjectCode(" cmp-2026 "));
            assertEquals("AB", ValidationUtil.requireProjectCode("ab"));
        }

        @Test
        void rejectsSpacesSymbolsAndTooShortValues() {
            assertThrows(ValidationException.class, () -> ValidationUtil.requireProjectCode("A B"));
            assertThrows(ValidationException.class, () -> ValidationUtil.requireProjectCode("CMP_2026"));
            assertThrows(ValidationException.class, () -> ValidationUtil.requireProjectCode("A"));
            assertThrows(ValidationException.class, () -> ValidationUtil.requireProjectCode(null));
        }
    }

    @Nested
    @DisplayName("dates")
    class Dates {

        @Test
        void parsesIsoDatesFromInputTypeDate() {
            assertEquals(LocalDate.of(2026, 10, 5), ValidationUtil.requireDate("2026-10-05", "Start date"));
            assertNull(ValidationUtil.optionalDate("  ", "Due date"));
        }

        @Test
        void rejectsOtherFormatsInsteadOfGuessing() {
            assertThrows(ValidationException.class, () -> ValidationUtil.requireDate("05/10/2026", "Start date"));
            assertThrows(ValidationException.class, () -> ValidationUtil.requireDate("2026-13-01", "Start date"));
            assertThrows(ValidationException.class, () -> ValidationUtil.requireDate(null, "Start date"));
        }

        @Test
        void endDateMayNotPrecedeStartDate() {
            LocalDate start = LocalDate.of(2026, 10, 1);
            LocalDate end = LocalDate.of(2026, 12, 15);

            ValidationUtil.validateDateRange(start, end, "Start date", "End date");
            ValidationUtil.validateDateRange(start, start, "Start date", "End date");   // same day is fine
            assertThrows(ValidationException.class,
                    () -> ValidationUtil.validateDateRange(end, start, "Start date", "End date"));
            assertThrows(ValidationException.class,
                    () -> ValidationUtil.validateDateRange(null, null, "Start date", "End date"));
        }
    }

    @Nested
    @DisplayName("numbers and whitelists")
    class Numbers {

        @Test
        void requireIntRejectsGarbage() {
            assertEquals(42, ValidationUtil.requireInt("42", "Estimate"));
            assertThrows(ValidationException.class, () -> ValidationUtil.requireInt("forty-two", "Estimate"));
            assertThrows(ValidationException.class, () -> ValidationUtil.requireInt(null, "Estimate"));
        }

        @Test
        void optionalIntClampsInsteadOfFailing() {
            assertEquals(1, ValidationUtil.optionalInt(null, 1, 1, 10_000));
            assertEquals(1, ValidationUtil.optionalInt("abc", 1, 1, 10_000));
            assertEquals(1, ValidationUtil.optionalInt("-5", 1, 1, 10_000));
            assertEquals(10_000, ValidationUtil.optionalInt("99999", 1, 1, 10_000));
            assertEquals(3, ValidationUtil.optionalInt("3", 1, 1, 10_000));
        }

        @Test
        void requireOneOfBlocksValuesTheFormNeverOffered() {
            assertEquals("HIGH", ValidationUtil.requireOneOf("HIGH", "Priority", "LOW", "MEDIUM", "HIGH"));
            assertThrows(ValidationException.class,
                    () -> ValidationUtil.requireOneOf("DROP TABLE", "Priority", "LOW", "MEDIUM", "HIGH"));
            assertThrows(ValidationException.class,
                    () -> ValidationUtil.requireOneOf("high", "Priority", "LOW", "MEDIUM", "HIGH"));
        }
    }

    @Nested
    @DisplayName("phone")
    class Phone {

        @Test
        void acceptsCommonFormats() {
            assertEquals("+91 98765 43210", ValidationUtil.optionalPhone("+91 98765 43210"));
            assertEquals("(0294) 123-4567", ValidationUtil.optionalPhone("(0294) 123-4567"));
            assertNull(ValidationUtil.optionalPhone("   "));
        }

        @Test
        void rejectsLetters() {
            assertThrows(ValidationException.class, () -> ValidationUtil.optionalPhone("call me"));
        }
    }
}
