package cmu.varviz.utils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import cmu.varviz.VarvizColors;
import cmu.varviz.VarvizException;

public interface FileUtils {
	
	public static void copyFileFromVarvizJar(String sourcePath, String resourceName, File destination) {
		if (new File(destination, resourceName).exists()) {
			return;
		}
		try (InputStream stream = VarvizColors.class.getResourceAsStream(sourcePath + "/" + resourceName)){
            if(stream == null) {
                throw new VarvizException("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }

            int readBytes;
            byte[] buffer = new byte[4096];
            File file = new File(destination + "/" + resourceName);
            try (OutputStream resStreamOut = java.nio.file.Files.newOutputStream(file.toPath())) {
	            while ((readBytes = stream.read(buffer)) > 0) {
	                resStreamOut.write(buffer, 0, readBytes);
	            }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
}
