package content.region.misthalin.lumbridge.dialogue;

import core.game.dialogue.DialoguePlugin;
import core.game.dialogue.FacialExpression;
import core.game.node.entity.npc.NPC;
import core.game.node.entity.player.Player;
import core.game.node.entity.npc.drop.NPCDropTables;
import core.plugin.Initializable;

/**
 * Hans provides player-facing server options.
 */
@Initializable
public final class HansDialoguePlugin extends DialoguePlugin {
	public static final String ADULT_VERIFIED = "/save:hans:adult-verified";
	public static final String HAS_CONFIRMED_HANS_SETUP = "/save:hans:setup-confirmed";
	public static final String TELEPORT_UNLOCK_BYPASS_ENABLED = "/save:teleportUnlockBypassEnabled";
	private static final String SETUP_REMINDER_SHOWN = "hans:setup-reminder-shown";
	private static final String SETUP_REMINDER = "Reminder: Talk to Hans at home to choose your XP rate and optional server settings. If you want 1x XP, you can choose that there too.";
	private static final String VERIFICATION_RETRY_AT = "/save:hans:adult-verification-retry-at";
	private static final long VERIFICATION_RETRY_DELAY = 30_000L;

	public HansDialoguePlugin() {
	}

	public HansDialoguePlugin(Player player) {
		super(player);
	}

	@Override
	public DialoguePlugin newInstance(Player player) {
		return new HansDialoguePlugin(player);
	}

	@Override
	public boolean open(Object... args) {
		npc = (NPC) args[0];
		interpreter.sendDialogues(npc, FacialExpression.NEUTRAL, "Hello! What server option can I help with?");
		stage = 0;
		return true;
	}

	@Override
	public boolean handle(int interfaceId, int buttonId) {
		switch (stage) {
			case 0:
				showMainOptions();
				stage = 1;
				break;
			case 1:
				if (buttonId == 1) {
					npc("Your current XP rate is: " + player.getSkills().experienceMultiplier + "x.");
					stage = 10;
				} else if (isAdultVerified() && buttonId == 2) {
					showAutoDropCommandAccess();
					stage = 30;
				} else if (isAdultVerified() && buttonId == 3) {
					showTeleportUnlockBypass();
					stage = 40;
				} else if (!isAdultVerified() && buttonId == 2) {
					startVerification();
				} else {
					end();
				}
				break;
			case 10:
				showExperienceOptions();
				stage = 11;
				break;
			case 11:
				setExperienceRate(buttonId);
				break;
			case 20:
				interpreter.sendOptions("Adult verification: question 1 of 3",
						"W-2", "1099", "W-4", "I-9");
				stage = 21;
				break;
			case 21:
				if (buttonId == 1) {
					interpreter.sendDialogues(npc, FacialExpression.NEUTRAL,
							"Correct. At a bar, what does it mean to open a tab?");
					stage = 22;
				} else {
					failVerification();
				}
				break;
			case 22:
				interpreter.sendOptions("Adult verification: question 2 of 3",
						"Reserve a table", "Start a running bill to pay later",
						"Pay the cover charge", "Ask for the bar's tax records");
				stage = 23;
				break;
			case 23:
				if (buttonId == 2) {
					interpreter.sendDialogues(npc, FacialExpression.NEUTRAL,
							"Correct. What does it mean to buy a round?");
					stage = 24;
				} else {
					failVerification();
				}
				break;
			case 24:
				interpreter.sendOptions("Adult verification: question 3 of 3",
						"Buy a drink only for yourself", "Buy a new circular table",
						"Buy drinks for the group", "Challenge everyone to a fight");
				stage = 25;
				break;
			case 25:
				if (buttonId == 3) {
					player.setAttribute(ADULT_VERIFIED, true);
					npc("Verification complete. You can now select the 50x XP rate.",
							"Adult options also include auto-drop commands and teleport unlock bypass.");
				} else {
					failVerification();
					break;
				}
				stage = 50;
				break;
			case 30:
				interpreter.sendOptions("Auto-drop command access",
						NPCDropTables.isAutoDropCommandsEnabled(player)
								? "Disable auto-pickup and auto-bank commands"
								: "Enable auto-pickup and auto-bank commands",
						"Back");
				stage = 31;
				break;
			case 31:
				if (buttonId == 1) {
					boolean enabled = !NPCDropTables.isAutoDropCommandsEnabled(player);
					NPCDropTables.setAutoDropCommandsEnabled(player, enabled);
					npc("Auto-pickup and auto-bank commands are now " + (enabled ? "enabled." : "disabled."),
							enabled ? "Use their commands to enable either feature." : "Both auto-drop features are now disabled.");
					stage = 50;
				} else {
					showMainOptions();
					stage = 1;
				}
				break;
			case 40:
				interpreter.sendOptions("Teleport unlock bypass",
						isTeleportUnlockBypassEnabled()
								? "Disable teleport unlock bypass"
								: "Enable teleport unlock bypass",
						"Back");
				stage = 41;
				break;
			case 41:
				if (buttonId == 1) {
					boolean enabled = !isTeleportUnlockBypassEnabled();
					if (enabled && !isAdultVerified()) {
						startVerification();
						break;
					}
					player.setAttribute(TELEPORT_UNLOCK_BYPASS_ENABLED, enabled);
					npc("Teleport unlock bypass is now " + (enabled ? "enabled." : "disabled."));
					stage = 50;
				} else {
					showMainOptions();
					stage = 1;
				}
				break;
			case 50:
				end();
				break;
		}
		return true;
	}

