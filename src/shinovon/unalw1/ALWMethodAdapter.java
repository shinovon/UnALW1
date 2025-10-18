/*
Copyright (c) 2025 Arman Jussupgaliyev
*/
package shinovon.unalw1;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ALWMethodAdapter extends MethodVisitor {

	private String className;
	private String superName;
	private String name;
	private String desc;

	public ALWMethodAdapter(MethodVisitor visitor, String className, String superName, String name, String desc) {
		super(Opcodes.ASM4, visitor);
		this.className = className;
		this.superName = superName;
		this.name = name;
		this.desc = desc;
	}
	
	public void visitInsn(int opcode) {
		if (("alw1".equals(Main.mode) || "auto".equals(Main.mode))
				&& (className.endsWith("ALW1") || className.endsWith("ALW2"))
				&& name.equals("startApp") && desc.equals("()V")) {
			if (opcode == Opcodes.RETURN) {
				super.visitVarInsn(Opcodes.ALOAD, 0);
				super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "startRealApp", "()V");
				System.out.println("Patched: " + className + '.' + this.name + this.desc);
				Main.alw1Found = true;
			}
		}
		super.visitInsn(opcode);
	}
	
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		if (("vserv".equals(Main.mode)
				|| ("auto".equals(Main.mode) && className.endsWith("VservManager")))
				&& "javax/microedition/io/Connector".equals(owner)) {
			System.out.println("Connector call wrapped: " + name + desc + " in " + className + '.' + this.name + this.desc);
			Main.connectorFound = true;
			owner = "UnVservConnector";
		} else if (("ia".equals(Main.mode) || "auto".equals(Main.mode))
				&& ("innerActiveStart".equals(name) || "innerActiveStartGame".equals(name)) && "()Z".equals(desc)) {
			System.out.println("Inneractive patched: " + name + desc + " in " + className + '.' + this.name + this.desc);
			super.visitInsn(Opcodes.POP);
			super.visitInsn(Opcodes.ICONST_1);
			Main.inneractiveFound = true;
			return;
		} else if (("hovr".equals(Main.mode) || "auto".equals(Main.mode))
				&& "WRAPPER".equals(className) && !this.name.startsWith("startApp")
				&& name.equals("startApp") && desc.equals("()V")) {
			System.out.println("WRAPPER real start app found: " + this.name + this.desc);
			Main.wrapperStartMethod = this.name;
			opcode = Opcodes.INVOKESPECIAL;
			owner = superName;
		}
		super.visitMethodInsn(opcode, owner, name, desc);
	}

}
