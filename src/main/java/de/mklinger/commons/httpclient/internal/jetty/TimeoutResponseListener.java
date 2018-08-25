package de.mklinger.commons.httpclient.internal.jetty;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response.Listener;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.io.CyclicTimeout;
import org.eclipse.jetty.util.thread.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class TimeoutResponseListener extends ResponseListenerWrapper {

	// Inspired by org.eclipse.jetty.client.TimeoutCompleteListener

	private static final Logger LOG = LoggerFactory.getLogger(TimeoutResponseListener.class);

	private final CyclicTimeout cyclicTimeout;

	public TimeoutResponseListener(final Listener delegate, final Request request, final long timeout, final TimeUnit timeUnit, final Scheduler scheduler) {
		super(delegate);
		cyclicTimeout = new CyclicTimeout(scheduler) {
			@Override
			public void onTimeoutExpired() {
				final long millis = timeUnit.toMillis(timeout);
				LOG.info("Timeout {} ms elapsed for {}", millis, request);
				request.abort(new TimeoutException("Total timeout " + millis + " ms elapsed"));
			}
		};
		cyclicTimeout.schedule(timeout, timeUnit);
	}

	@Override
	public void onComplete(final Result result) {
		try {
			cyclicTimeout.cancel();
		} catch (final Throwable e) {
			LOG.error("Error canelling timeout", e);
		}
		super.onComplete(result);
	}
}
