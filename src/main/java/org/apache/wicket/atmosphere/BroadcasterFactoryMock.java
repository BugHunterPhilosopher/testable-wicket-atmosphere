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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.DefaultBroadcasterFactory;
import org.atmosphere.util.SimpleBroadcaster;

public class BroadcasterFactoryMock extends DefaultBroadcasterFactory
{
	protected BroadcasterFactoryMock(final Class<? extends Broadcaster> clazz,
			final String broadcasterLifeCyclePolicy, final AtmosphereConfig c)
	{
		super(clazz, broadcasterLifeCyclePolicy, c);
		BroadcasterFactory.config = c;
	}

	@Override
	public synchronized void destroy()
	{
	}

	@Override
	public boolean add(final Broadcaster b, final Object id)
	{
		return false;
	}

	@Override
	public boolean remove(final Broadcaster b, final Object id)
	{
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Broadcaster> T lookup(final Class<T> c, final Object id,
			final boolean createIfNull)
	{
		final T sb = (T)new SimpleBroadcaster();
		// sb.setID("/*");
		final AtmosphereConfig conf = new AtmosphereConfig(EventBusMock.framework);
		assert null != conf.framework();
		assert true == conf.framework().isShareExecutorServices();

		try
		{
			sb.initialize("/*", new URI("/"), BroadcasterFactory.config);
		}
		catch (final URISyntaxException e)
		{
			e.printStackTrace();
		}
		return sb;
	}

	@Override
	public void removeAllAtmosphereResource(final AtmosphereResource r)
	{
	}

	@Override
	public boolean remove(final Object id)
	{
		return false;
	}

	@Override
	public Collection<Broadcaster> lookupAll()
	{
		final SimpleBroadcaster sb = new SimpleBroadcaster();
		final Collection<Broadcaster> all = new ArrayList<Broadcaster>();
		all.add(sb);
		return all;
	}

}
