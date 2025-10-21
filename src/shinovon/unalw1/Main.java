/*
Copyright (c) 2025 Arman Jussupgaliyev
*/
package shinovon.unalw1;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.JFrame;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class Main implements Runnable {
	
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
	
	static Main inst;
	
	// state
	public boolean alw1Found;
	public boolean vservFound;
	public boolean connectorFound;
	public boolean inneractiveFound;
	public boolean hovrFound;
	public boolean freexterFound;
	public boolean greystripeFound1;
	public boolean greystripeFound2;
	public boolean glomoFound;
	
	// greystripe
	public String greystripeConnectionClass;
	public String greystripeRunnerClass;
	public String greystripeStartFunc;
	public String greystripeCheckFunc;
	public boolean hasGsid;
	
	Map<String, ClassNode> classNodes = new HashMap<String, ClassNode>();

	private boolean running;
	
	// ui
	private JFrame frame;
	
	// options
	String proguard;
	String libraryjars;
	String outjar;
	String outdir;
	String mode = "auto";
	boolean verbose = false;
	
	Object target;
	boolean cli;
	int files;
	boolean failed;

	public static void main(String[] args) {
		if (args.length > 0) {
			System.out.println("UnALW1 v5.0 by shinovon, 2025");
			System.out.println("J2ME advertising and payment engines removal tool");
			System.out.println();
			if (args[0].endsWith("-version")) return;
			if (args[0].endsWith("-help")) {
				System.out.println("Supports: ALW1, vServ, InnerActive, Hovr, Freexter, Greystripe, Glomo");
				System.out.println();
				System.out.println("Usage: java -jar unalw1.jar [arguments]"); 
				System.out.println();
				System.out.println("Where options are:");
				System.out.println(" -in <jar file or directory>");
				System.out.println(" -outjar <jar file> Path to output jar");
				System.out.println(" -outdir <path> Path to output dir");
				System.out.println(" -proguard <proguard.jar> Path to proguard.jar, e.g: C:\\proguard-7.7.0\\lib\\proguard.jar");
				System.out.println(" -libraryjars <library path> Path to folder with MIDP libraries, e.g: C:\\Nokia\\Devices\\S40_5th_Edition_SDK\\lib");
				StringBuilder sb = new StringBuilder(" -mode <mode> ");
				for (String s: modes) {
					sb.append(s).append('/');
				}
				sb.setLength(sb.length() - 1);
				System.out.println(sb + ", auto by default");
				return;
			}

			inst = new Main();
			inst.cli = true;
			
			String key = null;
			for (String s : args) {
				if (key != null) {
					if ("-in".equals(key)) {
						inst.target = s;
					} else if ("-outjar".equals(key)) {
						inst.outjar = s;
					} else if ("-outdir".equals(key)) {
						inst.outdir = s;
					} else if ("-proguard".equals(key)) {
						inst.proguard = s;
					} else if ("-libraryjars".equals(key)) {
						inst.libraryjars = s;
					} else if ("-mode".equals(key)) {
						inst.mode = s;
					} else {
						System.err.println("Unrecognized parameter: " + key);
						System.exit(1);
						return;
					}
					key = null;
				} else {
					key = s;
				}
			}
			if (key != null) {
				System.err.println("Incomplete parameter: " + key);
				System.exit(1);
				return;
			}
			
			if (inst.target == null) {
				System.err.println("No input parameter set");
				System.exit(1);
				return;
			}
			
			new Thread(inst).start();
			return;
		}
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					inst = new Main();
					inst.initializeUI();
					inst.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void run() {
		running = true;
		try {
			checkMode: {
				for (String s: modes) {
					if (s.equalsIgnoreCase(mode)) {
						break checkMode;
					}
				}
				logError("Invalid mode: " + mode, true);
				return;
			}
			
			if (proguard == null) {
				logError("Proguard not set", true);
				return;
			}
			if (libraryjars == null) {
				logError("Library path not set", true);
				return;
			}
			try {
				File f;
				proguard = (f = new File(proguard)).getCanonicalPath();
				if (!f.exists()) {
					logError("Proguard not found", true);
					return;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			Object target = this.target;
			if (target instanceof String) {
				process((String) target);
			} else if (target instanceof List) {
				for (Object e: (List) target) {
					if (e == null) continue;
					process(e.toString());
				}
			}
			log(files == 0 ? "Nothing to do" : "Done", true);
		} finally {
			running = false;
		}
	}
	
	private void process(String t) {
		File f = new File(t);
		if (f.isDirectory()) {
			for (File s: f.listFiles()) {
				process(s);
			}
		} else {
			process(f);
		}
	}
	
	private void process(File f) {
		if (f.isDirectory()) {
			for (File s: f.listFiles()) {
				process(s);
			}
			return;
		}
		if (!f.isFile()) return;
		{
			String n = f.getName().toLowerCase();
			if (!n.endsWith(".zip") && !n.endsWith(".jar")) return;
			log("File: " + f, true);
		}
		
		resetState();
		files++;
		
		run: {
			try {
				String outjar;
				if (this.outjar != null) {
					outjar = this.outjar;
				} else if (this.outdir != null) {
					outjar = Paths.get(outdir).resolve(f.getName()).toString();
				} else {
					outjar = f.getCanonicalPath();
					outjar = outjar.substring(0, outjar.length() - 4) + "_unwrapped.jar";
				}
				
				File temp = File.createTempFile("unalw1", ".jar");
				try {
					try (ZipFile zipFile = new ZipFile(f)) {
						if ("gs".equals(mode) || "auto".equals(mode)) {
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
										logError("Jar file appears to be already patched, aborting.", false);
										break run;
									}
									if (verbose) log("Transforming " + name);
									
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
														
														log("Greystripe patched (method 1): " + greystripeConnectionClass + '.' + greystripeStartFunc + "()V");
														greystripeFound1 = true;
														break;
													}
												}
											}
										} else if (alw1Found && mn.name.equals("trialEnd") && mn.desc.equals("()V")) {
											// alw1: remove trialEnd() code
											log("Patched ALW1: " + className + '.' + mn.name + mn.desc);
											mn.instructions.clear();
											mn.instructions.add(new InsnNode(Opcodes.RETURN));
										}
	
										// renaming
										if (renameMethods != null) {
											for (String[] s : renameMethods) {
												if (s[0].equals(mn.name) && s[1].equals(mn.desc)) {
													log("Renaming " + className + "." + mn.name + mn.desc + " -> " + s[2]);
													mn.name = s[2];
													break;
												}
											}
										}
									}
									
									classNodes.put(className, node);
								} else {
									if (verbose) log("Copying " + name);
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
													log("Greystripe patched (method 2): " + greystripeRunnerClass);
													greystripeFound2 = true;
													break;
												}
												ins.remove(n);
											}
											break;
										}
									}
								}
								
								if (verbose) log("Writing " + className);
	
								ClassWriter classWriter = new ClassWriter(0);
								node.accept(classWriter);
								zipOut.putNextEntry(new ZipEntry(className + ".class"));
								zipOut.write(classWriter.toByteArray());
								zipOut.closeEntry();
							}
							
							// add unvserv connector classes
							if (connectorFound) {
								log("Adding vServ wrapper classes", false);
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
						log("Warning: Greystripe may be unwrapped partially");
					} else if (!vservFound
							&& !alw1Found
							&& !inneractiveFound
							&& !hovrFound
							&& !freexterFound
							&& !greystripeFound2
							&& !glomoFound) {
						if (hasGsid) {
							logError("Greystripe was detected, but could not unwrap it", false);
							failed = true;
							break run;
						} else if (!connectorFound) {
							logError("No ad engine was detected, aborting.", false);
							failed = true;
							break run;
						}
						log("Warning: No ad engine was detected, prooceding anyway..");
					}
	
					// preverify with proguard
					File tempConfig = File.createTempFile("unalw1", ".cfg");
					try {
						try (PrintStream ps = new PrintStream(new FileOutputStream(tempConfig))) {
							ps.println("-dontwarn");
							ps.println("-dontnote");
							ps.println("-dontobfuscate");
							ps.println("-dontoptimize");
							ps.println("-dontshrink");
							ps.println("-microedition");
							ps.println("-skipnonpubliclibraryclasses");
							ps.println("-target 1.3");
							ps.print("-libraryjars ");
							ps.println(escapeFileArg(libraryjars));
							ps.print("-injars ");
							ps.println(temp.getCanonicalPath());
							ps.print("-outjar ");
							ps.println(escapeFileArg(new File(outjar).getCanonicalPath()));
						}
						
						List<String> args = new ArrayList<String>();
						args.add(System.getProperty("java.home") + File.separatorChar + "bin" + File.separatorChar + "java");
						args.add("-jar");
						args.add(proguard);
						args.add("@" + tempConfig.getAbsolutePath());
						
						ProcessBuilder builder = new ProcessBuilder(args);
						builder.inheritIO();
						
						if (builder.start().waitFor() != 0) {
							logError("Proguard failed", false);
							failed = true;
							break run;
						}
					} finally {
						tempConfig.delete();
					}
					
					log("Wrote to " + outjar);
				} finally {
					temp.delete();
				}
			} catch (Exception e) {
				e.printStackTrace();
				failed = true;
			}
		}
		if (failed) {
			logError("Failed", false);
		}
		log("");
	}
	
	void logError(String s, boolean b) {
		System.err.println(s);
	}
	
	void log(String s) {
		log(s, false);
	}
	
	void log(String s, boolean b) {
		System.out.println(s);
//		if (textArea == null) return;
//		sb.append(s).append('\n');
//		if (b) textArea.setText(sb.toString());
	}
	
	void clear() {
//		if (textArea == null) return;
//		sb.setLength(0);
//		textArea.setText("");
//		if (global) {
//			found = new ArrayList<String>();
//		}
	}

	void initializeUI() {
		
	}
	
	public void resetState() {
		alw1Found = false;
		vservFound = false;
		connectorFound = false;
		inneractiveFound = false;
		hovrFound = false;
		freexterFound = false;
		greystripeFound1 = false;
		greystripeFound2 = false;
		glomoFound = false;
		
		greystripeConnectionClass = null;
		greystripeRunnerClass = null;
		greystripeStartFunc = null;
		greystripeCheckFunc = null;
		hasGsid = false;
		
		failed = false;
	}
	
	private static String escapeFileArg(String s) {
		StringBuffer sb = new StringBuffer("\'");
		
		for (char c : s.toCharArray()) {
			if (c == '\'') {
				sb.append('\\');
			}
			sb.append(c);
		}
		
		return sb.append('\'').toString();
	}
	
	private static void write(OutputStream out, InputStream in) throws Exception {
		int r;
		byte[] b = new byte[1024];
		while ((r = in.read(b)) != -1) {
			out.write(b, 0, r);
		}
	}

}
