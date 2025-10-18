/*
Copyright (c) 2025 Arman Jussupgaliyev
*/
package shinovon.unalw1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class Main {
	
	public static boolean alw1Found;
	public static boolean vservFound;
	public static boolean connectorFound;
	public static boolean inneractiveFound;
	public static boolean hovrFound;
	
	public static String mode;
	public static boolean verbose;
	
	public static String wrapperStartMethod;

	public static void main(String[] args) {
		
		if (args.length < 3) {
			System.out.println("UnALW1 v3.0");
			System.out.println("J2ME Ad engine removal tool");
			System.out.println("Supports: ALW1, vServ, InnerActive, Hovr");
			System.out.println();
			System.out.println("Usage: <injar> <outjar> <proguard> <libraryjars> [mode]");
			System.out.println();
			System.out.println("Where:");
			System.out.println(" injar: Path to input jar");
			System.out.println(" outjar: Path to output jar");
			System.out.println(" proguard: Path to proguard.jar, e.g: C:\\proguard-7.7.0\\lib\\proguard.jar");
			System.out.println(" libraryjars: path to folder with MIDP libraries, e.g: C:\\Nokia\\Devices\\S40_5th_Edition_SDK\\lib");
			System.out.println(" mode: auto/alw1/vserv/ia/hovr, auto by default");
			System.out.println();
			System.out.println("By shinovon, 2025");
			return;
		}
		
		String injar = args[0];
		String outjar = args[1];
		String proguard = args[2];
		String libraryjars = args[3];
		mode = args.length > 4 ? args[4].toLowerCase() : "auto";
		
		if (!"auto".equals(mode) && !"alw1".equals(mode) && !"vserv".equals(mode) && !"ia".equals(mode) && !"hovr".equals(mode)) {
			System.err.println("Invalid mode");
			System.exit(1);
			return;
		}
		
		try {
			File f;
			outjar = (f = new File(outjar)).getCanonicalPath();
//			if (f.exists()) f.delete();
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
								String className = name.substring(0, name.length() - 6);
								if (className.endsWith("UnVservConnector")) {
									System.err.println("Jar file appears to be already patched, aborting.");
									System.exit(1);
									return;
								}
								if (className.equals("WRAPPER")) {
									ClassReader classReader = new ClassReader(zipFile.getInputStream(entry));
									ClassNode node = new ClassNode();
									classReader.accept(new ALWClassAdapter(node, className), ClassReader.SKIP_DEBUG);
									for (Object m : node.methods) {
										MethodNode mn = (MethodNode) m;
										if (wrapperStartMethod.equals(mn.name)) {
											mn.name = "startApp";
										}
									}
									ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
									node.accept(classWriter);
									zipOut.putNextEntry(new ZipEntry(name));
									zipOut.write(classWriter.toByteArray());
									zipOut.closeEntry();
									continue;
								}
								if (verbose) System.out.println("Transforming " + name);
								ClassReader classReader = new ClassReader(zipFile.getInputStream(entry));
								ClassWriter classWriter = new ClassWriter(0);
								classReader.accept(new ALWClassAdapter(classWriter, className), ClassReader.SKIP_DEBUG);
								zipOut.putNextEntry(new ZipEntry(name));
								zipOut.write(classWriter.toByteArray());
								zipOut.closeEntry();
							} else {
								if (verbose) System.out.println("Copying " + name);
								try (InputStream in = zipFile.getInputStream(entry)) {
									zipOut.putNextEntry(new ZipEntry(entry.getName()));
									write(zipOut, in);
									zipOut.closeEntry();
								}
							}
						}
						
						if (connectorFound) {
							System.out.println("Adding vServ wrapper classes");
							try (ZipInputStream zipIn = new ZipInputStream("".getClass().getResourceAsStream("/vserv.jar"))) {
								ZipEntry entry;
								while ((entry = zipIn.getNextEntry()) != null) {
									ZipEntry copy = new ZipEntry(entry);
									copy.setCompressedSize(-1);
									zipOut.putNextEntry(copy);
									write(zipOut, zipIn);
									zipOut.closeEntry();
								}
							}
						}
					}
				}
				if (!vservFound && !alw1Found && !inneractiveFound && !hovrFound) {
					if (!connectorFound) {
						System.err.println("No ad engine was detected, aborting.");
						System.exit(1);
						return;
					}
					System.err.println("Warning: No ad engine was detected, prooceding anyway..");
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
	
	private static void write(OutputStream out, InputStream in) throws Exception {
		int r;
		byte[] b = new byte[1024];
		while ((r = in.read(b)) != -1) {
			out.write(b, 0, r);
		}
	}

}
