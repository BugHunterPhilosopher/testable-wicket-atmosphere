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

import java.util.Collection;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;

/**
 * Handles pseudo requests triggered by an event. An {@link AjaxRequestTarget}
 * is scheduled and the subscribed methods are invoked.
 * 
 * @author papegaaij
 */
public class AtmosphereRequestHandler implements IRequestHandler
{
	private final PageKey pageKey;

	private final AtmosphereEvent event;

	private final Collection<EventSubscription> subscriptions;

	private final EventSubscriptionInvoker eventSubscriptionInvoker;

	private boolean ajaxRequestScheduled = false;

	/**
	 * Construct.
	 * 
	 * @param pageKey
	 * @param subscriptions
	 * @param event
	 * @param eventSubscriptionInvoker
	 */
	public AtmosphereRequestHandler(final PageKey pageKey,
			final Collection<EventSubscription> subscriptions, final AtmosphereEvent event,
			final EventSubscriptionInvoker eventSubscriptionInvoker)
	{
		this.pageKey = pageKey;
		this.subscriptions = subscriptions;
		this.event = event;
		this.eventSubscriptionInvoker = eventSubscriptionInvoker;
	}

	@Override
	public void respond(final IRequestCycle requestCycle)
	{
		System.out.println("pageKey: " + this.pageKey.getPageId());

		final Page page = (Page)Application.get().getMapperContext()
				.getPageInstance(this.pageKey.getPageId());

		final AjaxRequestTarget target = WebApplication.get().newAjaxRequestTarget(page);
		this.executeHandlers(target, page);
	}

	private void executeHandlers(final AjaxRequestTarget target, final Page page)
	{
		for (final EventSubscription curSubscription : this.subscriptions)
		{
			if (curSubscription.getContextAwareFilter().apply(this.event))
			{
				final Component component = page.get(curSubscription.getComponentPath());
				if (curSubscription.getBehaviorIndex() == null)
				{
					this.invokeMethod(target, curSubscription, component);
				}
				else
				{
					this.invokeMethod(target, curSubscription,
							component.getBehaviorById(curSubscription.getBehaviorIndex()));
				}
			}
		}
	}

	private void invokeMethod(final AjaxRequestTarget target, final EventSubscription subscription,
			final Object base)
	{
		final AjaxRequestInitializer initializer = new AjaxRequestInitializer()
		{
			@Override
			public void initialize()
			{
				if (!AtmosphereRequestHandler.this.ajaxRequestScheduled)
				{
					RequestCycle.get().scheduleRequestHandlerAfterCurrent(target);
					AtmosphereRequestHandler.this.ajaxRequestScheduled = true;
				}
			}
		};
		this.eventSubscriptionInvoker.invoke(target, subscription, base, this.event, initializer);
	}

	@Override
	public void detach(final IRequestCycle requestCycle)
	{
	}
}
