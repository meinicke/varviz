package cmu.varviz.io.graphviz;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import cmu.varviz.trace.Trace;
import cmu.varviz.utils.CommandLineRunner;

public class GrapVizExport {

	private String fileName;
	private Trace trace;
	private Format extension;

	public GrapVizExport(String fileName, Trace vatrace) {
		int extensionPosition = fileName.lastIndexOf('.', fileName.length() - 1);
		this.fileName = fileName.substring(0, extensionPosition);
		String substring = fileName.substring(extensionPosition + 1);
		this.extension = Format.valueOf(substring);
		this.trace = vatrace;
	}

	public void write() {
		final File file = new File(fileName + ".gv");
		System.out.print("create file: " + file.getAbsolutePath());
		System.out.flush();
		try (PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8.name())) {
			trace.printToGraphViz(pw);
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		System.out.print(" > " + fileName + "." + extension);
		callGraphviz(file);
		file.delete();
	}

	private void callGraphviz(final File file) {
		final String[] commands = new String[] { "dot", "-T" + extension.toString(), file.getPath(), "-o", fileName + "." + extension.toString()};
		try {
			Field f = BufferedInputStream.class.getDeclaredField("DEFAULT_BUFFER_SIZE");
			f.setAccessible(true);
			System.out.println(f.getInt(null));
			f.setInt(null, 8192);
		} catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}

		CommandLineRunner.process(commands);
	}

}
