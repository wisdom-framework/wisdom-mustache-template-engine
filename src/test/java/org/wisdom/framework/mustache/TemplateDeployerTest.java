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

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ow2.chameleon.core.services.Watcher;
import org.wisdom.api.configuration.ApplicationConfiguration;

import java.io.File;
import java.net.MalformedURLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Check the template deployer behavior.
 */
public class TemplateDeployerTest {

    File directory = new File("target/base");

    @Before
    public void setUp() {
        FileUtils.deleteQuietly(directory);
        directory.mkdirs();
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(directory);
    }


    @Test
    public void start() {
        TemplateDeployer deployer = new TemplateDeployer();
        deployer.watcher = mock(Watcher.class);
        deployer.configuration = mock(ApplicationConfiguration.class);
        when(deployer.configuration.getBaseDir()).thenReturn(directory);
        when(deployer.configuration.getFileWithDefault("application.template.directory",
                "templates")).thenReturn(new File(directory, "templates"));
        deployer.engine = mock(MustacheTemplateCollector.class);
        when(deployer.engine.extension()).thenReturn(MustacheTemplateCollector.EXTENSION);

        deployer.start();
        deployer.stop();
    }

    @Test
    public void testAccept() {
        TemplateDeployer deployer = new TemplateDeployer();
        deployer.watcher = mock(Watcher.class);
        deployer.configuration = mock(ApplicationConfiguration.class);
        when(deployer.configuration.getBaseDir()).thenReturn(directory);
        when(deployer.configuration.getFileWithDefault("application.template.directory",
                "templates")).thenReturn(new File(directory, "templates"));
        deployer.engine = mock(MustacheTemplateCollector.class);
        when(deployer.engine.extension()).thenReturn(MustacheTemplateCollector.EXTENSION);

        assertThat(deployer.accept(new File("src/test/resources/templates/kitten2.mst.json"))).isTrue();
        // no th: in this file:
        assertThat(deployer.accept(new File("src/test/resources/templates/kitten.json"))).isFalse();
    }

    @Test
    public void testDynamism() throws MalformedURLException {
        TemplateDeployer deployer = new TemplateDeployer();
        deployer.watcher = mock(Watcher.class);
        deployer.configuration = mock(ApplicationConfiguration.class);
        when(deployer.configuration.getBaseDir()).thenReturn(directory);
        when(deployer.configuration.getFileWithDefault("application.template.directory",
                "templates")).thenReturn(new File(directory, "templates"));
        deployer.engine = mock(MustacheTemplateCollector.class);
        when(deployer.engine.extension()).thenReturn(MustacheTemplateCollector.EXTENSION);

        File file = new File("src/test/resources/templates/kitten4.mst.xml");
        deployer.onFileCreate(file);
        verify(deployer.engine).addTemplate(file.toURI().toURL());

        deployer.onFileChange(file);
        verify(deployer.engine).updatedTemplate(file);

        deployer.onFileDelete(file);
        verify(deployer.engine).deleteTemplate(file);
    }
}
