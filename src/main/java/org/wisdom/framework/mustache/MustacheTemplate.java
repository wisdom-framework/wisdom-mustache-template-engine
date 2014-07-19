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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.wisdom.api.Controller;
import org.wisdom.api.bodies.RenderableString;
import org.wisdom.api.http.Context;
import org.wisdom.api.http.MimeTypes;
import org.wisdom.api.http.Renderable;
import org.wisdom.api.templates.Template;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Implementation of {@link org.wisdom.api.templates.Template} for Mustache.
 * <p>
 * It computes the mime-type from the template's extensions. Are supported: mst.json, mst.xml,
 * mst.plain and mst.html. In all the other case, {@literal text/plain} is used.
 */
public class MustacheTemplate implements Template {

    /**
     * The template location.
     */
    public static final String TEMPLATES = "/templates/";

    private final URL url;
    private final DefaultMustacheFactory msf;
    protected Mustache compiled;
    private final String path;
    private final String mime;

    /**
     * Creates the template object.
     *
     * @param msf         the factory used to compile the template
     * @param templateURL the template url
     */
    public MustacheTemplate(DefaultMustacheFactory msf, URL templateURL) {
        this.url = templateURL;
        this.msf = msf;
        String externalForm = templateURL.toExternalForm();
        int indexOfTemplates = externalForm.indexOf(TEMPLATES);
        if (indexOfTemplates == -1) {
            this.path = FilenameUtils.getBaseName(templateURL.getFile());
        } else {
            String name = externalForm.substring(indexOfTemplates + TEMPLATES.length());
            int extIndex = name.indexOf(".mst");
            this.path = name.substring(0, extIndex);
        }

        mime = getMimeTypeForURL(externalForm);
    }

    private void compile() {
        InputStream stream = null;
        try {
            stream = this.url.openStream();
            Reader reader = new InputStreamReader(stream);
            this.compiled = msf.compile(reader, path);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read template " + url.toExternalForm());
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    /**
     * Gets the mime types for the template having the given url.
     *
     * @param externalForm the external form of the template's url.
     * @return the mime type, {@literal TEXT/PLAIN} if not found. The detection is based on the url's extension. Are
     * supported {@literal .mst.html}, {@literal .mst.json}, and {@literal .mst.xml}. Others are considered as
     * {@literal text/plain}.
     */
    public static String getMimeTypeForURL(String externalForm) {
        if (externalForm.endsWith(".mst.html")) {
            return MimeTypes.HTML;
        } else if (externalForm.endsWith(".mst.json")) {
            return MimeTypes.JSON;
        } else if (externalForm.endsWith(".mst.xml")) {
            return MimeTypes.XML;
        } else {
            // can be .mst.plain or just .mst
            return MimeTypes.TEXT;
        }
    }

    /**
     * Checks whether the given url has the right extension to be managed by the Mustache Template Engine.
     *
     * @param externalForm the external form of the template's url.
     * @return {@code true} if the template's url ends with {@literal .mst} or contains {@literal .mst.}.
     */
    public static boolean isMustacheTemplate(String externalForm) {
        return externalForm.endsWith(".mst") || externalForm.contains(".mst.");
    }

    /**
     * @return the full url of the template source.
     */
    public URL getURL() {
        return url;
    }

    /**
     * @return the template path, usually the template file path without the extension.
     */
    @Override
    public String name() {
        return path;
    }

    /**
     * @return the template full path. For example, for a file, it will be the file path (including extension).
     */
    @Override
    public String fullName() {
        return url.toExternalForm();
    }

    /**
     * @return the path of the template engine, generally the path of the technology.
     */
    @Override
    public String engine() {
        return "mustache";
    }

    /**
     * @return the mime type of the document produced by the template.
     */
    @Override
    public String mimetype() {
        return mime;
    }

    /**
     * Renders the template.
     *
     * @param controller the controller having requested the rendering.
     * @param variables  the parameters
     * @return the rendered object.
     */
    @Override
    public Renderable render(Controller controller, Map<String, Object> variables) {

        // Check whether we already have compiled the template.
        // To support partials, we do that at the last minute.
        if (compiled == null) {
            compile();
        }

        Map<String, Object> context = new HashMap<>();

        // If we have a HTTP context, extract data.
        Context ctx = org.wisdom.api.http.Context.CONTEXT.get();
        if (ctx != null) {
            context.putAll(ctx.session().getData());
            context.putAll(ctx.flash().getCurrentFlashCookieData());
            context.putAll(ctx.flash().getOutgoingFlashCookieData());
            for (Map.Entry<String, List<String>> entry : ctx.parameters().entrySet()) {
                if (entry.getValue().size() == 1) {
                    context.put(entry.getKey(), entry.getValue().get(0));
                } else {
                    context.put(entry.getKey(), entry.getValue());
                }
            }
        }
        context.putAll(variables);

        StringWriter writer = new StringWriter();
        compiled.execute(writer, context);
        return new RenderableString(writer.toString(), mimetype());
    }

    /**
     * Renders the template without explicit variables.
     *
     * @param controller the controller having requested the rendering.
     * @return the rendered object.
     */
    @Override
    public Renderable render(Controller controller) {
        return render(controller, new HashMap<String, Object>());
    }

    /**
     * @return the service properties exposed for the current template instance.
     */
    public Dictionary getServiceProperties() {
        Properties props = new Properties();
        props.put("name", name());
        props.put("fullName", fullName());
        props.put("mimetype", mimetype());
        props.put("engine", engine());
        return props;
    }
}
