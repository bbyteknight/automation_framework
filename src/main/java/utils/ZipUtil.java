package utils;

import exceptions.FrameworkException;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Create, extract, and inspect {@code .zip} archives via Zip4j — including password-protected,
 * AES-256 encrypted archives — so steps/tests can assert on archive contents without a scenario
 * having to shell out to an OS zip tool.
 */
public final class ZipUtil {

    private ZipUtil() {
    }

    /** Zips a single file or an entire directory (recursively) into a new, unencrypted archive. */
    public static File zip(String sourcePath, String destZipPath) {
        return zip(sourcePath, destZipPath, null);
    }

    /**
     * Same as {@link #zip(String, String)}, but AES-256 encrypts every entry when a non-blank
     * {@code password} is given.
     */
    public static File zip(String sourcePath, String destZipPath, String password) {
        File source = new File(sourcePath);
        File destZip = new File(destZipPath);
        FileHelper.ensureDirectoryExists(destZip.getAbsoluteFile().getParent());

        try {
            ZipFile zipFile = password == null || password.isBlank()
                    ? new ZipFile(destZip)
                    : new ZipFile(destZip, password.toCharArray());

            ZipParameters parameters = new ZipParameters();
            if (password != null && !password.isBlank()) {
                parameters.setEncryptFiles(true);
                parameters.setEncryptionMethod(EncryptionMethod.AES);
                parameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
            }

            if (source.isDirectory()) {
                zipFile.addFolder(source, parameters);
            } else {
                zipFile.addFile(source, parameters);
            }
            return destZip;
        } catch (ZipException e) {
            throw new FrameworkException("❌ Failed to zip \"" + sourcePath + "\" into \"" + destZipPath + "\"", e);
        }
    }

    /** Extracts every entry of an unencrypted archive into {@code destDirPath}. */
    public static File unzip(String zipPath, String destDirPath) {
        return unzip(zipPath, destDirPath, null);
    }

    /** Same as {@link #unzip(String, String)}, but unlocks a password-protected archive first. */
    public static File unzip(String zipPath, String destDirPath, String password) {
        File destDir = new File(destDirPath);
        FileHelper.ensureDirectoryExists(destDir.getPath());

        try {
            ZipFile zipFile = new ZipFile(zipPath);
            if (password != null && !password.isBlank()) {
                zipFile.setPassword(password.toCharArray());
            }
            zipFile.extractAll(destDirPath);
            return destDir;
        } catch (ZipException e) {
            throw new FrameworkException("❌ Failed to unzip \"" + zipPath + "\" into \"" + destDirPath + "\"", e);
        }
    }

    /**
     * Lists every entry's path inside the archive (files and directories). Reading the central
     * directory doesn't need a password even for an encrypted archive — only extracting entry
     * contents does.
     */
    public static List<String> listContents(String zipPath) {
        return fileHeaders(zipPath).stream()
                .map(FileHeader::getFileName)
                .collect(Collectors.toList());
    }

    /** Counts only file entries inside the archive (directory entries are excluded). */
    public static int fileCount(String zipPath) {
        return (int) fileHeaders(zipPath).stream()
                .filter(header -> !header.isDirectory())
                .count();
    }

    private static List<FileHeader> fileHeaders(String zipPath) {
        try {
            return new ZipFile(zipPath).getFileHeaders();
        } catch (ZipException e) {
            throw new FrameworkException("❌ Failed to read archive contents: " + zipPath, e);
        }
    }
}
