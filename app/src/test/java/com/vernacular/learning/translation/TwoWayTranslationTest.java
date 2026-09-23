package com.vernacular.learning.translation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.vernacular.learning.utils.TwoWayTranslationHelper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class TwoWayTranslationTest {

    @Test
    public void testTeacherHindiToStudentSanthali() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TwoWayTranslationHelper.TranslationResult> resultRef = new AtomicReference<>();

        TwoWayTranslationHelper.translateTeacherToSanthali(null, "एक, दो, तीन", result -> {
            resultRef.set(result);
            latch.countDown();
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        TwoWayTranslationHelper.TranslationResult result = resultRef.get();
        assertNotNull(result);
        assertTrue(result.isSuccess);
        assertEquals("ᱢᱤᱫ, ᱵᱟᱨ, ᱯᱮ", result.translatedText);
        assertEquals("एक, दो, तीन", result.originalText);
    }

    @Test
    public void testStudentSanthaliToTeacherHindi() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TwoWayTranslationHelper.TranslationResult> resultRef = new AtomicReference<>();

        TwoWayTranslationHelper.translateStudentToHindi(null, "ᱢᱤᱫ, ᱵᱟᱨ, ᱯᱮ", result -> {
            resultRef.set(result);
            latch.countDown();
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        TwoWayTranslationHelper.TranslationResult result = resultRef.get();
        assertNotNull(result);
        assertTrue(result.isSuccess);
        assertEquals("एक, दो, तीन", result.translatedText);
        assertEquals("ᱢᱤᱫ, ᱵᱟᱨ, ᱯᱮ", result.originalText);
    }

    @Test
    public void testClassroomGreetingsBidirectional() throws InterruptedException {
        // Teacher greeting
        CountDownLatch latch1 = new CountDownLatch(1);
        AtomicReference<TwoWayTranslationHelper.TranslationResult> res1 = new AtomicReference<>();
        TwoWayTranslationHelper.translateTeacherToSanthali(null, "नमस्ते", r -> {
            res1.set(r);
            latch1.countDown();
        });
        assertTrue(latch1.await(3, TimeUnit.SECONDS));
        assertTrue(res1.get().isSuccess);
        assertEquals("ᱡᱚᱦᱟᱨ", res1.get().translatedText);

        // Student greeting back
        CountDownLatch latch2 = new CountDownLatch(1);
        AtomicReference<TwoWayTranslationHelper.TranslationResult> res2 = new AtomicReference<>();
        TwoWayTranslationHelper.translateStudentToHindi(null, "ᱡᱚᱦᱟᱨ", r -> {
            res2.set(r);
            latch2.countDown();
        });
        assertTrue(latch2.await(3, TimeUnit.SECONDS));
        assertTrue(res2.get().isSuccess);
        assertEquals("नमस्ते", res2.get().translatedText);
    }

    @Test
    public void testUnknownSpeechReturnsFailureWithoutFabrication() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TwoWayTranslationHelper.TranslationResult> resultRef = new AtomicReference<>();

        TwoWayTranslationHelper.translateTeacherToSanthali(null, "कुछ अज्ञात वाक्य जो शब्दकोश में नहीं है", result -> {
            resultRef.set(result);
            latch.countDown();
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        TwoWayTranslationHelper.TranslationResult result = resultRef.get();
        assertNotNull(result);
        assertFalse("Should fail without fabricating translations", result.isSuccess);
        assertNull(result.translatedText);
        assertNotNull(result.errorMessage);
    }

    @Test
    public void testEmptyOrNullSpeechFailsGracefully() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TwoWayTranslationHelper.TranslationResult> resultRef = new AtomicReference<>();

        TwoWayTranslationHelper.translateTeacherToSanthali(null, "   ", result -> {
            resultRef.set(result);
            latch.countDown();
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        TwoWayTranslationHelper.TranslationResult result = resultRef.get();
        assertNotNull(result);
        assertFalse(result.isSuccess);
    }
}
