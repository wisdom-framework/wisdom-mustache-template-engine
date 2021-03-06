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
import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.wisdom.api.templates.Template;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Extends the default {@link com.github.mustachejava.DefaultMustacheFactory} to customize the cache system and the
 * template lookup.  The template lookup needs to be customized to allow {@literal partial} resolution. By default,
 * it uses the TTCL or the current classloader which does not work in OSGi environment. This resolution is then
 * done by checking the template collected by the {@link org.wisdom.framework.mustache.MustacheTemplateCollector}. This
 * strategy works because the template are compiled on the first use (and partials are resolved at that time).
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
        mustacheCache.remove(template.name());
        if (template.compiled != null) {
            ConcurrentMap<FragmentKey, Mustache> map = new ConcurrentHashMap<>(templateCache);
            for (Map.Entry<FragmentKey, Mustache> entry : map.entrySet()) {
                if (entry.getValue().equals(template.compiled)) {
                    templateCache.remove(entry.getKey());
                }
            }
        }
    }

    /**
     * Gets a {@link java.io.Reader} object on the source of the template having the given name. This method is used
     * to resolved partials.
     *
     * @param name the name of the template
     * @return a reader to read the template's source.
     * @throws com.github.mustachejava.MustacheException if the template cannot be found or read.
     */
    @Override
    public Reader getReader(String name) {
        // On windows the path containing '..' are not stripped from the path, so we ensure they are.
        String simplified = name;
        if (name.contains("..")) {
            simplified = Files.simplifyPath(name);
        }
        // Take into account absolute path
        if (simplified.startsWith("/") && simplified.length() > 1) {
            simplified = simplified.substring(1, simplified.length());
        }
        for (Template t : collector.getTemplates()) {
            MustacheTemplate template = (MustacheTemplate) t;
            if (template.name().equals(simplified)) {
                try {
                    return IOUtils.toBufferedReader(new StringReader(IOUtils.toString(template.getURL())));
                } catch (IOException e) {
                    throw new MustacheException("Cannot read the template " + name, e);
                }
            }
        }

        throw new MustacheException("Template \'" + name + "\' not found");
    }

}
