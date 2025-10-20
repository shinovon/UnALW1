/*
Copyright (c) 2025 Arman Jussupgaliyev
*/
package shinovon.unalw1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class Main {
	
	public static final String[] modes = {
			"auto",
			"alw1",
			"vserv",
			"ia",
			"hovr",
			"freexter",
			"gs",
			"glomo",
	};
	
	public static String mode;
	public static boolean verbose = false;
	
	public static boolean alw1Found;
	public static boolean vservFound;
	public static boolean connectorFound;
	public static boolean inneractiveFound;
	public static boolean hovrFound;
	public static boolean freexterFound;
	public static boolean greystripeFound1;
	public static boolean greystripeFound2;
	public static boolean glomoFound;
	
	// greystripe
	public static String greystripeConnectionClass;
	public static String greystripeRunnerClass;
	public static String greystripeStartFunc;
	public static String greystripeCheckFunc;
	public static boolean hasGsid;
	
	static Map<String, ClassNode> classNodes = new HashMap<String, ClassNode>();

	public static void main(String[] args) {
		
		if (args.length < 3) {
			System.out.println("UnALW1 v3.0");
			System.out.println("J2ME Ad engine removal tool");
			System.out.println("Supports: ALW1, vServ, InnerActive, Hovr, Freexter, Greystripe, Glomo");
			System.out.println();
			System.out.println("Usage: <injar> <outjar> <proguard> <libraryjars> [mode]");
			System.out.println();
			System.out.println("Where:");
			System.out.println(" injar: Path to input jar");
			System.out.println(" outjar: Path to output jar");
			System.out.println(" proguard: Path to proguard.jar, e.g: C:\\proguard-7.7.0\\lib\\proguard.jar");
			System.out.println(" libraryjars: path to folder with MIDP libraries, e.g: C:\\Nokia\\Devices\\S40_5th_Edition_SDK\\lib");
			StringBuilder sb = new StringBuilder(" mode: ");
			for (String s: modes) {
				sb.append(s).append('/');
			}
			sb.setLength(sb.length() - 1);
			System.out.println(sb + ", auto by default");
			System.out.println();
			System.out.println("By shinovon, 2025");
			return;
		}
		
		String injar = args[0];
		String outjar = args[1];
		String proguard = args[2];
		String libraryjars = args[3];
		mode = args.length > 4 ? args[4].toLowerCase() : "auto";
		
		checkMode: {
			for (String s: modes) {
				if (s.equalsIgnoreCase(mode)) {
					break checkMode;
				}
			}
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
					if ("gs".equals(Main.mode) || "auto".equals(Main.mode)) {
						// greystripe: check for .gsid resource
						hasGsid = zipFile.getEntry(".gsid") != null || zipFile.getEntry("/.gsid") != null;
					}
					
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
								if (verbose) System.out.println("Transforming " + name);
								
								ClassReader classReader = new ClassReader(zipFile.getInputStream(entry));
								ClassNode node = new ClassNode();
								ALWClassAdapter adapter = new ALWClassAdapter(node, className);
								classReader.accept(adapter, ClassReader.SKIP_DEBUG);
								
								// process nodes after class adapter
								List<String[]> renameMethods = adapter.getRenameList();
								for (Object m : node.methods) {
									MethodNode mn = (MethodNode) m;
									
									// greystripe method 1
									if (greystripeConnectionClass != null && greystripeConnectionClass.equals(className)) {
										if (mn.desc.equals("()V") && mn.name.equals(greystripeStartFunc)) {
											InsnList ins = mn.instructions;
											for (AbstractInsnNode n = ins.getFirst(); n != null; n = n.getNext()) {
												if (n instanceof MethodInsnNode
														&& ((MethodInsnNode)n).desc.equals("()Z") && ((MethodInsnNode) n).name.equals(greystripeCheckFunc)) {
													// remove connection check
													// if (!a()) return; -> if (!true) return;
													ins.remove(n.getPrevious());
													ins.set(n, new InsnNode(Opcodes.ICONST_1));
													
													System.out.println("Greystripe patched (method 1): " + greystripeConnectionClass + '.' + greystripeStartFunc + "()V");
													greystripeFound1 = true;
													break;
												}
											}
										}
									}

									// renaming
									if (renameMethods != null) {
										for (String[] s : renameMethods) {
											if (s[0].equals(mn.name) && s[1].equals(mn.desc)) {
												System.out.println("Renaming " + className + "." + mn.name + mn.desc + " -> " + s[2]);
												mn.name = s[2];
												break;
											}
										}
									}
								}
								
								classNodes.put(className, node);
							} else {
								if (verbose) System.out.println("Copying " + name);
								try (InputStream in = zipFile.getInputStream(entry)) {
									zipOut.putNextEntry(new ZipEntry(entry.getName()));
									write(zipOut, in);
									zipOut.closeEntry();
								}
							}
						}
						
						// write nodes
						for (Entry<String, ClassNode> entry : classNodes.entrySet()) {
							ClassNode node = entry.getValue();
							String className = entry.getKey();
							
							// greystripe method 2
							if (greystripeRunnerClass != null && className.equals(greystripeRunnerClass)) {
								if (!node.interfaces.contains("java/lang/Runnable") || node.interfaces.size() != 1) {
									greystripeRunnerClass = null;
								} else {
									for (Object m : node.methods) {
										MethodNode mn = (MethodNode) m;
										if (!mn.name.equals("run") || !mn.desc.equals("()V"))
											continue;

										InsnList ins = mn.instructions;
										for (AbstractInsnNode n : ins.toArray()) {
											// remove everything until getstatic MIDlet is found
											if (n.getOpcode() == Opcodes.GETSTATIC && !"Z".equals(((FieldInsnNode) n).desc)) {
												System.out.println("Greystripe patched (method 2): " + greystripeRunnerClass);
												greystripeFound2 = true;
												break;
											}
											ins.remove(n);
										}
										break;
									}
								}
							}
							
							if (verbose) System.out.println("Writing " + className);

							ClassWriter classWriter = new ClassWriter(0);
							node.accept(classWriter);
							zipOut.putNextEntry(new ZipEntry(className + ".class"));
							zipOut.write(classWriter.toByteArray());
							zipOut.closeEntry();
						}
						
						// add unvserv connector classes
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

				if (greystripeFound1) {
					System.out.println("Warning: Greystripe may be unwrapped partially");
				} else if (!vservFound
						&& !alw1Found
						&& !inneractiveFound
						&& !hovrFound
						&& !freexterFound
						&& !greystripeFound2
						&& !glomoFound) {
					if (hasGsid) {
						System.err.println("Greystripe was detected, but could not unwrap it");
						System.exit(1);
						return;
					} else if (!connectorFound) {
						System.err.println("No ad engine was detected, aborting.");
						System.exit(1);
						return;
					}
					System.err.println("Warning: No ad engine was detected, prooceding anyway..");
				}
				
				// preverify with proguard
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
