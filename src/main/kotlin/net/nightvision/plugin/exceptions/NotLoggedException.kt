package net.nightvision.plugin.exceptions

class NotLoggedException(command: List<String>) :
    RuntimeException("User is not logged: ${command.joinToString(" ")}")