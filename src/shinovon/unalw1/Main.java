/*
Copyright (c) 2025 Arman Jussupgaliyev
*/
package shinovon.unalw1;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;

public class Main implements Runnable {
	
	public static final String VERSION = "8.2";
	
	public static final String[] modes = {
			"auto",
			"alw1",
			"vserv",
			"ia",
			"hovr",
			"freexter",
			"gs",
			"glomo",
			"lm",
			"gloft",
			"infond",
			"sm",
			"sms",
	};
	
	public static final String[] modeNames = {
			"Auto",
			"ALW1",
			"vServ",
			"Inneractive",
			"Hovr",
			"Freexter",
			"Greystripe",
			"Glomo",
			"LM",
			"Gameloft",
			"Infond",
			"Sensible Mobiles",
			"SMS",
	};
	
	static Main inst;
	
	// state
	public boolean alw1Patched;
	public boolean vservConnectorPatched;
	public boolean inneractivePatched;
	public boolean hovrPatched;
	public boolean freexterPatched;
	public boolean greystripePatched1;
	public boolean greystripePatched2;
	public boolean glomoPatched;
	public boolean smsPatched;
	public boolean lmPatched;
	public boolean vservContextFound;
	public boolean gloftPatched;
	public boolean infondPatched;
	public boolean asgatechPatched;
	public boolean smPatched;
	
	// greystripe
	public String greystripeConnectionClass;
	public String greystripeRunnerClass;
	public String greystripeStartFunc;
	public String greystripeCheckFunc;
	public boolean hasGsid;
	public boolean hasGlomoCfg;
	
	// inneractive
	public String iaRunnerClass;
	public String iaCanvasClass;
	
	// gameloft
	public String gloftCanvasClass;
	public String gloftTimeEndedFunc;
	public String gloftStartedFunc;
	public boolean hasDataIGP;
	
	// vserv
	public String startMainAppClass;
	public String vservClass;
	
	// infond
	public String infondStartFunc;
	
	// glomo
	public String glomoRegClass;
	
	Map<String, ClassNode> classNodes = new HashMap<String, ClassNode>();

	boolean running;
	
	// ui
	private JFrame frame;
	private JTextField inField;
	private JTextField proguardField;
	private JTextField libField;
	private JTextField outField;
	private static JTextArea textArea;
	
	private StringBuilder sb = new StringBuilder();
	
	// options
	String proguard;
	String libraryjars;
	String outjar;
	String outdir;
	String mode = "auto";
	boolean verbose;
	
	Object target;
	boolean cli;
	int files;
	boolean failed;
	boolean noOutput;

