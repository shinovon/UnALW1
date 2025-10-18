/*
Copyright (c) 2025 Arman Jussupgaliyev
*/
package shinovon.unalw1;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ALWClassAdapter extends ClassVisitor {

	private String className;
	private String superName;

	public ALWClassAdapter(ClassVisitor visitor, String name) {
		super(Opcodes.ASM4, visitor);
		if (!"alw1".equals(Main.mode) && name.endsWith("VservManager")) {
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
		if ("WRAPPER".equals(className) && "startApp".equals(name) && "()V".equals(desc)) {
			System.out.println("Found hovr WRAPPER");
			Main.hovrFound = true;
			name = "startApp_";
		}
		MethodVisitor visitor = super.visitMethod(access, name, desc, signature, exceptions);
		if (visitor != null) {
			return new ALWMethodAdapter(visitor, this.className, this.superName, name, desc);
		}
		return null;
	}

}
