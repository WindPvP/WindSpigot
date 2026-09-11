package com.destroystokyo.paper.event.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;

import com.destroystokyo.paper.network.StatusClient;
import com.mojang.authlib.GameProfile;

/**
 * Called when a server list ping is coming in. Unlike the legacy
 * {@link ServerListPingEvent}, this event exposes the {@link StatusClient}
 * performing the ping, allows the reported server version/protocol to be
 * overridden, allows the player count to be hidden entirely, and exposes the
 * player sample (the list shown when hovering the player count) as a mutable
 * list rather than requiring it to be backed by real online players.
 * <p>
 * Since this event extends {@link ServerListPingEvent}, plugins that still
 * listen for the legacy event will keep working unchanged - both events share
 * the same {@link org.bukkit.event.HandlerList}.
 */
public class PaperServerListPingEvent extends ServerListPingEvent implements Cancellable {

	private final StatusClient client;

	private int numPlayers;
	private boolean hidePlayers;
	private final List<GameProfile> playerSample = new ArrayList<GameProfile>();

	private String version;
	private int protocolVersion;

	private CachedServerIcon favicon;

	private boolean cancelled;

	private boolean originalPlayerCount = true;
	private Object[] players;

	public PaperServerListPingEvent(StatusClient client, String motd, int numPlayers, int maxPlayers, String version,
			int protocolVersion, CachedServerIcon favicon) {
		super(client.getAddress().getAddress(), motd, numPlayers, maxPlayers);
		this.client = client;
		this.numPlayers = numPlayers;
		this.version = version;
		this.protocolVersion = protocolVersion;
		setServerIcon(favicon);
	}

	/**
	 * Returns the {@link StatusClient} pinging the server.
	 *
	 * @return the client
	 */
	public StatusClient getClient() {
		return client;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Returns {@code -1} if players are hidden using {@link #shouldHidePlayers()}.
	 */
	@Override
	public int getNumPlayers() {
		if (this.hidePlayers) {
			return -1;
		}

		return this.numPlayers;
	}

	/**
	 * Sets the number of players displayed in the server list.
	 * <p>
	 * Note that this won't have any effect if {@link #shouldHidePlayers()} is
	 * enabled.
	 *
	 * @param numPlayers the number of online players
	 */
	public void setNumPlayers(int numPlayers) {
		if (this.numPlayers != numPlayers) {
			this.numPlayers = numPlayers;
			this.originalPlayerCount = false;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Returns {@code -1} if players are hidden using {@link #shouldHidePlayers()}.
	 */
	@Override
	public int getMaxPlayers() {
		if (this.hidePlayers) {
			return -1;
		}

		return super.getMaxPlayers();
	}

	/**
	 * Returns whether all player related information is hidden in the server
	 * list. This will cause {@link #getNumPlayers()}, {@link #getMaxPlayers()}
	 * and {@link #getPlayerSample()} to be skipped in the response.
	 * <p>
	 * The Vanilla Minecraft client will display the player count as {@code ???}
	 * when this option is enabled.
	 *
	 * @return {@code true} if the player count is hidden
	 */
	public boolean shouldHidePlayers() {
		return this.hidePlayers;
	}

	/**
	 * Sets whether all player related information is hidden in the server list.
	 * This will cause {@link #getNumPlayers()}, {@link #getMaxPlayers()} and
	 * {@link #getPlayerSample()} to be skipped in the response.
	 * <p>
	 * The Vanilla Minecraft client will display the player count as {@code ???}
	 * when this option is enabled.
	 *
	 * @param hidePlayers {@code true} if the player count should be hidden
	 */
	public void setHidePlayers(boolean hidePlayers) {
		this.hidePlayers = hidePlayers;
	}

	/**
	 * Returns a mutable list of {@link GameProfile} that will be displayed as
	 * online players on the client.
	 * <p>
	 * The Vanilla Minecraft client will display them when hovering the player
	 * count with the mouse.
	 *
	 * @return the mutable player sample list
	 */
	public List<GameProfile> getPlayerSample() {
		return this.playerSample;
	}

	/**
	 * Returns the version that will be sent as the server version to the
	 * client.
	 *
	 * @return the server version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Sets the version that will be sent as the server version to the client.
	 *
	 * @param version the server version
	 */
	public void setVersion(String version) {
		this.version = Objects.requireNonNull(version, "version must not be null");
	}

	/**
	 * Returns the protocol version that will be sent as the protocol version
	 * of the server to the client.
	 *
	 * @return the protocol version of the server
	 */
	public int getProtocolVersion() {
		return this.protocolVersion;
	}

	/**
	 * Sets the protocol version that will be sent as the protocol version of
	 * the server to the client.
	 *
	 * @param protocolVersion the protocol version of the server
	 */
	public void setProtocolVersion(int protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	/**
	 * Gets the server icon sent to the client.
	 *
	 * @return the icon to send to the client, or {@code null} for none
	 */
	public CachedServerIcon getServerIcon() {
		return this.favicon;
	}

	/**
	 * Sets the server icon sent to the client.
	 *
	 * @param icon the icon to send to the client, or {@code null} for none
	 */
	@Override
	public void setServerIcon(CachedServerIcon icon) {
		if (icon != null && icon.isEmpty()) {
			icon = null;
		}

		this.favicon = icon;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Cancelling this event will cause the connection to be closed immediately,
	 * without sending a response to the client.
	 */
	@Override
	public boolean isCancelled() {
		return this.cancelled;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Cancelling this event will cause the connection to be closed immediately,
	 * without sending a response to the client.
	 */
	@Override
	public void setCancelled(boolean cancel) {
		this.cancelled = cancel;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Note:</b> For compatibility reasons, this method will return all
	 * online players, not just the ones referenced in {@link #getPlayerSample()}.
	 * Removing a player will:
	 * <ul>
	 * <li>Decrement the online player count (if and only if) the player count
	 * wasn't changed by another plugin before.</li>
	 * <li>Remove all entries from {@link #getPlayerSample()} that refer to the
	 * removed player (based on their {@link UUID}).</li>
	 * </ul>
	 */
	@Override
	public Iterator<Player> iterator() {
		if (this.players == null) {
			this.players = getOnlinePlayers();
		}

		return new PlayerIterator();
	}

	protected Object[] getOnlinePlayers() {
		return Bukkit.getOnlinePlayers().toArray();
	}

	protected Player getBukkitPlayer(Object player) {
		return (Player) player;
	}

	private final class PlayerIterator implements Iterator<Player> {

		private int next, current;
		private Player player;

		@Override
		public boolean hasNext() {
			for (; this.next < players.length; this.next++) {
				if (players[this.next] != null) {
					return true;
				}
			}

			return false;
		}

		@Override
		public Player next() {
			if (!hasNext()) {
				this.player = null;
				throw new NoSuchElementException();
			}

			this.current = this.next++;
			return this.player = getBukkitPlayer(players[this.current]);
		}

		@Override
		public void remove() {
			if (this.player == null) {
				throw new IllegalStateException();
			}

			final UUID uniqueId = this.player.getUniqueId();
			this.player = null;

			// Remove player from iterator
			players[this.current] = null;

			// Remove player from sample
			Iterator<GameProfile> sampleIterator = getPlayerSample().iterator();
			while (sampleIterator.hasNext()) {
				if (uniqueId.equals(sampleIterator.next().getId())) {
					sampleIterator.remove();
				}
			}

			// Decrement player count
			if (originalPlayerCount) {
				numPlayers--;
			}
		}
	}
}
