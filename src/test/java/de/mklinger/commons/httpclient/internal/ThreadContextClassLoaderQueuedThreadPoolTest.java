package de.mklinger.commons.httpclient.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Test;

public class ThreadContextClassLoaderQueuedThreadPoolTest {
	@Test
	public void test() {
		final URLClassLoader classLoader = new URLClassLoader(new URL[0]);

		final ThreadContextClassLoaderQueuedThreadPool threadPool = new ThreadContextClassLoaderQueuedThreadPool(classLoader);
		final Thread newThread = threadPool.newThread(() -> {});

		assertThat(newThread.getContextClassLoader(), is(classLoader));
	}
}
