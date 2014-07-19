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

import org.apache.felix.ipojo.annotations.Invalidate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wisdom.api.annotations.Service;
import org.wisdom.api.templates.Template;
import org.wisdom.api.templates.TemplateEngine;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The main entry point of the Mustasche Template engine.
 * It orchestrates the order components, and manages the registration of the {@link Template} services.
 * <p>
 * Mustache supports "mst.*" templates such as {@literal foo.mst} or {@literal foo.mst.plain}. The extension is
 * used to determine the mime-type of the template (are supported: xml, json, html and plain).
 */
@Service({TemplateEngine.class, MustacheTemplateCollector.class})
public class MustacheTemplateCollector implements TemplateEngine {

    /**
     * The extension of the file managed by this template engine.
     */
    public static final String EXTENSION = "mst.*";
    /**
     * The bundle context.
     */
    private final BundleContext context;

    /**
     * The default Mustasche factory.
     */
    private final ExtendedMustacheFactory msf;

    private static final Logger LOGGER = LoggerFactory.getLogger(MustacheTemplateCollector.class.getName());

    /**
     * The registration map.
     */
    private Map<MustacheTemplate, ServiceRegistration<Template>> registrations = new ConcurrentHashMap<>();

    /**
     * Creates a new instance of {@link org.wisdom.framework.mustache.MustacheTemplateCollector}.
     *
     * @param context the bundle context.
     */
    public MustacheTemplateCollector(BundleContext context) {
        this.context = context;
        this.msf = new ExtendedMustacheFactory(this);
    }

    /**
     * Gets the current list of templates.
     *
     * @return the current list of template
     */
    @Override
    public Collection<Template> getTemplates() {
        return new ArrayList<Template>(registrations.keySet());
    }


    /**
     * The name of the template engine.
     *
     * @return "mustache"
     */
    @Override
    public String name() {
        return "mustache";
    }

    /**
     * Mustache supports "mst.*" templates such as {@literal foo.mst} or {@literal foo.mst.plain}. The extension is
     * used to determine the mime-type of the template (are supported: xml, json, html and plain).
     *
     * @return "mst.x"
     */
    @Override
    public String extension() {
        return EXTENSION;
    }

    /**
     * Stops the collector. This methods clear all registered {@link org.wisdom.api.templates.Template} services.
     */
    @Invalidate
    public void stop() {
        for (ServiceRegistration<Template> reg : registrations.values()) {
            try {
                reg.unregister();
            } catch (Exception e) { //NOSONAR
                // Ignore it.
            }
        }
        registrations.clear();
    }

    /**
     * Updates the template object using the given file as backend.
     *
     * @param templateFile the template file
     */
    public void updatedTemplate(File templateFile) {
        MustacheTemplate template = getTemplateByFile(templateFile);
        if (template != null) {
            LOGGER.info("Mustache template updated for {} ({})", templateFile.getAbsoluteFile(), template.fullName());
            updatedTemplate(template);
        } else {
            try {
                addTemplate(templateFile.toURI().toURL());
            } catch (MalformedURLException e) { //NOSONAR
                // Ignored.
            }
        }
    }

    /**
     * Gets the template object using the given file as backend.
     *
     * @param templateFile the file
     * @return the template object, {@literal null} if not found
     */
    private MustacheTemplate getTemplateByFile(File templateFile) {
        try {
            return getTemplateByURL(templateFile.toURI().toURL());
        } catch (MalformedURLException e) {  //NOSONAR
            // Ignored.
        }
        return null;
    }

    /**
     * Gets the template object using the given url as backend.
     *
     * @param url the url
     * @return the template object, {@literal null} if not found
     */
    private MustacheTemplate getTemplateByURL(URL url) {
        Collection<MustacheTemplate> list = registrations.keySet();
        for (MustacheTemplate template : list) {
            if (template.getURL().sameFile(url)) {
                return template;
            }
        }
        return null;
    }

    /**
     * Deletes the template using the given file as backend.
     *
     * @param templateFile the file
     */
    public void deleteTemplate(File templateFile) {
        MustacheTemplate template = getTemplateByFile(templateFile);
        if (template != null) {
            deleteTemplate(template);
        }
    }

    /**
     * Adds a template form the given url.
     *
     * @param templateURL the url
     * @return the added template. IF the given url is already used by another template, return this other template.
     */
    public MustacheTemplate addTemplate(URL templateURL) {
        MustacheTemplate template = getTemplateByURL(templateURL);
        if (template != null) {
            // Already existing.
            return template;
        }
        template = new MustacheTemplate(msf, templateURL);
        ServiceRegistration<Template> reg = context.registerService(Template.class, template,
                template.getServiceProperties());
        registrations.put(template, reg);
        LOGGER.info("Mustache template added for {}", templateURL.toExternalForm());
        return template;
    }

    /**
     * Clears the cache for the given template.
     *
     * @param template the template
     */
    public void updatedTemplate(MustacheTemplate template) {
        msf.clear(template);
    }

    /**
     * Deletes the given template. The service is unregistered, and the cache is cleared.
     *
     * @param template the template
     */
    public void deleteTemplate(MustacheTemplate template) {
        // 1 - unregister the service
        try {
            ServiceRegistration reg = registrations.remove(template);
            if (reg != null) {
                reg.unregister();
            }
        } catch (Exception e) { //NOSONAR
            // May already have been unregistered during the shutdown sequence.
        }

        // 2 - remove the result from the cache
        msf.clear(template);
    }
}
