/*
Copyright (c) 2025 Arman Jussupgaliyev
*/
package shinovon.unalw1;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ALWMethodAdapter extends MethodVisitor {

	private ALWClassAdapter classAdapter;
	private String className;
	private String superName;
	private String name;
	private String desc;
	
	private int greystripeCheck;

	public ALWMethodAdapter(ALWClassAdapter classAdapter, MethodVisitor visitor, String className, String superName, String name, String desc) {
		super(Opcodes.ASM4, visitor);
		this.classAdapter = classAdapter;
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
				// alw1: add this.startRealApp() at the end of startApp()
				
				// realAppStarted = 1;
				super.visitInsn(Opcodes.ICONST_1);
				super.visitFieldInsn(Opcodes.PUTSTATIC, this.className, "realAppStarted", "I");
				
				// this.startRealApp()
				super.visitVarInsn(Opcodes.ALOAD, 0);
				super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "startRealApp", "()V");
				System.out.println("Patched ALW1: " + className + '.' + this.name + this.desc);
				Main.alw1Found = true;
			}
		}
		super.visitInsn(opcode);
	}
	
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		// TODO handle obfuscated inneractive
		if (("vserv".equals(Main.mode)
				|| ("auto".equals(Main.mode) && className.endsWith("VservManager")))
				&& "javax/microedition/io/Connector".equals(owner)) {
			// vserv: wrap connector static calls
			System.out.println("Connector call wrapped: " + name + desc + " in " + className + '.' + this.name + this.desc);
			Main.connectorFound = true;
			owner = "UnVservConnector";
		} else if (("ia".equals(Main.mode) || "auto".equals(Main.mode))
				&& ("innerActiveStart".equals(name) || "innerActiveStartGame".equals(name)) && "()Z".equals(desc)) {
			// ia: remove innerActiveStart calls
			System.out.println("Inneractive patched (method 1): " + name + desc + " in " + className + '.' + this.name + this.desc);
			super.visitInsn(Opcodes.POP);
			super.visitInsn(Opcodes.ICONST_1);
			Main.inneractiveFound = true;
			return;
		} else if (("ia".equals(Main.mode) || "auto".equals(Main.mode))
				&& owner.endsWith("IASDK") && "start".equals(name) && "(Ljavax/microedition/midlet/MIDlet;)B".equals(desc)) {
			// ia: remove IASDK.start(MIDlet) call
			System.out.println("Inneractive patched (method 2): " + name + desc + " in " + className + '.' + this.name + this.desc);
			super.visitInsn(Opcodes.POP);
			super.visitInsn(Opcodes.ICONST_0);
			Main.inneractiveFound = true;
			return;
		} else if (("hovr".equals(Main.mode) || "auto".equals(Main.mode))
				&& "WRAPPER".equals(className) && !this.name.startsWith("startApp")
				&& name.equals("startApp") && desc.equals("()V")) {
			// hovr: rename internal start app to startApp
			System.out.println("WRAPPER real start app found: " + this.name + this.desc);
			classAdapter.renameMethod(this.name, this.desc, "startApp");
			opcode = Opcodes.INVOKESPECIAL;
			owner = superName;
		} else if (Main.freexterFound && "destroyApp".equals(this.name) && "(Z)V".equals(this.desc)
				 && "destroyApp".equals(name) && "(Z)V".equals(desc)) {
			// freexter: this.destroyApp() -> super.destroyApp
			opcode = Opcodes.INVOKESPECIAL;
			owner = superName;
		} else if (opcode == Opcodes.INVOKEVIRTUAL && Main.hasGsid && "startApp".equals(this.name) && "()V".equals(this.desc) && desc.equals("()V")) {
			Main.greystripeRunnerClass = owner;
		} else if (("glomo".equals(Main.mode) || "auto".equals(Main.mode))
				&& opcode == Opcodes.INVOKESTATIC && owner.endsWith("RegStarter") && "start".equals(name) && desc.endsWith(")V")) {
			// glomo: remove RegStarter.start(MIDlet) static call
			// TODO net lizard
			System.out.println("Glomo patched (method 1): " + name + desc + " in " + className + '.' + this.name + this.desc);
			Main.glomoFound = true;
			super.visitInsn(Opcodes.POP);
			return;
		} else if (("alw1".equals(Main.mode) || "auto".equals(Main.mode))
				&& (className.endsWith("ALW1") || className.endsWith("ALW2"))
				&& this.name.equals("startApp") && this.desc.equals("()V")
				&& opcode == Opcodes.INVOKESPECIAL && name.equals("startApp") && desc.equals("()V")) {
			// alw1: add return after super.startApp();
			super.visitMethodInsn(opcode, owner, name, desc);
			super.visitInsn(Opcodes.RETURN);
			return;
		}
		super.visitMethodInsn(opcode, owner, name, desc);
	}
	
	public void visitLdcInsn(Object cst) {
		if (Main.hasGsid && this.className.indexOf('/') != -1 && this.desc.equals("()V")
				&& cst instanceof String && cst.equals("Connection failed")) {
			// greystripe: find start function by exception message "Connection failed"
			System.out.println("Greystripe start function found: " + this.className + '.' + this.name + this.desc);
			Main.greystripeStartFunc = this.name;
		}
		super.visitLdcInsn(cst);
	}
	
	public void visitIntInsn(int opcode, int operand) {
		if (opcode == Opcodes.SIPUSH && Main.hasGsid && this.desc.equals("()Z") && this.className.indexOf('/') != -1) {
			// greystripe: find connection check function by magic numbers sequence 10002, 10004
			if (operand == 10004 && greystripeCheck == 10002) {
				System.out.println("Greystripe check function found: " + this.className + '.' + this.name + this.desc);
				Main.greystripeConnectionClass = this.className;
				Main.greystripeCheckFunc = this.name;
			} else {
				greystripeCheck = operand;
			}
		}
		super.visitIntInsn(opcode, operand);
	}

}
