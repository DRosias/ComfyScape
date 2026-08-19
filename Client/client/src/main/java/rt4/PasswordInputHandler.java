package rt4;

import javax.swing.JPasswordField;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.Arrays;
import java.util.Base64;

/** Provides masked input for the server's password prompts. */
public final class PasswordInputHandler {
	private static final String RSA_PREFIX = "rsa:";
	private static volatile String pendingResponse;

	private PasswordInputHandler() {
	}

	public static boolean handle(int scriptId, Object[] arguments) {
		if (scriptId != 110 || arguments.length < 2 || !(arguments[1] instanceof JagString)) {
			return false;
		}
		String prompt = arguments[1].toString();
		if (!prompt.toLowerCase().contains("password")) {
			return false;
		}

		final Component parent = GameShell.frame;
		javax.swing.SwingUtilities.invokeLater(() -> {
			JPasswordField field = new JPasswordField(20);
			int result = JOptionPane.showConfirmDialog(parent, field, prompt,
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			char[] chars = result == JOptionPane.OK_OPTION ? field.getPassword() : new char[0];
			try {
				pendingResponse = encrypt(new String(chars));
			} finally {
				Arrays.fill(chars, '\0');
				field.setText("");
			}
		});
		return true;
	}

	private static String encrypt(String value) {
		Buffer encrypted = new Buffer(129);
		encrypted.p1(10);
		encrypted.pjstr(JagString.parse(value));
		encrypted.rsaenc(GlobalConfig.RSA_EXPONENT, GlobalConfig.RSA_MODULUS);
		return RSA_PREFIX + Base64.getEncoder().encodeToString(Arrays.copyOf(encrypted.data, encrypted.offset));
	}

	static void flush() {
		String value = pendingResponse;
		if (value == null) {
			return;
		}
		pendingResponse = null;
		JagString text = JagString.parse(value);
		Protocol.outboundBuffer.p1isaac(ClientProt.RESUME_P_STRINGDIALOG);
		Protocol.outboundBuffer.p1(text.length() + 1);
		Protocol.outboundBuffer.pjstr(text);
	}
}
