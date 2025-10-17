/*
Copyright (c) 2025 Arman Jussupgaliyev
*/
package shinovon.unalw1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.*;

public class Main {
	
	public static boolean alw1Found;

	public static void main(String[] args) {
		
		if (args.length < 3) {
			System.out.println("UnALW1 v1.0");
			System.out.println("by shinovon, 2025");
			System.out.println();
			System.out.println("Usage:");
			System.out.println("<injar> <outjar> <proguard.jar> [<library jars>]");
			return;
		}
		
		String injar = args[0];
		String outjar = args[1];
		String proguard = args[2];
		String libraryjars;
		if (args.length > 3) {
			libraryjars = args[3];
		} else {
			libraryjars = System.getProperty("java.home") + File.separatorChar + "lib" + File.separatorChar + "rt.jar";
		}
		
		try {
			File f;
			outjar = (f = new File(outjar)).getCanonicalPath();
			if (f.exists()) f.delete();
			proguard = (f = new File(proguard)).getCanonicalPath();
			if (!f.exists()) {
				System.err.println("Proguard not found");
				System.exit(1);
				return;
			}
			
			File temp = File.createTempFile("unalw1", ".jar");
			temp.deleteOnExit();
			try {
				try (ZipFile zipFile = new ZipFile(injar)) {
					try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(temp))) {
						Enumeration<? extends ZipEntry> entries = zipFile.entries();
						while (entries.hasMoreElements()) {
							ZipEntry entry = entries.nextElement();
							String name = entry.getName();
							if (name.endsWith(".class")) {
								System.out.println("Transforming " + name);
								ClassReader classReader = new ClassReader(zipFile.getInputStream(entry));
								ClassWriter classWriter = new ClassWriter(0);
								classReader.accept(new ALWClassAdapter(classWriter, name.substring(0, name.length() - 6)), ClassReader.SKIP_DEBUG);
								
								zipOut.putNextEntry(new ZipEntry(name));
								zipOut.write(classWriter.toByteArray());
								zipOut.closeEntry();
							} else {
								System.out.println("Copying " + name);
								zipOut.putNextEntry(new ZipEntry(entry));
								try (InputStream in = zipFile.getInputStream(entry)) {
									int r;
									byte[] b = new byte[1024];
									while ((r = in.read(b)) != -1) {
										zipOut.write(b, 0, r);
									}
								}
							}
						}
					}
				}
				if (!alw1Found) {
					System.err.println("ALW1.startApp() not found, aborting");
					System.exit(1);
					return;
				}
				System.out.println("Preverifying");
				
				ProcessBuilder builder = new ProcessBuilder(
						new String[] {
						System.getProperty("java.home") + File.separatorChar + "bin" + File.separatorChar + "java",
						"-jar",
						proguard,
						"-dontwarn",
						"-dontnote",
						"-dontobfuscate",
						"-dontoptimize",
						"-dontshrink",
						"-microedition",
						"-target",
						"1.3",
						"-libraryjars",
						libraryjars,
						"-injars",
						temp.getAbsolutePath(),
						"-outjar",
						outjar,
//						"-keep public class * extends javax.microedition.midlet.MIDlet").
						}
						);
				builder.inheritIO();
				Process p = builder.start();
				
				if (p.waitFor() != 0) {
					System.err.println("Proguard failed");
					System.exit(1);
					return;
				}
				
				System.out.println("Done");
			} finally {
				temp.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
