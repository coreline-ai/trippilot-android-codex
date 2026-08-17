package io.trippilot.app.integration.codex.alpine.runtime

import android.system.Os
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Writes only non-secret official CLI policy and verifies credential metadata without reading it. */
internal object OfficialCodexCliHomeProvisioner {
    fun provision(home: File) {
        requirePrivateDirectory(home)
        val config = File(home, "config.toml")
        check(!Files.isSymbolicLink(config.toPath()))
        val temporary = File(home, ".config.toml.${System.nanoTime()}.partial")
        try {
            FileOutputStream(temporary).use { output ->
                output.write(CODEX_CONFIG.toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            Os.chmod(temporary.absolutePath, MODE_OWNER_RW)
            Files.move(temporary.toPath(), config.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            Os.chmod(config.absolutePath, MODE_OWNER_RW)
        } finally {
            temporary.delete()
        }
        requirePrivateFile(config)
        val credential = File(home, "auth.json")
        if (credential.exists() || Files.isSymbolicLink(credential.toPath())) requirePrivateFile(credential)
    }

    private fun requirePrivateDirectory(directory: File) {
        check(directory.isDirectory && !Files.isSymbolicLink(directory.toPath()))
        check(Os.stat(directory.absolutePath).st_mode and MODE_MASK == MODE_OWNER_RWX)
    }

    private fun requirePrivateFile(file: File) {
        check(file.isFile && !Files.isSymbolicLink(file.toPath()))
        check(Os.stat(file.absolutePath).st_mode and MODE_MASK == MODE_OWNER_RW)
    }

    private const val CODEX_CONFIG = """forced_login_method = "chatgpt"
cli_auth_credentials_store = "file"

[history]
persistence = "none"

[analytics]
enabled = false

[feedback]
enabled = false

[otel]
environment = "local"
trace_exporter = "none"
log_user_prompt = false
"""
    private const val MODE_OWNER_RWX = 448
    private const val MODE_OWNER_RW = 384
    private const val MODE_MASK = 511
}