	public static void main(String[] args) {
		if (args.length > 0) {
			System.out.println("UnALW1 v" + VERSION + " by shinovon, 2025");
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
				} else if ("-s".equals(s)) {
					inst.noOutput = true;
				} else if ("-verbose".equals(s)) {
					inst.verbose = true;
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
		clear();
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
				Path f;
				proguard = (f = Paths.get(proguard).toAbsolutePath().normalize()).toString();
				if (!Files.exists(f)) {
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
	
	private void process(String path) {
		try (Stream<Path> paths = Files.walk(Paths.get(path))) {
			paths.filter(Files::isRegularFile).forEach(this::process);
		} catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	private void process(Path f) {
		{
			String n = f.getFileName().toString().toLowerCase();
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
					outjar = Paths.get(outdir).resolve(f.getFileName()).toString();
				} else {
					outjar = f.toString();
					outjar = outjar.substring(0, outjar.length() - 4) + "_unwrapped.jar";
				}
				
				Path temp = Files.createTempFile("unalw1", ".jar");
				try {
					try (ZipFile zipFile = new ZipFile(f.toFile())) {
						if (zipFile.getEntry("UnALW1") != null) {
							logError("Jar file appears to be already patched, aborting.", false);
							break run;
						}
						if ("gs".equals(mode) || "auto".equals(mode)) {
							// greystripe: check for .gsid resource
							if (hasGsid = zipFile.getEntry(".gsid") != null || zipFile.getEntry("/.gsid") != null) {
								log("Found .gsid, assuming Greystripe");
							}
						}
						if ("glomo".equals(mode) || "auto".equals(mode)) {
							// glomo: check for glomo.cfg resource
							if (zipFile.getEntry("glomo.cfg") != null || zipFile.getEntry("/glomo.cfg") != null) {
								hasGlomoCfg = true;
								log("Found glomo.cfg, assuming Glomo");
							} else if (zipFile.getEntry("cfg.cfg") != null || zipFile.getEntry("/cfg.cfg") != null) {
								hasGlomoCfg = true;
								log("Found cfg.cfg, assuming Glomo");
							}
						}
						if ("gloft".equals(mode) || "auto".equals(mode)) {
							// gameloft: check for dataIGP resource
							if (hasDataIGP = zipFile.getEntry("dataIGP") != null || zipFile.getEntry("/dataIGP") != null) {
								log("Found dataIGP, assuming Gameloft");
							}
						}
						
						try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)))) {
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
														greystripePatched1 = true;
														break;
													}
												}
											}
										} else if (alw1Patched && mn.name.equals("trialEnd") && mn.desc.equals("()V")) {
											// alw1: remove trialEnd() code
											log("Patched ALW1: " + className + '.' + mn.name + mn.desc);
											mn.instructions.clear();
											clearFunction(mn);
											mn.instructions.add(new InsnNode(Opcodes.RETURN));
										} else if (("auto".equals(mode) || "lm".equals(mode))
												&& className.endsWith("LMGFlow") && mn.name.equals("vStartFlow") && mn.desc.equals("()V")) {
											// lm: replace code LMGFlow.vStartFlow() to this.vMenuOpStartGame()
											log("Patched LM vStartFlow: " + className + '.' + mn.name + mn.desc);
											Main.inst.lmPatched = true;
											clearFunction(mn);
											mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
											mn.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, className, "vMenuOpStartGame", "()V"));
											mn.instructions.add(new InsnNode(Opcodes.RETURN));
										} else if (("auto".equals(mode) || "ia".equals(mode))
												&& iaCanvasClass != null && iaCanvasClass.equals(className)
												&& (mn.desc.equals("(Ljavax/microedition/midlet/MIDlet;)B") || mn.desc.equals("()B"))) {
											// ia: remove TimerTask.run() code
											log("Patched Inneractive (method 2): " + className + '.' + mn.name + mn.desc);
											Main.inst.inneractivePatched = true;
											clearFunction(mn);
											mn.instructions.add(new InsnNode(Opcodes.ICONST_0));
											mn.instructions.add(new InsnNode(Opcodes.IRETURN));
										} else if (("auto".equals(mode) || "vserv".equals(mode))
												&& startMainAppClass != null && startMainAppClass.equals(className)
												&& "startApp".equals(mn.name) && "()V".equals(mn.desc)) {
											
											InsnList ins = mn.instructions;
											boolean hasStartMainApp = false;
											for (AbstractInsnNode n = ins.getFirst(); n != null; n = n.getNext()) {
												if (n instanceof MethodInsnNode && ((MethodInsnNode) n).name.equals("startMainApp")) {
													hasStartMainApp = true;
												}
											}
											
											if (!hasStartMainApp) {
												// vserv: add startMainApp call at end of startApp
												log("Patched vServ startApp: " + className + '.' + mn.name + mn.desc);
												vservConnectorPatched = true;
												
												ins.remove(ins.getLast()); // remove return
												ins.add(new LdcInsnNode(500L));
												ins.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V"));
												ins.add(new VarInsnNode(Opcodes.ALOAD, 0));
												ins.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, className, "startMainApp", "()V"));
												ins.add(new InsnNode(Opcodes.RETURN));
											}
										} else if (("auto".equals(mode) || "infond".equals(mode))
												&& className.startsWith("infond")
												&& "startApp".equals(mn.name) && "()V".equals(mn.desc)) {
											// infond: add real start call at end of startApp
											log("Patched infond: " + className + '.' + mn.name + mn.desc);
											infondPatched = true;
											
											InsnList ins = mn.instructions;
											ins.remove(ins.getLast()); // remove return
											ins.add(new LdcInsnNode(500L));
											ins.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V"));
											ins.add(new VarInsnNode(Opcodes.ALOAD, 0));
											ins.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, className, infondStartFunc, "()V"));
											ins.add(new InsnNode(Opcodes.RETURN));
										} else if (("auto".equals(mode) || "sm".equals(mode))
												&& (("getAdsBannerInThread".equals(mn.name) && "(Z)V".equals(mn.desc)))) {
											// sm: remove getAdsBannerInThread code
											log("Patched sm: " + className + '.' + mn.name + mn.desc);
											smPatched = true;
											
											clearFunction(mn);
											mn.instructions.add(new InsnNode(Opcodes.RETURN));
										} else if (("auto".equals(mode) || "glomo".equals(mode))
												 && hasGlomoCfg && className.equals(glomoRegClass)) {
											if ("()Z".equals(mn.desc) || "(Z)Z".equals(mn.desc)) {
												InsnList ins = mn.instructions;
												boolean hasGoodLdc = false;
												boolean hasCompareTo = false;
												for (AbstractInsnNode n = ins.getFirst(); n != null; n = n.getNext()) {
													// parseLong() == ActivationKey()
													if (n.getOpcode() == Opcodes.INVOKESTATIC && n instanceof MethodInsnNode
															&& ((MethodInsnNode) n).name.equals("parseLong")
															&& (n = n.getNext()) instanceof MethodInsnNode && ((MethodInsnNode) n).desc.equals("()J")
															&& (n = n.getNext()) != null && n.getOpcode() == Opcodes.LCMP
															&& (n = n.getNext()) != null && n.getOpcode() == Opcodes.IFNE
															&& (n = n.getNext()) != null && n.getOpcode() == Opcodes.ICONST_1) {
														// glomo: replace isRegistered() and isSubscribed() code to always return true
														log("Glomo patched (method 2, isRegistered or isSubscribed): " + className + '.' + mn.name + mn.desc);
														glomoPatched = true;
														
														clearFunction(mn);
														mn.instructions.add(new InsnNode(Opcodes.ICONST_1));
														mn.instructions.add(new InsnNode(Opcodes.IRETURN));
														break;
													}
													if (n.getOpcode() == Opcodes.LDC && n instanceof LdcInsnNode
															&& "good".equals(((LdcInsnNode) n).cst)) {
														hasGoodLdc = true;
													}
													if (n.getOpcode() == Opcodes.INVOKEVIRTUAL && n instanceof MethodInsnNode
															&& ((MethodInsnNode) n).name.equals("compareTo")) {
														hasCompareTo = true;
													}
												}
												if (hasGoodLdc && hasCompareTo) {
													// glomo net lizard: isActivated()
													log("Glomo patched (method 2, isActivated): " + className + '.' + mn.name + mn.desc);
													glomoPatched = true;
													
													clearFunction(mn);
													mn.instructions.add(new InsnNode(Opcodes.ICONST_1));
													mn.instructions.add(new InsnNode(Opcodes.IRETURN));
													break;
												}
											}
											if ("(Ljava/lang/String;I)Z".equals(mn.desc)) {
												// glomo: find checkSerial()
												InsnList ins = mn.instructions;
												for (AbstractInsnNode n = ins.getFirst(); n != null; n = n.getNext()) {
													// parseLong() == ActivationKey()
													if (n.getOpcode() == Opcodes.INVOKEVIRTUAL && n instanceof MethodInsnNode
															&& ((MethodInsnNode) n).name.equals("getTime")) {
														log("Glomo patched (method 2, checkSerial): " + className + '.' + mn.name + mn.desc);
														glomoPatched = true;
														
														clearFunction(mn);
														mn.instructions.add(new InsnNode(Opcodes.ICONST_1));
														mn.instructions.add(new InsnNode(Opcodes.IRETURN));
														break;
													}
												}
											}
											if ("()Ljava/lang/String;".equals(mn.desc) || "(Ljava/lang/String;)Ljava/lang/String;".equals(mn.desc)) {
												log("Glomo patched (method 2, string): " + className + '.' + mn.name + mn.desc);
												clearFunction(mn);
												mn.instructions.add(new LdcInsnNode(""));
												mn.instructions.add(new InsnNode(Opcodes.ARETURN));
											}
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
								} else if (!noOutput) {
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
													greystripePatched2 = true;
													break;
												}
												ins.remove(n);
											}
											break;
										}
									}
								// gameloft canvas class
								} else if (gloftCanvasClass != null && className.equals(gloftCanvasClass)) {
									for (Object m : node.methods) {
										MethodNode mn = (MethodNode) m;
										
										mtd: {
											if (mn.desc.equals("()Z")) {
												InsnList ins = mn.instructions;
												for (AbstractInsnNode n : ins.toArray()) {
													// patch getAppProperty to remove dependency on jad
													if (n.getOpcode() == Opcodes.INVOKEVIRTUAL && "getAppProperty".equals(((MethodInsnNode) n).name)) {
														String ldc = (String) ((LdcInsnNode) n.getPrevious()).cst;
														String replace = null;
														if ("SMS-Default-Profile".equals(ldc)
																|| "SMS-Default-Language".equals(ldc)
																|| "SMS-ConfigFromJad".equals(ldc)
																|| "SMS-GameCodeIGP".equals(ldc)
																|| "SMS-PhoneModel".equals(ldc)
																|| "SMS-Profiles".equals(ldc)) {
															replace = "0";
														} else if ("SMS-DemoTime".equalsIgnoreCase(ldc)) {
															replace = Integer.toString(Integer.MAX_VALUE);
														} else {
															continue;
														}
														
														ins.remove(n.getPrevious()); // pop ldc
														ins.remove(n.getPrevious()); // pop getfield
														if (replace == null) {
															ins.set(n, new InsnNode(Opcodes.ACONST_NULL));
														} else {
															ins.set(n, new LdcInsnNode(replace));
														}
														log("Patched getAppProperty(" + ldc + ") to " + replace + " at " + className + '.' + mn.name + mn.desc);
													}
												}
											}
											
											if (!mn.desc.equals("()V"))
												continue;
											
											InsnList ins = mn.instructions;
											boolean hasSetCurrent = false,
													hasSetFullScreen = false,
													hasPauseApp = false,
													hasRepaint = false;
											for (AbstractInsnNode n : ins.toArray()) {
												if (n.getOpcode() == Opcodes.INVOKEVIRTUAL) {
													if ("setCurrent".equals(((MethodInsnNode) n).name)) hasSetCurrent = true;
													if ("setFullScreenMode".equals(((MethodInsnNode) n).name)) hasSetFullScreen = true;
													if ("pauseApp".equals(((MethodInsnNode) n).name)) hasPauseApp = true;
													if ("repaint".equals(((MethodInsnNode) n).name)) hasRepaint = true;
												}
											}
											
											// start game immediately after demo canvas initialization
											if (hasSetCurrent && hasSetFullScreen) {
												log("Gameloft demo patched: " + className + '.' + mn.name + mn.desc);
												gloftPatched = true;
												
												ins.remove(ins.getLast()); // remove return
												ins.add(new VarInsnNode(Opcodes.ALOAD, 0));
												ins.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, className, this.gloftStartedFunc, "()V"));
												ins.add(new InsnNode(Opcodes.RETURN));
												break mtd;
											}
											
											// delete time ended function code, just in case
											if (mn.name.equals(this.gloftTimeEndedFunc)
													|| (hasRepaint && hasPauseApp)) {
												log("Gameloft demo timer patched: " + className + '.' + mn.name + mn.desc);
												gloftPatched = true;
												
												clearFunction(mn);
												mn.instructions.add(new InsnNode(Opcodes.RETURN));
												continue;
											}
										}
									}
								}
								
								if (noOutput) continue;
								if (verbose) log("Writing " + className);
	
								ClassWriter classWriter = new ClassWriter(0);
								node.accept(classWriter);
								zipOut.putNextEntry(new ZipEntry(className + ".class"));
								zipOut.write(classWriter.toByteArray());
								zipOut.closeEntry();
							}
							
							// add unvserv connector classes
							if (vservConnectorPatched && !noOutput) {
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
							
							if (!noOutput) {
								ZipEntry copy = new ZipEntry("UnALW1");
								copy.setCompressedSize(-1);
								zipOut.putNextEntry(copy);
								zipOut.write(("UnALW1 v" + VERSION + " (https://github.com/shinovon/UnALW1)").getBytes());
								zipOut.closeEntry();
							}
						}
					}
	
					if (greystripePatched1) {
						log("Warning: Greystripe may be unwrapped partially");
					} else if (!vservConnectorPatched
							&& !alw1Patched
							&& !inneractivePatched
							&& !hovrPatched
							&& !freexterPatched
							&& !greystripePatched2
							&& !glomoPatched
							&& !lmPatched
							&& !infondPatched
							&& !asgatechPatched
							&& !smPatched) {
						if (hasGsid) {
							logError("Greystripe was detected, but could not patch it, please report to developer!", false);
							failed = true;
							break run;
						} else if (hasGlomoCfg) {
							logError("Glomo was detected, but could not patch it", false);
							failed = true;
							break run;
						} else if (!vservConnectorPatched) {
							if (vservContextFound) {
								logError("vServ was detected, but could not patch it, please report to developer!", false);
								failed = true;
								break run;
							} else if (smsPatched) {
							} else {
								logError("No known ad engines were detected, aborting.", false);
								failed = true;
								break run;
							}
						} else {
							log("Warning: No known ad engines were detected, prooceding anyway..");
						}
					}
	
					if (noOutput) break run;
					
					// preverify with proguard
					Path tempConfig = Files.createTempFile("unalw1", ".cfg");
					try {
						try (PrintStream ps = new PrintStream(new FileOutputStream(tempConfig.toFile()))) {
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
							ps.println(temp.toAbsolutePath().toString());
							ps.print("-outjar ");
							ps.println(escapeFileArg(Paths.get(outjar).toAbsolutePath().normalize().toString()));
						}
						
						List<String> args = new ArrayList<String>();
						args.add(Paths.get(System.getProperty("java.home")).resolve("bin").resolve("java").toString());
						args.add("-jar");
						args.add(proguard);
						args.add("@" + tempConfig.toAbsolutePath().toString());
						
						ProcessBuilder builder = new ProcessBuilder(args);
						builder.inheritIO();
						
						if (builder.start().waitFor() != 0) {
							logError("Proguard failed", false);
							failed = true;
							break run;
						}
					} finally {
						Files.delete(tempConfig);
					}
					
					log("Wrote to " + outjar);
				} finally {
					Files.delete(temp);
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
		System.out.println(s);
		if (textArea == null) return;
		sb.append(s).append('\n');
		if (b) textArea.setText(sb.toString());
	}
	
	void log(String s) {
		log(s, false);
	}
	
	void log(String s, boolean b) {
		System.out.println(s);
		if (textArea == null) return;
		sb.append(s).append('\n');
		if (b) textArea.setText(sb.toString());
	}
	
	void clear() {
		if (textArea == null) return;
		sb.setLength(0);
		textArea.setText("");
	}

	void initializeUI() {
		frame = new JFrame();
		frame.setTitle("UnAWL1 v" + VERSION);
		frame.setBounds(100, 100, 350, 536);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new GridLayout(0, 1, 0, 0));
		
		JPanel panel_1 = new JPanel();
		frame.getContentPane().add(panel_1);
		panel_1.setLayout(new BorderLayout(5, 0));
		
		JPanel panel = new JPanel();
		panel_1.add(panel, BorderLayout.NORTH);
		panel.setLayout(new GridLayout(0, 1, 0, 0));
		
		JPanel panel_3 = new JPanel();
		panel.add(panel_3);
		panel_3.setLayout(new BorderLayout(0, 0));
		
		JLabel lblInput = new JLabel("Input: ");
		panel_3.add(lblInput, BorderLayout.WEST);
		
		inField = new JTextField();
		inField.setText("");
		inField.setToolTipText("Input jar file or directory path");
		panel_3.add(inField, BorderLayout.CENTER);
		inField.setColumns(10);
		
