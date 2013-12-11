/*
 * Copyright (c) 2013, dooApp <contact@dooapp.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Neither the name of dooApp nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.dooapp.fxform.model;

import com.dooapp.fxform.filter.FieldFilter;
import com.dooapp.fxform.filter.FilterException;
import com.dooapp.fxform.filter.IncludeFilter;
import com.dooapp.fxform.reflection.FieldProvider;
import com.dooapp.fxform.reflection.MultipleBeanSource;
import com.dooapp.fxform.reflection.impl.ReflectionFieldProvider;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: Antoine Mischler <antoine@dooapp.com>
 * Date: 03/12/2013
 * Time: 14:51
 */
public class DefaultElementProvider implements ElementProvider {

	private final static Logger logger = Logger.getLogger(DefaultElementProvider.class.getName());

	FieldProvider fieldProvider = new ReflectionFieldProvider();

	ElementFactory elementFactory = new DefaultElementFactory();

	@Override
	public <T> ListProperty<Element> getElements(final ObjectProperty<T> source, final ListProperty<FieldFilter> filters) {
		final ListProperty<Element> elements = new SimpleListProperty<Element>(FXCollections.<Element>observableArrayList());
		filters.addListener(new ListChangeListener<FieldFilter>() {

			@Override
			public void onChanged(Change<? extends FieldFilter> change) {
				elements.clear();
				elements.setAll(createElements(source, filters));
			}
		});
		elements.setAll(createElements(source, filters));
		return elements;
	}

	protected <T> ListProperty<Element> createElements(final ObjectProperty<T> source, List<FieldFilter> filters) {
		ListProperty<Element> elements = new SimpleListProperty<Element>(FXCollections.<Element>observableArrayList());
		List<Field> fields = extractFields(source.get(), getIncludeFilter(filters));
		for (Field field : fields) {
			try {
				final Element element = elementFactory.create(field);
				element.sourceProperty().bind(new ObjectBinding() {

					{
						bind(source);
					}

					@Override
					protected Object computeValue() {
						if (source.get() != null && source.get() instanceof MultipleBeanSource) {
							MultipleBeanSource multipleBeanSource = (MultipleBeanSource) source.get();
							return multipleBeanSource.getSource(element);
						}
						return source.get();
					}
				});
				if (element.getType() != null) {
					elements.add(element);
				}
			} catch (FormException e) {
				logger.log(Level.WARNING, e.getMessage(), e);
			}
		}
		for (FieldFilter filter : filters) {
			try {
				elements.setAll(filter.filter(Collections.unmodifiableList(elements.get())));
			} catch (FilterException e) {
				logger.log(Level.WARNING, e.getMessage(), e);
			}
		}
		return elements;
	}

	private <T> List<Field> extractFields(T source, IncludeFilter includeFilter) {
		if (includeFilter != null) {
			return fieldProvider.getProperties(source, includeFilter.getNames());
		}
		else {
			return fieldProvider.getProperties(source);
		}
	}

	private IncludeFilter getIncludeFilter(List<FieldFilter> filters) {
		for (FieldFilter filter : filters) {
			if (IncludeFilter.class.isAssignableFrom(filter.getClass())) {
				return (IncludeFilter) filter;
			}
		}
		return null;
	}
}
