package net.minecraft.server;

import java.net.InetSocketAddress;

// CraftBukkit start
import com.mojang.authlib.GameProfile;

// CraftBukkit end

public class PacketStatusListener implements PacketStatusInListener {

	private static final IChatBaseComponent a = new ChatComponentText("Status request has been handled.");
	private final MinecraftServer minecraftServer;
	private final NetworkManager networkManager;
	// Paper start - carry handshake info through for StatusClient/PaperServerListPingEvent
	private final String hostname;
	private final int port;
	private final int protocolVersion;
	// Paper end
	private boolean d;

	public PacketStatusListener(MinecraftServer minecraftserver, NetworkManager networkmanager, String hostname,
			int port, int protocolVersion) {
		this.minecraftServer = minecraftserver;
		this.networkManager = networkmanager;
		// Paper start
		this.hostname = hostname;
		this.port = port;
		this.protocolVersion = protocolVersion;
		// Paper end
	}

	@Override
	public void a(IChatBaseComponent ichatbasecomponent) {
	}

	@Override
	public void a(PacketStatusInStart packetstatusinstart) {
		if (this.d) {
			this.networkManager.close(PacketStatusListener.a);
			return;
		}
		this.d = true;

		// CraftBukkit start - fire ping event
		final Object[] players = minecraftServer.getPlayerList().players.toArray();
		java.util.List<GameProfile> profiles = new java.util.ArrayList<GameProfile>(players.length);
		for (Object player : players) {
			if (player != null) {
				profiles.add(((EntityPlayer) player).getProfile());
			}
		}

		// Spigot Start - the default hover-tooltip sample is capped/shuffled, but the
		// real online count (used for the event and the "N/max players" display) is not
		java.util.List<GameProfile> sample = new java.util.ArrayList<GameProfile>(profiles);
		if (!sample.isEmpty()) {
			// This sucks, its inefficient but we have no simple way of doing it differently
			java.util.Collections.shuffle(sample);
			// Cap the sample to n (or less) displayed players, ie: Vanilla behaviour
			sample = sample.subList(0, Math.min(sample.size(), org.spigotmc.SpigotConfig.playerSample));
		}
		// Spigot End

		// Paper start - fire PaperServerListPingEvent (also satisfies legacy ServerListPingEvent listeners)
		final InetSocketAddress rawAddress = (InetSocketAddress) this.networkManager.getSocketAddress();
		final InetSocketAddress virtualHost = getVirtualHost(this.hostname, this.port);
		final int clientProtocolVersion = this.protocolVersion;

		com.destroystokyo.paper.network.StatusClient client = new com.destroystokyo.paper.network.StatusClient() {
			@Override
			public InetSocketAddress getAddress() {
				return rawAddress;
			}

			@Override
			public int getProtocolVersion() {
				return clientProtocolVersion;
			}

			@Override
			public InetSocketAddress getVirtualHost() {
				return virtualHost;
			}
		};

		com.destroystokyo.paper.event.server.PaperServerListPingEvent event = new com.destroystokyo.paper.event.server.PaperServerListPingEvent(
				client, minecraftServer.getMotd(), profiles.size(), minecraftServer.getPlayerList().getMaxPlayers(),
				minecraftServer.getServerModName() + " " + minecraftServer.getVersion(), 47, // TODO: Update when protocol changes
				minecraftServer.server.getServerIcon());
		event.getPlayerSample().addAll(sample);

		this.minecraftServer.server.getPluginManager().callEvent(event);

		if (event.isCancelled()) {
			this.networkManager.close(PacketStatusListener.a);
			return;
		}

		ServerPing ping = new ServerPing();
		if (event.getServerIcon() != null) {
			ping.setFavicon(event.getServerIcon().getData());
		}
		ping.setMOTD(new ChatComponentText(event.getMotd()));
		if (!event.shouldHidePlayers()) {
			ServerPing.ServerPingPlayerSample playerSample = new ServerPing.ServerPingPlayerSample(
					event.getMaxPlayers(), event.getNumPlayers());
			java.util.List<GameProfile> finalSample = event.getPlayerSample();
			playerSample.a(finalSample.toArray(new GameProfile[finalSample.size()]));
			ping.setPlayerSample(playerSample);
		}
		ping.setServerInfo(new ServerPing.ServerData(event.getVersion(), event.getProtocolVersion()));

		this.networkManager.handle(new PacketStatusOutServerInfo(ping));
		// Paper end / CraftBukkit end
	}

	// Paper start
	/**
	 * Resolves the virtual host the client used for this ping. When BungeeCord IP
	 * forwarding is enabled, the real hostname is extracted from the forwarded
	 * handshake payload; the returned address is intentionally unresolved.
	 */
	private static InetSocketAddress getVirtualHost(String hostname, int port) {
		if (hostname == null || hostname.isEmpty()) {
			return null;
		}

		String resolvedHost = hostname;
		if (org.spigotmc.SpigotConfig.bungee) {
			String[] split = hostname.split("\00");
			if (split.length == 3 || split.length == 4) {
				resolvedHost = split[0];
			}
		}

		return InetSocketAddress.createUnresolved(resolvedHost, port);
	}
	// Paper end

	@Override
	public void a(PacketStatusInPing packetstatusinping) {
		this.networkManager.handle(new PacketStatusOutPong(packetstatusinping.a()));
		this.networkManager.close(PacketStatusListener.a);
	}
}
