package net.nightvision.plugin.exceptions

class CommandNotFoundException(command: List<String>) :
    RuntimeException("Command not found: ${command.joinToString(" ")}")