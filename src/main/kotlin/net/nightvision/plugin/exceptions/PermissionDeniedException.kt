package net.nightvision.plugin.exceptions

class PermissionDeniedException(command: List<String>) :
    RuntimeException("Permission denied: ${command.joinToString(" ")}")