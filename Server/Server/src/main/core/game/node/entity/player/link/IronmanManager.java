package core.game.node.entity.player.link;

import core.game.node.entity.player.Player;


import java.nio.ByteBuffer;

/**
 * Compatibility shim for legacy player saves.
 *
 * Ironman mode is no longer available on this server. Keeping this class lets
 * older save files load safely while ensuring every account is a standard one.
 */
public class IronmanManager {

	/**
	 * The player instance.
	 */
	private final Player player;

	/**
	 * The iron man mode.
	 */
	private IronmanMode mode = IronmanMode.NONE;

	/**
	 * Constructs a new {@code IronmanManager} {@code Object}
	 * @param player the player.
	 */
	public IronmanManager(Player player) {
		this.player = player;
	}

	/**
	 * Checks the restriction.
	 * @return {@code True} if so.
	 */
	public boolean checkRestriction() {
		return false;
	}

	/**
	 * Checks the restriction.
	 * @return {@code True} if so.
	 */
	public boolean checkRestriction(IronmanMode mode) {
		return false;
	}

	/**
	 * Checks if the player is an ironman.
	 * @return {@code True} if one.
	 */
	public boolean isIronman() {
		return false;
	}

	/**
	 * Gets the player.
	 * @return the player
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * Gets the mode.
	 * @return the mode
	 */
	public IronmanMode getMode() {
		return IronmanMode.NONE;
	}

	/**
	 * Sets the mode.
	 * @param mode the mode to set.
	 */
	public void setMode(IronmanMode mode) {
		this.mode = IronmanMode.NONE;
	}

}