//		JButton btnNewButton = new JButton("...");
//		btnNewButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//			}
//		});
//		panel_3.add(btnNewButton, BorderLayout.EAST);
		
		JPanel panel_6 = new JPanel();
		panel.add(panel_6);
		panel_6.setLayout(new BorderLayout(0, 0));
		
		JLabel lblNewLabel_2 = new JLabel("Output: ");
		panel_6.add(lblNewLabel_2, BorderLayout.WEST);
		
		outField = new JTextField();
		outField.setText("");
		outField.setToolTipText("Output jar file or directory path, may be left empty");
		panel_6.add(outField, BorderLayout.CENTER);
		outField.setColumns(10);
		
//		JButton btnNewButton_3 = new JButton("...");
//		btnNewButton_3.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//			}
//		});
//		panel_6.add(btnNewButton_3, BorderLayout.EAST);
		
		JPanel panel_4 = new JPanel();
		panel.add(panel_4);
		panel_4.setLayout(new BorderLayout(0, 0));
		
		JLabel lblNewLabel = new JLabel("Proguard jar: ");
		panel_4.add(lblNewLabel, BorderLayout.WEST);
		
		proguardField = new JTextField();
		proguardField.setText("");
		proguardField.setToolTipText("Path to proguard.jar, e.g: C:\\proguard-7.7.0\\lib\\proguard.jar");
		panel_4.add(proguardField, BorderLayout.CENTER);
		proguardField.setColumns(10);
		
