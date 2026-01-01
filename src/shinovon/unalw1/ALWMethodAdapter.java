/*
Copyright (c) 2025 Arman Jussupgaliyev

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
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
	
	private Object lastLdc;

	public ALWMethodAdapter(ALWClassAdapter classAdapter, MethodVisitor visitor, String className, String superName, String name, String desc) {
		super(Opcodes.ASM4, visitor);
		this.classAdapter = classAdapter;
		this.className = className;
		this.superName = superName;
		this.name = name;
		this.desc = desc;
	}
	
	@SuppressWarnings("deprecation")
	public void visitInsn(int opcode) {
		if (("alw1".equals(Main.inst.mode) || "auto".equals(Main.inst.mode))
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
				Main.inst.log("Patched ALW1: " + className + '.' + this.name + this.desc);
				Main.inst.alw1Patched = true;
			}
		}
		super.visitInsn(opcode);
	}
	
	@SuppressWarnings("deprecation")
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		if (("vserv".equals(Main.inst.mode)
				|| ("auto".equals(Main.inst.mode)
						&& (className.endsWith("VservManager") || className.endsWith("VservAd") || className.endsWith("VSERV_BCI_CLASS_000")
								|| className.equals(Main.inst.vservClass))))
				&& "javax/microedition/io/Connector".equals(owner)) {
			// vserv: wrap connector static calls
			Main.inst.log("Connector call wrapped: " + name + desc + " in " + className + '.' + this.name + this.desc);
			Main.inst.vservConnectorPatched = true;
			owner = "UnVservConnector";
		} else if (("ia".equals(Main.inst.mode) || "auto".equals(Main.inst.mode))
				&& ("innerActiveStart".equals(name) || "innerActiveStartGame".equals(name)) && "()Z".equals(desc)) {
			// ia: remove innerActiveStart calls
			Main.inst.log("Inneractive patched (method 1): " + name + desc + " in " + className + '.' + this.name + this.desc);
			super.visitInsn(Opcodes.POP);
			super.visitInsn(Opcodes.ICONST_1);
			Main.inst.inneractivePatched = true;
			return;
		} else if (("ia".equals(Main.inst.mode) || "auto".equals(Main.inst.mode))
				&& owner.endsWith("IASDK") && "start".equals(name) && "(Ljavax/microedition/midlet/MIDlet;)B".equals(desc)) {
			// ia: remove IASDK.start(MIDlet) call
			Main.inst.log("Inneractive patched (method 2): " + name + desc + " in " + className + '.' + this.name + this.desc);
			super.visitInsn(Opcodes.POP);
			super.visitInsn(Opcodes.ICONST_0);
			Main.inst.inneractivePatched = true;
			return;
		} else if (("hovr".equals(Main.inst.mode) || "auto".equals(Main.inst.mode))
				&& "WRAPPER".equals(className) && !this.name.startsWith("startApp")
				&& name.equals("startApp") && desc.equals("()V")) {
			// hovr: rename internal start app to startApp
			Main.inst.log("WRAPPER real start app found: " + this.name + this.desc);
			classAdapter.renameMethod(this.name, this.desc, "startApp");
			opcode = Opcodes.INVOKESPECIAL;
			owner = superName;
		} else if (Main.inst.freexterPatched && "destroyApp".equals(this.name) && "(Z)V".equals(this.desc)
				 && "destroyApp".equals(name) && "(Z)V".equals(desc)) {
			// freexter: this.destroyApp() -> super.destroyApp
			opcode = Opcodes.INVOKESPECIAL;
			owner = superName;
		} else if (opcode == Opcodes.INVOKEVIRTUAL && Main.inst.hasGsid && "startApp".equals(this.name) && "()V".equals(this.desc) && desc.equals("()V")) {
			Main.inst.greystripeRunnerClass = owner;
		} else if (("glomo".equals(Main.inst.mode) || "auto".equals(Main.inst.mode))
				&& opcode == Opcodes.INVOKESTATIC && owner.endsWith("RegStarter") && "start".equals(name) && desc.endsWith(")V")) {
			// glomo: remove RegStarter.start(MIDlet) static call
			Main.inst.log("Glomo patched (method 1): " + name + desc + " in " + className + '.' + this.name + this.desc);
			Main.inst.glomoPatched = true;
			super.visitInsn(Opcodes.POP);
			return;
		} else if (("alw1".equals(Main.inst.mode) || "auto".equals(Main.inst.mode))
				&& (className.endsWith("ALW1") || className.endsWith("ALW2"))
				&& this.name.equals("startApp") && this.desc.equals("()V")
				&& opcode == Opcodes.INVOKESPECIAL && name.equals("startApp") && desc.equals("()V")) {
			// alw1: add return after super.startApp();
			super.visitMethodInsn(opcode, owner, name, desc);
			super.visitInsn(Opcodes.RETURN);
			return;
		} else if (("sms".equals(Main.inst.mode) || "auto".equals(Main.inst.mode))
				&& owner.equals("javax/wireless/messaging/MessageConnection") && name.equals("send")) {
			// remove sms send
			Main.inst.log("Patched SMS send: " + name + desc + " in " + className + '.' + this.name + this.desc);
			Main.inst.smsPatched = true;
			super.visitInsn(Opcodes.POP);
			super.visitInsn(Opcodes.POP);
			return;
		} else if (("lm".equals(Main.inst.mode) || "auto".equals(Main.inst.mode))
				&& className.endsWith("LMGFlow") && this.name.equals("vMenuOpStartGame")
				&& name.equals("checkLicense") && desc.equals("(Z)Z")) {
			// lm: remove checkLicense() call
			Main.inst.log("Patched LM checkLicense: " + name + desc + " in " + className + '.' + this.name + this.desc);
			Main.inst.lmPatched = true;
			super.visitInsn(Opcodes.POP);
			return;
		} else if (("gloft".equals(Main.inst.mode) || "auto".equals(Main.inst.mode))
				&& Main.inst.hasDataIGP && !this.name.equals("startApp")
				&& name.equals("startApp") && desc.equals("()V")) {
			// gameloft
			Main.inst.log("startApp caller found: " + this.className + '.' + this.name + this.desc);
			Main.inst.gloftCanvasClass = this.className;
			Main.inst.gloftStartedFunc = this.name;
		} else if ("auto".equals(Main.inst.mode)
				&& name.equals("checkExpiration") && desc.equals("()Z")) {
			// asgatech: patch checkExpiration() call
			Main.inst.log("Time bomb patched: " + name + desc + " in " + className + '.' + this.name + this.desc);
			super.visitInsn(Opcodes.POP);
			super.visitInsn(Opcodes.ICONST_1);
			Main.inst.asgatechPatched = true;
			return;
		} else if (("infond".equals(Main.inst.mode) || "auto".equals(Main.inst.mode))
				&& name.equals("iaa") && desc.equals("()Z")
				&& Main.inst.infondStartFunc == null) {
			// infond
			Main.inst.log("Found infond wrapper: " + owner + " in " + className + '.' + this.name + this.desc);
			Main.inst.infondStartFunc = this.name;
		} else if (("sm".equals(Main.inst.mode) || "auto".equals(Main.inst.mode))
				&& name.equals("showAtStart") && desc.equals("()V")
				&& "javax/microedition/midlet/MIDlet".equals(superName)) {
			// sm: replace showAtStart() call to startMainApp()
			// TODO check if startMainApp exists
			Main.inst.log("Patched sm: " + name + desc + " in " + className + '.' + this.name + this.desc);
			super.visitInsn(Opcodes.POP);
			super.visitVarInsn(Opcodes.ALOAD, 0);
			super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "startMainApp", "()V");
			Main.inst.smPatched = true;
			return;
		} else if ("auto".equals(Main.inst.mode)
				&& name.equals("getAppProperty") && desc.equals("(Ljava/lang/String;)Ljava/lang/String;")
				&& "Demo-Time".equals(lastLdc)) {
			// patch Demo-Time
			Main.inst.log("Patched getAppProperty(Demo-Time): " + className + '.' + this.name + this.desc);
			super.visitInsn(Opcodes.POP);
			super.visitInsn(Opcodes.POP);
			super.visitLdcInsn(Integer.toString(Integer.MAX_VALUE / 1000));
			return;
		}
		super.visitMethodInsn(opcode, owner, name, desc);
	}
	
	public void visitLdcInsn(Object cst) {
		lastLdc = cst;
		if (cst instanceof String && cst.equals("Connection failed")
				&& Main.inst.hasGsid && this.className.indexOf('/') != -1 && this.desc.equals("()V") ) {
			// greystripe: find start function by exception message "Connection failed"
			Main.inst.log("Greystripe start function found: " + this.className + '.' + this.name + this.desc);
			Main.inst.greystripeStartFunc = this.name;
		} else if (cst instanceof String && cst.equals("X-VSERV-CONTEXT")) {
			// vserv
			Main.inst.log("vServ string constant found: " + this.className + '.' + this.name + this.desc);
			Main.inst.vservContextFound = true;
			Main.inst.vservClass = className;
		} else if (cst instanceof String && cst.equals("GlowingMobile")) {
			// glomo
			Main.inst.hasGlomoCfg = true;
			Main.inst.log("GlomoDistributer string constant found: " + this.className + '.' + this.name + this.desc);
		} else if (cst instanceof String && cst.equals("IA-X-errorInDisclaimerNotice") && name.equals("<init>")) {
			// ia
			Main.inst.log("Inneractive string constant found: " + this.className + '.' + this.name + this.desc);
			Main.inst.iaRunnerClass = className;
		} else if (cst instanceof String && cst.equals("IA-X-contentName")) {
			// ia
			Main.inst.log("Inneractive string constant found: " + this.className + '.' + this.name + this.desc);
			Main.inst.iaCanvasClass = className;
		} else if (cst instanceof String && cst.equals("Demo Time ended!")) {
			// gameloft
			Main.inst.log("Gameloft string constant found (demo time ended): " + this.className + '.' + this.name + this.desc);
			Main.inst.gloftCanvasClass = this.className;
			Main.inst.gloftTimeEndedFunc = this.name;
		} else if (cst instanceof String && cst.equals("Wrapped GAME Started!")) {
			// gameloft
			Main.inst.log("Gameloft string constant found (wrapped game started): " + this.className + '.' + this.name + this.desc);
			Main.inst.gloftCanvasClass = this.className;
			Main.inst.gloftStartedFunc = this.name;
		} else if (cst instanceof String && (cst.equals("+lm") || cst.equals("Prefix: ["))
				&& !this.name.equals("<init>")) {
			// glomo
			Main.inst.log("GlomoRegistrator string constant found: " + this.className + '.' + this.name + this.desc);
			Main.inst.glomoRegClass = className;
		} else if (cst instanceof String && (cst.equals("M7-URI") || cst.equals("PCS-Game-Lobby-API"))
				&& this.name.equals("<init>")) {
			// m7
			Main.inst.log("M7 string constant found: " + this.className + '.' + this.name + this.desc);
			Main.inst.m7Class = className;
		}
		super.visitLdcInsn(cst);
	}
	
	public void visitIntInsn(int opcode, int operand) {
		if (opcode == Opcodes.SIPUSH && Main.inst.hasGsid && this.desc.equals("()Z") && this.className.indexOf('/') != -1) {
			// greystripe: find connection check function by magic numbers sequence 10002, 10004
			if (operand == 10004 && greystripeCheck == 10002) {
				Main.inst.log("Greystripe check function found: " + this.className + '.' + this.name + this.desc);
				Main.inst.greystripeConnectionClass = this.className;
				Main.inst.greystripeCheckFunc = this.name;
			} else {
				greystripeCheck = operand;
			}
		}
		super.visitIntInsn(opcode, operand);
	}

}
