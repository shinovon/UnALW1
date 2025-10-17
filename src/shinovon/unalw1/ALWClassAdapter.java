/*
Copyright (c) 2025 Arman Jussupgaliyev
*/
package shinovon.unalw1;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ALWClassAdapter extends ClassVisitor {

	private String className;

	public ALWClassAdapter(ClassVisitor visitor, String name) {
		super(Opcodes.ASM4, visitor);
		this.className = name;
	}
	
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor visitor = super.visitMethod(access, name, desc, signature, exceptions);
		if (visitor != null) {
			return new ALWMethodAdapter(visitor, this.className, name, desc);
		}
		return null;
	}

}
