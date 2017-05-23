package cmu.varviz.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import cmu.varviz.VarvizConstants;

public interface FileUtils {

	public static void CopyFileFromVarvizJar(String sourcePath, String resourceName, File destination) {
		if (new File(destination + "/" + resourceName).exists()) {
			return;
		}
		try (InputStream stream = VarvizConstants.class.getResourceAsStream(sourcePath + "/" + resourceName)){
            if(stream == null) {
                throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }

            int readBytes;
            byte[] buffer = new byte[4096];
            try (OutputStream resStreamOut = new FileOutputStream(destination + "/" + resourceName)) {
	            while ((readBytes = stream.read(buffer)) > 0) {
	                resStreamOut.write(buffer, 0, readBytes);
	            }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
}
