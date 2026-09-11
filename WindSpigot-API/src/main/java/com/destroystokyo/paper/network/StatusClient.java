package com.destroystokyo.paper.network;

import java.net.InetSocketAddress;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;

/**
 * Represents a client requesting the current status from the server (e.g.
 * from the server list).
 *
 * @see PaperServerListPingEvent
 */
public interface StatusClient {

	/**
	 * Gets the address of the client pinging the server.
	 *
	 * @return the address of the client
	 */
	InetSocketAddress getAddress();

	/**
	 * Gets the protocol version reported by the client pinging the server.
	 *
	 * @return the protocol version of the client
	 */
	int getProtocolVersion();

	/**
	 * Gets the hostname (and port) the client used to reach this server, i.e.
	 * the address used in the client's server list entry, or the virtual host
	 * a proxy forwarded on behalf of the client.
	 * <p>
	 * This address is unresolved and should only be used for informational or
	 * routing purposes.
	 *
	 * @return the virtual host used by the client, or {@code null} if unknown
	 */
	InetSocketAddress getVirtualHost();
}
