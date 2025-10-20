/*
Copyright (c) 2025 Arman Jussupgaliyev
*/
package shinovon.unalw1;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ALWClassAdapter extends ClassVisitor {

	private List<String[]> rename;
	private String className;
	private String superName;

	public ALWClassAdapter(ClassVisitor visitor, String name) {
		super(Opcodes.ASM4, visitor);
		if (("auto".equals(Main.mode) || "vserv".equals(Main.mode))
				&& name.endsWith("VservManager")) {
			// TODO: vserv: check if all vserv have this class
			System.out.println("Found VservManager");
			Main.vservFound = true;
		}
		this.className = name;
	}
	
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.superName = superName;
		super.visit(version, access, name, signature, superName, interfaces);
	}
	
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (superName != null && !superName.equals("javax/microedition/MIDlet")) {
			boolean freexter = ("auto".equals(Main.mode) || "freexter".equals(Main.mode)) && "iMidlet".equals(className);
			if ("startApp".equals(name) && "()V".equals(desc)) {
				// hovr and freexter: remove wrapped startApp()
				if (("auto".equals(Main.mode) || "hovr".equals(Main.mode))
						&& "WRAPPER".equals(className)) {
					System.out.println("Found hovr WRAPPER");
					Main.hovrFound = true;
					name = "startApp_";
				} else if (freexter) {
					System.out.println("Found freexter");
					Main.freexterFound = true;
					name = "startApp_";
				}
			} else if ("destroyApp".equals(name) && "(Z)V".equals(desc)) {
				// freexter: remove wrapped destroyApp()
				if (freexter) {
					Main.freexterFound = true;
					name = "destroyApp_";
				}
			} else if (freexter && "fxStart".equals(name) && "()V".equals(desc)) {
				// freexter: rename fxStart to startApp
				Main.freexterFound = true;
				name = "startApp";
			} else if (freexter && "fxDestroy".equals(name) && "()V".equals(desc)) {
				// freexter: rename fxDestroy to destroyApp
				Main.freexterFound = true;
				name = "destroyApp";
				desc = "(Z)V";
			}
		}
		MethodVisitor visitor = super.visitMethod(access, name, desc, signature, exceptions);
		if (visitor != null) {
			return new ALWMethodAdapter(this, visitor, this.className, this.superName, name, desc);
		}
		return null;
	}
	
	protected void renameMethod(String name, String desc, String newName) {
		if (rename == null) {
			rename = new ArrayList<String[]>();
		}
		rename.add(new String[] { name, desc, newName });
	}
	
	public List<String[]> getRenameList() {
		return rename;
	}

}
