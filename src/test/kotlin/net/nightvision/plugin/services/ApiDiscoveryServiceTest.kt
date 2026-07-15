package net.nightvision.plugin.services

import org.junit.Assert.*
import org.junit.Test

class ApiDiscoveryServiceTest {

    /**
     * Mirrors the command-building logic in ApiDiscoveryService.extract()
     * to verify --lang is included/omitted correctly.
     */
    private fun buildCommand(directory: String, lang: String): List<String> {
        val command = mutableListOf("nightvision", "swagger", "extract", directory)
        if (lang.isNotEmpty() && lang != "all") {
            command.add("--lang")
            command.add(lang)
        }
        command.addAll(listOf("--no-upload", "--output", "test.yml"))
        return command
    }

    @Test
    fun `all omits lang flag`() {
        val cmd = buildCommand("/path/to/project", "all")
        assertFalse("--lang should not be present", cmd.contains("--lang"))
    }

    @Test
    fun `empty string omits lang flag`() {
        val cmd = buildCommand("/path/to/project", "")
        assertFalse("--lang should not be present", cmd.contains("--lang"))
    }

    @Test
    fun `specific language includes lang flag`() {
        val cmd = buildCommand("/path/to/project", "java")
        val langIndex = cmd.indexOf("--lang")
        assertTrue("--lang should be present", langIndex >= 0)
        assertEquals("java", cmd[langIndex + 1])
    }

    @Test
    fun `PHP language includes lang flag`() {
        val cmd = buildCommand("/path/to/project", "php")
        val langIndex = cmd.indexOf("--lang")
        assertTrue("--lang should be present", langIndex >= 0)
        assertEquals("php", cmd[langIndex + 1])
    }

    @Test
    fun `command always includes no-upload and output`() {
        val cmd = buildCommand("/path/to/project", "all")
        assertTrue(cmd.contains("--no-upload"))
        assertTrue(cmd.contains("--output"))
    }

    @Test
    fun `command starts with nightvision swagger extract`() {
        val cmd = buildCommand("/my/dir", "Python")
        assertEquals("nightvision", cmd[0])
        assertEquals("swagger", cmd[1])
        assertEquals("extract", cmd[2])
        assertEquals("/my/dir", cmd[3])
    }

    @Test
    fun `csharp language includes lang flag`() {
        val cmd = buildCommand("/path/to/project", "csharp")
        val langIndex = cmd.indexOf("--lang")
        assertTrue("--lang should be present", langIndex >= 0)
        assertEquals("csharp", cmd[langIndex + 1])
    }

    @Test
    fun `js language includes lang flag`() {
        val cmd = buildCommand("/path/to/project", "js")
        val langIndex = cmd.indexOf("--lang")
        assertTrue("--lang should be present", langIndex >= 0)
        assertEquals("js", cmd[langIndex + 1])
    }
}
