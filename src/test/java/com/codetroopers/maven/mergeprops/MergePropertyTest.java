package com.codetroopers.maven.mergeprops;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:cedric@gatay.fr">cgatay</a>
 */
public class MergePropertyTest {
    @Test
    public void testExtractFilePrefix(){
        assertEquals("lang", MergeProperty.extractFilePrefix("lang_fr.properties"));
        assertEquals("lang", MergeProperty.extractFilePrefix("lang.properties"));
        assertEquals("lang", MergeProperty.extractFilePrefix("lang.toto_en.properties"));
        assertEquals("lang", MergeProperty.extractFilePrefix("src/main/resources/lang.properties"));
    }

    @Test
    public void testExtractFileSuffix(){
        assertEquals("properties", MergeProperty.extractFileSuffix("lang_fr.properties"));
        assertEquals("properties", MergeProperty.extractFileSuffix("lang.toto.properties"));
        assertEquals("properties", MergeProperty.extractFileSuffix("lang.properties"));
        assertEquals("properties", MergeProperty.extractFileSuffix("src/main/resources/lang.properties"));
    }

    @Test
    public void testExtractLocaleFromFileName(){
        assertEquals("",MergeProperty.extractLocaleFromFileName("language.properties"));
        assertEquals("en",MergeProperty.extractLocaleFromFileName("language_en.properties"));
        assertEquals("fr",MergeProperty.extractLocaleFromFileName("language_fr.properties"));
        assertEquals("fr",MergeProperty.extractLocaleFromFileName("TOTO.language_fr.properties"));
        assertEquals("fr",MergeProperty.extractLocaleFromFileName("TOTO_language_fr.properties"));
    }

    @Test
    public void testInvalidPrefix() throws MojoFailureException {
        Properties p = new Properties();
        p.put("TEST.yata", "wondelle");
        assertFalse(MergeProperty.containsInvalidPrefix("TEST", p));
        p = new Properties();
        p.put("TESTu.yata", "wondelle");
        try {
            MergeProperty.containsInvalidPrefix("TEST", p);
            fail();
        } catch (MojoFailureException e) {
        }
        p = new Properties();
        p.put("T.yata", "wondelle");
        try {
            MergeProperty.containsInvalidPrefix("TEST", p);
            fail();
        } catch (MojoFailureException e) {
        }
    }

    @Test
    public void testPreprocessSetDoubleQuotes() throws MojoFailureException {
        Properties p = new Properties();
        p.put("ya.ta", "chaine normale sans quote");
        p.put("yi.ti", "chaine avec un'simple quote");
        p.put("yu.tu", "chaine avec un''double simple quote");
        p.put("ye.te", "chaine ' avec un simple quote et des espaces et avec un '' double simple quote");
        p.put("yo.to", "chaine''''avec quatre simple quote");
        Map<String, Properties> map = new HashMap<String, Properties>();
        map.put("test", p);
        MergeProperty.processProperties(map, new Merge(), null);
        final Properties q = map.get("test");
        assertEquals("chaine normale sans quote", q.getProperty("ya.ta"));
        assertEquals("chaine avec un''simple quote", q.getProperty("yi.ti"));
        assertEquals("chaine avec un''double simple quote", q.getProperty("yu.tu"));
        assertEquals("chaine '' avec un simple quote et des espaces et avec un '' double simple quote",
                     q.getProperty("ye.te"));
        assertEquals("chaine''''avec quatre simple quote", q.getProperty("yo.to"));
    }

    @Test
    public void testPreprocessFailsIfNotSameQuantity() throws Exception {
        Map<String, Properties> map = commonInitForCount();
        try {
            MergeProperty.processProperties(map, new Merge(), null);
            Assert.fail("There is not the same amount of keys in the two bundles");
        } catch (MojoFailureException e) {
            Assert.assertTrue("The source object should be a list", e.getSource() instanceof Collection);
            Assert.assertEquals("The lonelyWords list should contains 3 items", 3, ((Collection) e.getSource()).size());
            Assert.assertTrue(((Collection) e.getSource()).contains("trulu.lu"));
            Assert.assertTrue(((Collection) e.getSource()).contains("trolo.lo"));
            Assert.assertTrue(((Collection) e.getSource()).contains("trili.li"));
        }

    }

    @Test
    public void testPreprocessCheckIgnoredIfPropertyPresent() throws Exception {
        System.setProperty(MergeProperty.ERROR_IGNORE_FLAG, "true");
        Map<String, Properties> map = commonInitForCount();
        MergeProperty.processProperties(map, new Merge(), null);
    }

    private Map<String, Properties> commonInitForCount() {
        Properties p =new Properties();
        p.put("trala.la", "itoo");
        p.put("trili.li", "zizou");
        Properties p2 = new Properties();
        p2.put("trala.la", "qqqqs");
        p2.put("trili.li", "SXC");
        p2.put("trulu.lu", "lkj");
        Properties p3 = new Properties();
        p3.put("trala.la", "qqqqs");
        p3.put("trolo.lo", "loal");
        Map<String, Properties> map = new HashMap<String, Properties>();
        map.put("test1", p);
        map.put("test2", p2);
        map.put("test3", p3);
        return map;
    }
}
