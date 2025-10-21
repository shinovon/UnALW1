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
		if (("auto".equals(Main.inst.mode) || "vserv".equals(Main.inst.mode))
				&& name.endsWith("VservManager")) {
			// TODO: vserv: check if all vserv have this class
			Main.inst.log("Found VservManager");
		}
		this.className = name;
	}
	
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.superName = superName;
		super.visit(version, access, name, signature, superName, interfaces);
	}
	
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (superName != null && !superName.equals("javax/microedition/MIDlet")) {
			boolean freexter = ("auto".equals(Main.inst.mode) || "freexter".equals(Main.inst.mode)) && "iMidlet".equals(className);
			if ("startApp".equals(name) && "()V".equals(desc)) {
				// hovr and freexter: remove wrapped startApp()
				if (("auto".equals(Main.inst.mode) || "hovr".equals(Main.inst.mode))
						&& "WRAPPER".equals(className)) {
					Main.inst.log("Found hovr WRAPPER");
					Main.inst.hovrPatched = true;
					name = "startApp_";
				} else if (freexter) {
					Main.inst.log("Patched Freexter startApp at " + className);
					Main.inst.freexterPatched = true;
					name = "startApp_";
				}
			} else if ("destroyApp".equals(name) && "(Z)V".equals(desc)) {
				// freexter: remove wrapped destroyApp()
				if (freexter) {
					Main.inst.log("Patched Freexter destroyApp at " + className);
					Main.inst.freexterPatched = true;
					name = "destroyApp_";
				}
			} else if (freexter && "fxStart".equals(name) && "()V".equals(desc)) {
				// freexter: rename fxStart to startApp
				Main.inst.log("Patched Freexter fxStart at " + className);
				Main.inst.freexterPatched = true;
				name = "startApp";
			} else if (freexter && "fxDestroy".equals(name) && "()V".equals(desc)) {
				// freexter: rename fxDestroy to destroyApp
				Main.inst.log("Patched Freexter fxDestroy at " + className);
				Main.inst.freexterPatched = true;
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
