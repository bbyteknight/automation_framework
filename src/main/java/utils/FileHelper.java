package utils;

import exceptions.FrameworkException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/** Shared filesystem helpers — directory creation and byte/string writes used by ScreenshotUtil
 * and anything else that needs to drop an artifact under target/ or test-output/. */
public final class FileHelper {

    private FileHelper() {
    }

    /** Creates the directory (and parents) if it doesn't already exist. */
    public static void ensureDirectoryExists(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new FrameworkException("❗ Could not create directory: " + directoryPath);
        }
    }

    public static File writeBytesToFile(byte[] data, String filePath) {
        File file = new File(filePath);
        ensureDirectoryExists(file.getParentFile().getPath());
        try {
            FileUtils.writeByteArrayToFile(file, data);
            return file;
        } catch (IOException e) {
            throw new FrameworkException("❗ Could not write file: " + filePath, e);
        }
    }

    public static String readFileAsString(String filePath) {
        try {
            return FileUtils.readFileToString(new File(filePath), "UTF-8");
        } catch (IOException e) {
            throw new FrameworkException("❗ Could not read file: " + filePath, e);
        }
    }
}
