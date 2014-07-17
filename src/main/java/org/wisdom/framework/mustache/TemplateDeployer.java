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
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.ow2.chameleon.core.services.AbstractDeployer;
import org.ow2.chameleon.core.services.Deployer;
import org.ow2.chameleon.core.services.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wisdom.api.annotations.Service;
import org.wisdom.api.configuration.ApplicationConfiguration;

import java.io.File;
import java.net.MalformedURLException;

/**
 * A Chameleon deployer tracking template files.
 */
@Service
public class TemplateDeployer extends AbstractDeployer implements Deployer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateDeployer.class);

    @Requires
    MustacheTemplateCollector engine;

    @Requires
    Watcher watcher;

    @Requires
    ApplicationConfiguration configuration;

    /**
     * The directory containing templates.
     */
    private File directory;


    @Validate
    public void start() {
        LOGGER.info("Starting mustache template deployer");

        directory = configuration.getFileWithDefault("application.template.directory", "templates");
        if (!directory.isDirectory()) {
            LOGGER.info("Creating the template directory : {}", directory.getAbsolutePath());
            directory.mkdirs();
        }
        LOGGER.info("Template directory set to {}", directory.getAbsolutePath());

        watcher.add(new File(configuration.getBaseDir(), "templates"), true);
    }

    @Invalidate
    public void stop() {
        try {
            watcher.removeAndStopIfNeeded(directory);
        } catch (RuntimeException e) { //NOSONAR
            // An exception can be thrown when the platform is shutting down.
            // ignore it.
        }
    }

    /**
     * Checks whether the given file is a HTML file, and if it is and the file does exist,
     * check it contains the 'th' prefix indicating a thymeleaf template.
     *
     * @param file the file
     * @return {@literal true} if the file is accepted by the current deployer, {@literal false} otherwise
     */
    @Override
    public boolean accept(File file) {
        return file.getName().contains(".mst");
    }

    /**
     * Callback called when an accepted file is created.
     *
     * @param file the new file
     */
    @Override
    public void onFileCreate(File file) {
        try {
            if (engine != null) {
                engine.addTemplate(file.toURI().toURL());
            }
        } catch (MalformedURLException e) {
            LOGGER.error("Cannot compute the url of file {}", file.getAbsolutePath(), e);
        }
    }

    /**
     * Callback called when an accepted file is updated.
     *
     * @param file the updated file
     */
    @Override
    public void onFileChange(File file) {
        if (engine != null) {
            engine.updatedTemplate(file);
        }
    }

    /**
     * Callback called when an accepted file is deleted.
     *
     * @param file the file
     */
    @Override
    public void onFileDelete(File file) {
        if (engine != null) {
            engine.deleteTemplate(file);
        }
    }
}
