package de.mklinger.commons.httpclient.internal;

import java.util.function.Supplier;

import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class ThreadContextClassLoaderQueuedThreadPool extends QueuedThreadPool {
	private final ClassLoader classLoader;

	public ThreadContextClassLoaderQueuedThreadPool(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public Thread newThread(Runnable runnable) {
		return getWithContextClassLoader(classLoader, () -> super.newThread(runnable));
	}

	private <T> T getWithContextClassLoader(ClassLoader classLoader, Supplier<T> s) {
		final ClassLoader old = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return s.get();
		} finally {
			Thread.currentThread().setContextClassLoader(old);
		}
	}
}