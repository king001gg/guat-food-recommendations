package com.guatfood.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuditAspectSanitizeTest {

    private final AuditAspect aspect = new AuditAspect();

    @Test
    void redactsPasswordAndTokenVariants() {
        assertEquals("password=***&keyword=rice",
                aspect.sanitizeQuery("password=abc&keyword=rice"));
        assertEquals("user[password]=***",
                aspect.sanitizeQuery("user[password]=abc"));
        assertEquals("password[]=***",
                aspect.sanitizeQuery("password[]=abc"));
        assertEquals("oldPassword=***&newPassword=***",
                aspect.sanitizeQuery("oldPassword=a&newPassword=b"));
        assertEquals("token=***",
                aspect.sanitizeQuery("token=a.b.c"));
        assertEquals("pass%77ord=***",
                aspect.sanitizeQuery("pass%77ord=abc"));
        assertEquals("secret_key=***",
                aspect.sanitizeQuery("secret_key=abc"));
        assertEquals("apiKey=***",
                aspect.sanitizeQuery("apiKey=abc"));
    }

    @Test
    void keepsNonSensitiveParametersReadable() {
        assertEquals("keyword=rice&page=1",
                aspect.sanitizeQuery("keyword=rice&page=1"));
        assertEquals("monkey=banana",
                aspect.sanitizeQuery("monkey=banana"));
    }

    @Test
    void handlesEmptyQuery() {
        assertNull(aspect.sanitizeQuery(null));
        assertNull(aspect.sanitizeQuery(""));
        assertNull(aspect.sanitizeQuery("   "));
    }
}
