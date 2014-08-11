/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wicket.atmosphere;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.ThreadContext;
import org.apache.wicket.page.IPageManagerContext;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WicketFilter;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.tester.WicketTester;
import org.atmosphere.cpr.AsyncSupport;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceFactory;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.DefaultBroadcaster;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class EventBusMock extends EventBus
{
	protected static final Logger log = LoggerFactory.getLogger(EventBusMock.class);

	private final List<Object> events = new ArrayList<Object>();
	static AtmosphereFramework framework;
	private Broadcaster broadcaster;
	private WebApplication application;
	public Map<String, PageKey> trackedPages = Maps.newHashMap();
	private final Multimap<PageKey, EventSubscription> subscriptions = HashMultimap.create();
	private final List<ResourceRegistrationListener> registrationListeners = new CopyOnWriteArrayList<ResourceRegistrationListener>();
	private AtmosphereResource resource;
	public IPageManagerContext pageManagerContext;
	private MapperContextMock mapperContext;
	private static final Logger LOGGER = LoggerFactory.getLogger(EventBusMock.class);
	final static BroadcasterFactoryMock broadcasterFactory;
	private Page page;

	private WicketTester tester;

	static
	{
		EventBusMock.framework = new AtmosphereFramework()
		{
			@Override
			public boolean isShareExecutorServices()
			{
				return true;
			}
		};

		final AtmosphereConfig config = EventBusMock.framework.getAtmosphereConfig();
		System.out.println("=== " + config);
		broadcasterFactory = new BroadcasterFactoryMock(DefaultBroadcaster.class, "NEVER", config);
		EventBusMock.framework.setBroadcasterFactory(EventBusMock.broadcasterFactory);
		EventBusMock.framework.setAsyncSupport(Mockito.mock(AsyncSupport.class));
		try
		{
			EventBusMock.framework.init(new ServletConfig()
			{
				@Override
				public String getServletName()
				{
					return "void";
				}

				@Override
				public ServletContext getServletContext()
				{
					return Mockito.mock(ServletContext.class);
				}

				@Override
				public String getInitParameter(final String name)
				{
					return null;
				}

				@Override
				public Enumeration<String> getInitParameterNames()
				{
					return null;
				}
			});
		}
		catch (final ServletException e)
		{
			EventBusMock.LOGGER.error("error in static block", e);
		}
		assert null != EventBusMock.framework.getBroadcasterFactory();
	}

	public EventBusMock(final WebApplication _application, final MapperContextMock mapperContext)
	{
		this(_application, EventBusMock.broadcasterFactory.get());

		this.broadcaster = EventBusMock.broadcasterFactory.get();
		assert null != this.broadcaster;

		this.application = _application;
		this.mapperContext = mapperContext;

		this.resource = AtmosphereResourceFactory.getDefault().create(
				EventBusMock.framework.getAtmosphereConfig(),
				EventBusMock.broadcasterFactory.get(),
				AtmosphereResponse.newInstance().request(AtmosphereRequest.newInstance()),
				Mockito.mock(AsyncSupport.class), Mockito.mock(AtmosphereHandler.class));
	}

	/**
	 * Creates and registers an {@code EventBusMock} for the given application
	 * and broadcaster
	 *
	 * @param application
	 * @param broadcaster
	 */
	public EventBusMock(final WebApplication application, final Broadcaster broadcaster)
	{
		super(application, broadcaster);
	}

	@Override
	public void post(final Object event, final String resourceUuid)
	{
		this.events.add(event);
		assert null != this.resource;
		this.post(event, this.resource);
	}

	@Override
	public void post(final Object event, final AtmosphereResource _resource)
	{
		final ThreadContext oldContext = ThreadContext.get(false);
		try
		{
			this.postToSingleResource(event, _resource);
		}
		catch (final Exception e)
		{
			EventBusMock.LOGGER.error("error in post()", e);
		}
		finally
		{
			ThreadContext.restore(oldContext);
		}
	}

	private void postToSingleResource(final Object payload, final AtmosphereResource _resource)
	{
		final AtmosphereEvent event = new AtmosphereEvent(payload, _resource);
		ThreadContext.detach();
		ThreadContext.setApplication(this.application);
		PageKey key;
		Collection<EventSubscription> subscriptionsForPage;
		synchronized (this)
		{
			key = this.trackedPages.get(_resource.uuid());
			// subscriptionsForPage = Collections2.filter(
			// Collections.unmodifiableCollection(this.subscriptions.get(key)),
			// new EventFilterMock(event));
			subscriptionsForPage = this.subscriptions.get(key);
		}
		if (key == null)
		{
			this.broadcaster.removeAtmosphereResource(_resource);
		}
		else if (!subscriptionsForPage.isEmpty())
		{
			this.post(_resource, key, subscriptionsForPage, event);
		}
	}

	private void post(final AtmosphereResource _resource, final PageKey pageKey,
			final Collection<EventSubscription> subscriptionsForPage, final AtmosphereEvent event)
	{
		String filterPath = WebApplication.get().getWicketFilter().getFilterConfig()
				.getInitParameter(WicketFilter.FILTER_MAPPING_PARAM);
		filterPath = filterPath.substring(1, filterPath.length() - 1);
		final HttpServletRequest httpRequest = new HttpServletRequestWrapper(_resource.getRequest())
		{
			@Override
			public String getContextPath()
			{
				final String ret = super.getContextPath();
				return ret == null ? "" : ret;
			}
		};
		final AtmosphereWebRequest req = new AtmosphereWebRequest(
				(ServletWebRequest)this.application.newWebRequest(httpRequest, filterPath),
				pageKey, subscriptionsForPage, event);
		final Response response = new AtmosphereWebResponse(_resource.getResponse());
		final RequestCycle requestCycle = this.application.createRequestCycle(req, response);
		if (requestCycle.processRequestAndDetach())
		{
			System.out.println("~~~ " + response.toString());
			this.broadcaster.broadcast(response.toString(), _resource);

			final Response webResponse = this.tester.getRequestCycle().getResponse();
			webResponse.reset();
			webResponse.write(response.toString());
		}
	}

	@Override
	public void addRegistrationListener(final ResourceRegistrationListener listener)
	{
		this.registrationListeners.add(listener);
	}

	/**
	 * Registers a page for the given tracking-id in the {@code EventBus}.
	 *
	 * @param trackingId
	 * @param page
	 */
	@Override
	public synchronized void registerPage(final String trackingId, final Page _page)
	{
		final PageKey oldPage = this.trackedPages.remove(trackingId);
		this.page = _page;
		final PageKey pageKey = new PageKey(this.page.getPageId(), Session.get().getId());
		if ((oldPage != null) && !oldPage.equals(pageKey))
		{
			this.subscriptions.removeAll(oldPage);
			this.fireUnregistration(trackingId);
		}
		this.trackedPages.put(trackingId, pageKey);
		this.fireRegistration(trackingId, this.page);

		EventBusMock.LOGGER.info("registered page {} for session {}", pageKey.getPageId(),
				pageKey.getSessionId());

		this.application.getPageManagerProvider().get(this.pageManagerContext).touchPage(this.page);
		this.mapperContext.registerPage(pageKey.getPageId(), this.page);
		Session.get().getPageManager().touchPage(this.page);
	}

	/**
	 * Registers an {@link EventSubscription} for the given page.
	 *
	 * @param page
	 * @param subscription
	 */
	@Override
	public synchronized void register(final Page page, final EventSubscription subscription)
	{
		if (EventBusMock.log.isDebugEnabled())
		{
			EventBusMock.log.debug(
					"registering {} for page {} for session {}: {}{}",
					new Object[] {
							subscription.getBehaviorIndex() == null ? "component" : "behavior",
							page.getPageId(),
							Session.get().getId(),
							subscription.getComponentPath(),
							subscription.getBehaviorIndex() == null ? "" : ":"
									+ subscription.getBehaviorIndex() });
		}
		final PageKey pageKey = new PageKey(page.getPageId(), Session.get().getId());
		if (!this.subscriptions.containsEntry(pageKey, subscription))
		{
			this.subscriptions.put(pageKey, subscription);
		}
	}

	/**
	 * Removes a previously added {@link ResourceRegistrationListener}.
	 *
	 * @param listener
	 */
	@Override
	public void removeRegistrationListener(final ResourceRegistrationListener listener)
	{
		this.registrationListeners.add(listener);
	}

	public void fireRegistration(final String uuid, final Page page)
	{
		for (final ResourceRegistrationListener curListener : this.registrationListeners)
		{
			curListener.resourceRegistered(uuid, page);
		}
	}

	public void fireUnregistration(final String uuid)
	{
		for (final ResourceRegistrationListener curListener : this.registrationListeners)
		{
			curListener.resourceUnregistered(uuid);
		}
	}

	public List<Object> getEvents()
	{
		return this.events;
	}

	public AtmosphereResource getResource()
	{
		return this.resource;
	}

	public void setTester(final WicketTester _tester)
	{
		this.tester = _tester;
	}

}
