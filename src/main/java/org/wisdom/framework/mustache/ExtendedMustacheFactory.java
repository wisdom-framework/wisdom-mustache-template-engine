/*
 * #%L
 * Wisdom-Framework
 * %%
 * Copyright (C) 2013 - 2014 Wisdom Framework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wisdom.framework.mustache;

import com.github.mustachejava.DeferringMustacheFactory;
import com.github.mustachejava.FragmentKey;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import org.apache.commons.io.IOUtils;
import org.wisdom.api.templates.Template;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Extends the default {@link com.github.mustachejava.DefaultMustacheFactory} to customize the cache system.
 */
public class ExtendedMustacheFactory extends DeferringMustacheFactory {

    /**
     * The collector used to resolve partials.
     */
    MustacheTemplateCollector collector;

    /**
     * Creates an instance of {@link org.wisdom.framework.mustache.ExtendedMustacheFactory}.
     *
     * @param collector the collector
     */
    public ExtendedMustacheFactory(MustacheTemplateCollector collector) {
        this.collector = collector;
    }

    /**
     * Removes the template from the cache.
     *
     * @param template the template
     */
    public void clear(MustacheTemplate template) {
        mustacheCache.invalidate(template.name());
        if (template.compiled != null) {
            ConcurrentMap<FragmentKey, Mustache> map = new ConcurrentHashMap<>(templateCache.asMap());
            for (Map.Entry<FragmentKey, Mustache> entry : map.entrySet()) {
                if (entry.getValue().equals(template.compiled)) {
                    templateCache.invalidate(entry.getKey());
                }
            }
        }
    }

    /**
     * Gets a {@link java.io.Reader} object on the source of the template having the given name. This mehtod is used
     * to resolved partials.
     *
     * @param name the name of the template
     * @return a reader to read the template's source.
     * @throws com.github.mustachejava.MustacheException if the template cannot be found or read.
     */
    @Override
    public Reader getReader(String name) {
        for (Template t : collector.getTemplates()) {
            MustacheTemplate template = (MustacheTemplate) t;
            if (template.name().equals(name)) {
                try {
                    return IOUtils.toBufferedReader(new StringReader(IOUtils.toString(template.getURL())));
                } catch (IOException e) {
                    throw new MustacheException("Cannot read the template " + name);
                }
            }
        }

        throw new MustacheException("Template \'" + name + "\' not found");
    }

}
