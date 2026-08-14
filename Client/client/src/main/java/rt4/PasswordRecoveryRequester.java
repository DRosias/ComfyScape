package rt4;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Bridges the legacy login-screen recovery link to the private game server. */
public final class PasswordRecoveryRequester {
    private static final int RECOVERY_OPCODE = 187;
    private static final int MAX_USERNAME_LENGTH = 12;

    private PasswordRecoveryRequester() {
    }

    public static boolean handleIfRecoveryUrl(JagString url) {
        String resolved;
        try {
            resolved = url.method3127(GameShell.instance.getCodeBase()).toString();
        } catch (Exception ignored) {
            resolved = new String(url.method3148(), StandardCharsets.ISO_8859_1);
        }

        String normalizedUrl = resolved.toLowerCase(Locale.ROOT);
        boolean runescapeUrl = normalizedUrl.contains("runescape.com");
        boolean recoveryPath = normalizedUrl.contains("password")
            || normalizedUrl.contains("recover")
            || normalizedUrl.contains("accountappeal");
        if (!runescapeUrl || !recoveryPath) {
            return false;
        }

        String username = new String(Player.usernameInput.method3148(), StandardCharsets.ISO_8859_1)
            .trim()
            .toLowerCase(Locale.ROOT)
            .replace(' ', '_');
        if (!isValidUsername(username)) {
            showMessage("Enter your username on the login screen, then click the password reset link again.", JOptionPane.WARNING_MESSAGE);
            return true;
        }

        sendRequest(username);
        return true;
    }

    private static boolean isValidUsername(String username) {
        if (username.length() < 1 || username.length() > MAX_USERNAME_LENGTH) {
            return false;
        }
        for (int index = 0; index < username.length(); index++) {
            char character = username.charAt(index);
            if ((character < 'a' || character > 'z')
                && (character < '0' || character > '9')
                && character != '_') {
                return false;
            }
        }
        return true;
    }

    private static void sendRequest(final String username) {
        Thread requestThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(client.hostname, client.port), 5000);
                    byte[] encodedUsername = username.getBytes(StandardCharsets.US_ASCII);
                    socket.getOutputStream().write(RECOVERY_OPCODE);
                    socket.getOutputStream().write(encodedUsername.length);
                    socket.getOutputStream().write(encodedUsername);
                    socket.getOutputStream().flush();
                    showMessage(
                        "Password reset request sent. Contact a mod or owner IRL for approval.",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (Exception ignored) {
                    showMessage("The game server could not be reached. Try again when it is online.", JOptionPane.ERROR_MESSAGE);
                }
            }
        }, "password-recovery-request");
        requestThread.setDaemon(true);
        requestThread.start();
    }

    private static void showMessage(final String message, final int messageType) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(GameShell.frame, message, "Password Reset", messageType);
            }
        });
    }
}
