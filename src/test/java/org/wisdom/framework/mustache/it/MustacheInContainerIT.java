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
package org.wisdom.framework.mustache.it;

import com.github.mustachejava.TemplateFunction;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ow2.chameleon.testing.helpers.OSGiHelper;
import org.wisdom.api.DefaultController;
import org.wisdom.api.http.MimeTypes;
import org.wisdom.api.http.Renderable;
import org.wisdom.api.templates.Template;
import org.wisdom.framework.mustache.Cat;
import org.wisdom.framework.mustache.MustacheTemplate;
import org.wisdom.test.WisdomRunner;
import org.wisdom.test.parents.WisdomTest;

import javax.annotation.Nullable;
import java.io.File;
import java.net.MalformedURLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the Mustache Template Engine.
 */
@RunWith(WisdomRunner.class)
public class MustacheInContainerIT extends WisdomTest {

    private OSGiHelper osgi;

    @Before
    public void setUp() {
        osgi = new OSGiHelper(context);
    }

    @After
    public void tearDown() {
        osgi.dispose();
        osgi = null;
    }

    @Test
    public void testJsonTemplate() throws MalformedURLException {
        Template template = osgi.getServiceObject(Template.class, "(name=kitten2)");
        assertThat(template).isNotNull();
        assertThat(template.engine()).isEqualTo("mustache");
        assertThat(template.mimetype()).isEqualTo(MimeTypes.JSON);
        assertThat(template.name()).isEqualTo("kitten2");

        Renderable renderable = template.render(new DefaultController() {
        }, ImmutableMap.<String, Object>of("items", Cat.cats()));

        assertThat(renderable.mimetype()).isEqualTo(MimeTypes.JSON);
        assertThat((String) renderable.content())
                .contains("\"name\": \"romeo\",")
                .contains("\"name\": \"tom\",");
    }

    @Test
    public void testTemplateUsingAFunction() throws MalformedURLException {
        Template template = osgi.getServiceObject(Template.class, "(name=function/function)");
        assertThat(template.engine()).isEqualTo("mustache");
        assertThat(template.mimetype()).isEqualTo(MimeTypes.TEXT);
        assertThat(template.name()).isEqualTo("function/function");

        Renderable renderable = template.render(new DefaultController() {
        }, ImmutableMap.of(
                "name", "Wisdom",
                "wrapped", new TemplateFunction() {
                    @Nullable
                    @Override
                    public String apply(String input) {
                        return "<b>" + input + "</b>";
                    }
                }

        ));

        assertThat(renderable.mimetype()).isEqualTo(MimeTypes.TEXT);
        assertThat((String) renderable.content()).contains("<b>").contains("Wisdom is awesome.").contains("</b>");
    }

    @Test
    public void testMailTemplate() throws MalformedURLException {
        Template template = osgi.getServiceObject(Template.class, "(name=mustache/mail)");
        assertThat(template.engine()).isEqualTo("mustache");
        assertThat(template.mimetype()).isEqualTo(MimeTypes.TEXT);
        assertThat(template.name()).isEqualTo("mustache/mail");

        Renderable renderable = template.render(new DefaultController() {
        }, ImmutableMap.<String, Object>of(
                "name", "Wisdom",
                "value", 10000,
                "taxed_value", 10000 - (10000 * 0.4),
                "in_ca", true
        ));

        assertThat(renderable.mimetype()).isEqualTo(MimeTypes.TEXT);
        assertThat((String) renderable.content())
                .contains("Hello Wisdom")
                .contains("You have just won 10000 dollars!")
                .contains("Well, 6000.0 dollars, after taxes.");
    }

    @Test
    public void testSectionAndInvertedSection() throws MalformedURLException {
        Template template = osgi.getServiceObject(Template.class, "(name=mustache/section)");

        Renderable renderable = template.render(new DefaultController() {
        }, ImmutableMap.<String, Object>of(
                "repo", ImmutableList.of(
                        ImmutableMap.of("name", "central"),
                        ImmutableMap.of("name", "local"))
        ));

        assertThat(renderable.mimetype()).isEqualTo(MimeTypes.TEXT);
        assertThat((String) renderable.content())
                .contains("<b>central</b>")
                .contains("<b>local</b>");

        renderable = template.render(new DefaultController() {
        }, ImmutableMap.<String, Object>of(
                "repo", ImmutableList.of()
        ));

        assertThat((String) renderable.content())
                .contains("No repos :(");
    }

    @Test
    public void testPartials() throws MalformedURLException {
        Template template = osgi.getServiceObject(Template.class, "(name=mustache/base)");
        assertThat(template.engine()).isEqualTo("mustache");
        assertThat(template.mimetype()).isEqualTo(MimeTypes.HTML);
        assertThat(template.name()).isEqualTo("mustache/base");

        Renderable renderable = template.render(new DefaultController() {
        }, ImmutableMap.<String, Object>of(
                "names", ImmutableList.of(
                        ImmutableMap.of("name", "romeo"),
                        ImmutableMap.of("name", "gros minet"),
                        ImmutableMap.of("name", "tom")
                )
        ));

        assertThat(renderable.mimetype()).isEqualTo(MimeTypes.HTML);
        System.out.println(renderable.content());
//        assertThat((String) renderable.content())
//                .contains("Hello Wisdom")
//                .contains("You have just won 10000 dollars!")
//                .contains("Well, 6000.0 dollars, after taxes.");
    }


}
