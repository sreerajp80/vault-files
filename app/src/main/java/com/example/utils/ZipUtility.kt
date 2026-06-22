package com.example.utils

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtility {

    /**
     * Compresses a file or directory recursively into a ZIP file.
     * @param sourceFile The file or folder to be zipped.
     * @param destZipFile The output ZIP file.
     * @return true if successful, false otherwise.
     */
    fun zip(sourceFile: File, destZipFile: File): Boolean {
        return try {
            FileOutputStream(destZipFile).use { fos ->
                ZipOutputStream(BufferedOutputStream(fos)).use { zos ->
                    if (sourceFile.isDirectory) {
                        zipDirectory(sourceFile, sourceFile, zos)
                    } else {
                        zipFile(sourceFile, "", zos)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun zipDirectory(rootDir: File, currentDir: File, zos: ZipOutputStream) {
        val files = currentDir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                zipDirectory(rootDir, file, zos)
            } else {
                // Calculate relative path for zip entry
                val relativePath = file.absolutePath.substring(rootDir.absolutePath.length + 1)
                zipFile(file, relativePath, zos)
            }
        }
    }

    private fun zipFile(file: File, relativePath: String, zos: ZipOutputStream) {
        val entryName = relativePath.ifEmpty { file.name }
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        
        BufferedInputStream(FileInputStream(file)).use { bis ->
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (bis.read(buffer).also { bytesRead = it } != -1) {
                zos.write(buffer, 0, bytesRead)
            }
        }
        zos.closeEntry()
    }

    /**
     * Unzips a zip file into a target directory.
     * @param zipFile The input zip file.
     * @param targetDir The output directory.
     * @return true if successful, false otherwise.
     */
    fun unzip(zipFile: File, targetDir: File): Boolean {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        return try {
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val file = File(targetDir, entry.name)
                    
                    // Prevent path traversal attacks (Zip Slip vulnerability checker)
                    val canonicalPath = file.canonicalPath
                    val canonicalTarget = targetDir.canonicalPath
                    if (!canonicalPath.startsWith(canonicalTarget + File.separator) && canonicalPath != canonicalTarget) {
                        throw SecurityException("Hostile zip entry detected outside destination root: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        // Create parent directories if missing
                        val parent = file.parentFile
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs()
                        }
                        
                        FileOutputStream(file).use { fos ->
                            val buffer = ByteArray(4096)
                            var bytesRead: Int
                            while (zis.read(buffer).also { bytesRead = it } != -1) {
                                fos.write(buffer, 0, bytesRead)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
