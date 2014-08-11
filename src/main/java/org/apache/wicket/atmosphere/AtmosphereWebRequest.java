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

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.protocol.http.RequestUtils;
import org.apache.wicket.protocol.http.servlet.MultipartServletWebRequest;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Url;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.time.Time;
import org.apache.wicket.util.upload.FileItemFactory;
import org.apache.wicket.util.upload.FileUploadException;

/**
 * Internal request to signal the processing of an event. This request will be
 * mapped by {@link AtmosphereRequestMapper} to an
 * {@link AtmosphereRequestHandler}. The response will be written to the client
 * of a suspended connection.
 * 
 * @author papegaaij
 */
class AtmosphereWebRequest extends ServletWebRequest
{
	private final ServletWebRequest wrappedRequest;

	private final PageKey pageKey;

	private final Collection<EventSubscription> subscriptions;

	private final AtmosphereEvent event;

	AtmosphereWebRequest(final ServletWebRequest wrappedRequest, final PageKey pageKey,
			final Collection<EventSubscription> subscriptions, final AtmosphereEvent event)
	{
		super(wrappedRequest.getContainerRequest(), wrappedRequest.getFilterPrefix());
		this.wrappedRequest = wrappedRequest;
		this.pageKey = pageKey;
		this.subscriptions = subscriptions;
		this.event = event;
	}

	public PageKey getPageKey()
	{
		return this.pageKey;
	}

	public Collection<EventSubscription> getSubscriptions()
	{
		return this.subscriptions;
	}

	public AtmosphereEvent getEvent()
	{
		return this.event;
	}

	@Override
	public List<Cookie> getCookies()
	{
		return this.wrappedRequest.getCookies();
	}

	@Override
	public List<String> getHeaders(final String name)
	{
		return this.wrappedRequest.getHeaders(name);
	}

	@Override
	public String getHeader(final String name)
	{
		return this.wrappedRequest.getHeader(name);
	}

	@Override
	public Time getDateHeader(final String name)
	{
		return this.wrappedRequest.getDateHeader(name);
	}

	@Override
	public Url getUrl()
	{
		return this.wrappedRequest.getUrl();
	}

	@Override
	public Url getClientUrl()
	{
		return this.wrappedRequest.getClientUrl();
	}

	@Override
	public Locale getLocale()
	{
		try
		{
			return this.wrappedRequest.getLocale();
		}
		catch (final Exception e)
		{
			return Locale.ENGLISH;
		}
	}

	@Override
	public Charset getCharset()
	{
		// called from the super constructor, when wrappedRequest is still null
		if (this.wrappedRequest == null)
		{
			return RequestUtils.getCharset(super.getContainerRequest());
		}
		return this.wrappedRequest.getCharset();
	}

	@Override
	public Cookie getCookie(final String cookieName)
	{
		return this.wrappedRequest.getCookie(cookieName);
	}

	@Override
	public int hashCode()
	{
		return this.wrappedRequest.hashCode();
	}

	@Override
	public Url getOriginalUrl()
	{
		return this.wrappedRequest.getOriginalUrl();
	}

	@Override
	public IRequestParameters getQueryParameters()
	{
		return this.wrappedRequest.getQueryParameters();
	}

	@Override
	public IRequestParameters getRequestParameters()
	{
		return this.wrappedRequest.getRequestParameters();
	}

	@Override
	public boolean equals(final Object obj)
	{
		return this.wrappedRequest.equals(obj);
	}

	@Override
	public String getFilterPrefix()
	{
		return this.wrappedRequest.getFilterPrefix();
	}

	@Override
	public String toString()
	{
		return this.wrappedRequest.toString();
	}

	@Override
	public IRequestParameters getPostParameters()
	{
		return this.wrappedRequest.getPostParameters();
	}

	@Override
	public ServletWebRequest cloneWithUrl(final Url url)
	{
		return this.wrappedRequest.cloneWithUrl(url);
	}

	@Override
	public MultipartServletWebRequest newMultipartWebRequest(final Bytes maxSize,
			final String upload) throws FileUploadException
	{
		return this.wrappedRequest.newMultipartWebRequest(maxSize, upload);
	}

	@Override
	public MultipartServletWebRequest newMultipartWebRequest(final Bytes maxSize,
			final String upload, final FileItemFactory factory) throws FileUploadException
	{
		return this.wrappedRequest.newMultipartWebRequest(maxSize, upload, factory);
	}

	@Override
	public String getPrefixToContextPath()
	{
		return this.wrappedRequest.getPrefixToContextPath();
	}

	@Override
	public HttpServletRequest getContainerRequest()
	{
		return this.wrappedRequest.getContainerRequest();
	}

	@Override
	public String getContextPath()
	{
		return this.wrappedRequest.getContextPath();
	}

	@Override
	public String getFilterPath()
	{
		return this.wrappedRequest.getFilterPath();
	}

	@Override
	public boolean shouldPreserveClientUrl()
	{
		return this.wrappedRequest.shouldPreserveClientUrl();
	}

	@Override
	public boolean isAjax()
	{
		return true;
	}
}
