package com.velrix.platform.support;

import org.junit.jupiter.api.Assumptions;

import java.net.InetSocketAddress;
import java.net.Socket;

public final class RedisTestSupport {

	private RedisTestSupport() {
	}

	public static void assumeRedisUp() {
		Assumptions.assumeTrue(isRedisUp(), "跳过：本机未启动 Redis（localhost:6379）");
	}

	public static boolean isRedisUp() {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress("localhost", 6379), 500);
			return true;
		} catch (Exception ignored) {
			return false;
		}
	}
}
