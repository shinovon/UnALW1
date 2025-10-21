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
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;

public class Main implements Runnable {
	
	public static final String VERSION = "5.0";
	
	public static final String[] modes = {
			"auto",
			"alw1",
			"vserv",
			"ia",
			"hovr",
			"freexter",
			"gs",
			"glomo",
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
	public boolean vservContextFound;
	
	// greystripe
	public String greystripeConnectionClass;
	public String greystripeRunnerClass;
	public String greystripeStartFunc;
	public String greystripeCheckFunc;
	public boolean hasGsid;
	public boolean hasGlomoCfg;
	
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
	boolean verbose = false;
	
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
						if ("glomo".equals(mode) || "auto".equals(mode)) {
							// glomo: check for glomo.cfg resource
							hasGlomoCfg = zipFile.getEntry("glomo.cfg") != null || zipFile.getEntry("/glomo.cfg") != null;
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
														greystripePatched1 = true;
														break;
													}
												}
											}
										} else if (alw1Patched && mn.name.equals("trialEnd") && mn.desc.equals("()V")) {
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
							&& !smsPatched) {
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
							} else {
								logError("No known ad engines were detected, aborting.", false);
							}
							failed = true;
							break run;
						}
						log("Warning: No known ad engines were detected, prooceding anyway..");
					}
	
					if (noOutput) break run;
					
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

	/**
	 * Inititalize the frame
	 */
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
						File f = new File(s);
						if (f.isDirectory()) {
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
		alw1Patched = false;
		vservConnectorPatched = false;
		inneractivePatched = false;
		hovrPatched = false;
		freexterPatched = false;
		greystripePatched1 = false;
		greystripePatched2 = false;
		glomoPatched = false;
		vservContextFound = false;
		
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