//		JButton btnNewButton_1 = new JButton("...");
//		btnNewButton_1.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//			}
//		});
//		panel_4.add(btnNewButton_1, BorderLayout.EAST);
		
		JPanel panel_5 = new JPanel();
		panel.add(panel_5);
		panel_5.setLayout(new BorderLayout(0, 0));
		
		JLabel lblNewLabel_1 = new JLabel("MIDP libraries: ");
		panel_5.add(lblNewLabel_1, BorderLayout.WEST);
		
		libField = new JTextField();
		libField.setText("");
		libField.setToolTipText("Path to folder with MIDP libraries, e.g: C:\\Nokia\\Devices\\S40_5th_Edition_SDK\\lib");
		panel_5.add(libField, BorderLayout.CENTER);
		libField.setColumns(10);
		
//		JButton btnNewButton_2 = new JButton("...");
//		btnNewButton_2.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//			}
//		});
//		panel_5.add(btnNewButton_2, BorderLayout.EAST);
		
		JPanel panel_7 = new JPanel();
		panel.add(panel_7);
		panel_7.setLayout(new BorderLayout(0, 0));
		
		JComboBox comboBox = new JComboBox();
		comboBox.setModel(new DefaultComboBoxModel(modeNames));
		panel_7.add(comboBox);
		
		JLabel lblNewLabel_3 = new JLabel("Mode: ");
		panel_7.add(lblNewLabel_3, BorderLayout.WEST);
		
		JPanel panel_2 = new JPanel();
		panel.add(panel_2);
		panel_2.setLayout(new BorderLayout(0, 0));
		
		JButton openBtn = new JButton("Start");
		panel_2.add(openBtn);
		
		openBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (running) return;
				target = inField.getText();
				String s = outField.getText();
				try {
					if (s.trim().isEmpty()) {
						outdir = null;
						outjar = null;
					} else {
						Path f = Paths.get(s);
						if (Files.isDirectory(f)) {
							outdir = s;
							outjar = null;
						} else {
							outdir = null;
							outjar = s;
						}
					}
				} catch (Exception ignored) {}
				proguard = proguardField.getText();
				libraryjars = libField.getText();
				mode = modes[comboBox.getSelectedIndex()];
				new Thread(Main.this).start();
			}
		});
		
		JScrollPane scrollPane = new JScrollPane();
		panel_1.add(scrollPane, BorderLayout.CENTER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setText("");
		scrollPane.setViewportView(textArea);
		
		DropTarget dropTarget = new DropTarget(textArea, DnDConstants.ACTION_COPY_OR_MOVE, null);
		try {
			dropTarget.addDropTargetListener(new DropTargetListener() {

				@Override
				public void dragEnter(DropTargetDragEvent dtde) {
				}

				@Override
				public void dragOver(DropTargetDragEvent dtde) {
				}

				@Override
				public void dropActionChanged(DropTargetDragEvent dtde) {
				}

				@Override
				public void dragExit(DropTargetEvent dte) {
				}

				@Override
				public void drop(DropTargetDropEvent dtde) {
					if (running) return;
					try {
						Transferable t = dtde.getTransferable();
						if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
							dtde.acceptDrop(dtde.getDropAction());
							List transferData = (List) t.getTransferData(DataFlavor.javaFileListFlavor);
							
	                        if (transferData != null && transferData.size() > 0) {
	                        	StringBuilder sb = new StringBuilder();
	                        	for (Object s : transferData) {
	                        		sb.append(s).append(File.pathSeparatorChar);
	                        	}
	                        	sb.setLength(sb.length() - 1);
	                        	inField.setText(sb.toString());
	                            dtde.dropComplete(true);
	                        }
						} else {
		                    dtde.rejectDrop();
		                }
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void resetState() {
		// note: neep in sync with state variables
		
		alw1Patched = false;
		vservConnectorPatched = false;
		inneractivePatched = false;
		hovrPatched = false;
		freexterPatched = false;
		greystripePatched1 = false;
		greystripePatched2 = false;
		glomoPatched = false;
		smsPatched = false;
		lmPatched = false;
		vservContextFound = false;
		gloftPatched = false;
		infondPatched = false;
		asgatechPatched = false;
		smPatched = false;
		
		greystripeConnectionClass = null;
		greystripeRunnerClass = null;
		greystripeStartFunc = null;
		greystripeCheckFunc = null;
		hasGsid = false;
		hasGlomoCfg = false;
		
		iaRunnerClass = null;
		iaCanvasClass = null;
		
		gloftCanvasClass = null;
		gloftTimeEndedFunc = null;
		gloftStartedFunc = null;
		hasDataIGP = false;
		
		startMainAppClass = null;
		vservClass = null;
		
		infondStartFunc = null;
		
		failed = false;
		
		classNodes.clear();
	}
	
	private static void clearFunction(MethodNode mn) {
		mn.instructions.clear();
		try {
			mn.tryCatchBlocks.clear();
		} catch (Exception ignored) {}
		try {
			mn.localVariables.clear();
		} catch (Exception ignored) {}
	}
	
	private static String escapeFileArg(String s) {
		StringBuffer sb = new StringBuffer("\'");
		
		for (char c : s.toCharArray()) {
			if (c == '\'') {
				continue;
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