	private void showMainOptions() {
		if (isAdultVerified()) {
			interpreter.sendOptions("Server Options", "Change XP rate", "Auto-drop command access",
					"Teleport unlock bypass", "Leave");
		} else {
			interpreter.sendOptions("Server Options", "Change XP rate", "Prove I'm not a child", "Leave");
		}
	}

	private void showAutoDropCommandAccess() {
		npc("Auto-pickup and auto-bank command access is currently "
				+ (NPCDropTables.isAutoDropCommandsEnabled(player) ? "enabled." : "disabled."));
	}

	private void showTeleportUnlockBypass() {
		npc("Teleport unlock bypass is currently "
				+ (isTeleportUnlockBypassEnabled() ? "enabled." : "disabled."));
	}

	private boolean isTeleportUnlockBypassEnabled() {
		return player.getAttribute(TELEPORT_UNLOCK_BYPASS_ENABLED, false);
	}

	private void showExperienceOptions() {
		if (isAdultVerified()) {
			interpreter.sendOptions("Change XP rate",
					"1x - The Purist (100-300 hours per skill)",
					"10x - The Grinder (10-30 hours per skill)",
					"25x - The Casual (4-8 hours per skill)",
					"50x - The Casual-With-No-Time (1-3 hours per skill)");
		} else {
			interpreter.sendOptions("Change XP rate",
					"1x - The Purist (100-300 hours per skill)",
					"10x - The Grinder (10-30 hours per skill)",
					"25x - The Casual (4-8 hours per skill)");
		}
	}

	private void setExperienceRate(int buttonId) {
		double rate;
		switch (buttonId) {
			case 1:
				rate = 1.0;
				break;
			case 2:
				rate = 10.0;
				break;
			case 3:
				rate = 25.0;
				break;
			case 4:
				if (!isAdultVerified()) {
					npc("The 50x rate requires adult verification.");
					stage = 50;
					return;
				}
				rate = 50.0;
				break;
			default:
				end();
				return;
		}
		player.getSkills().experienceMultiplier = rate;
		player.setAttribute(HAS_CONFIRMED_HANS_SETUP, true);
		npc("Tada, your XP rate is now " + rate + "x.", "Happy Scaping!");
		stage = 50;
	}

	private void startVerification() {
		long retryAt = player.getAttribute(VERIFICATION_RETRY_AT, 0L);
		long remaining = retryAt - System.currentTimeMillis();
		if (remaining > 0) {
			long seconds = (remaining + 999L) / 1000L;
			npc("Please wait " + seconds + " second" + (seconds == 1 ? "" : "s") + " before trying again.");
			stage = 50;
			return;
		}
		npc("Three questions. Get them all right in one go.",
				"First: which form reports an employee's wages to the IRS?");
		stage = 20;
	}

	private void failVerification() {
		player.setAttribute(VERIFICATION_RETRY_AT, System.currentTimeMillis() + VERIFICATION_RETRY_DELAY);
		npc("Not quite. Take 30 seconds, then you may try again.");
		stage = 50;
	}

	private boolean isAdultVerified() {
		return isAdultVerified(player);
	}

	public static boolean isAdultVerified(Player player) {
		return player.getAttribute(ADULT_VERIFIED, false);
	}

	public static boolean hasConfirmedHansSetup(Player player) {
		return player.getAttribute(HAS_CONFIRMED_HANS_SETUP, false);
	}

	/**
	 * Sends the Hans setup reminder at most once per login session.
	 * Tutorial Island is excluded because its completion flow delivers this reminder explicitly.
	 */
	public static void sendSetupReminderIfNeeded(Player player) {
		if (player.isArtificial()
				|| !player.getAttribute("tutorial:complete", false)
				|| hasConfirmedHansSetup(player)
				|| player.getAttribute(SETUP_REMINDER_SHOWN, false)) {
			return;
		}
		player.setAttribute(SETUP_REMINDER_SHOWN, true);
		player.sendMessage(SETUP_REMINDER);
	}

	@Override
	public int[] getIds() {
		return new int[] { 0 };
	}
}
