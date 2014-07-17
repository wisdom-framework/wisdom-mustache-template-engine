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

import com.github.mustachejava.MustacheException;
import com.github.mustachejava.TemplateFunction;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.wisdom.api.Controller;
import org.wisdom.api.DefaultController;
import org.wisdom.api.http.MimeTypes;
import org.wisdom.api.http.Renderable;
import org.wisdom.api.http.Result;
import org.wisdom.test.parents.Action;
import org.wisdom.test.parents.FakeContext;
import org.wisdom.test.parents.Invocation;

import javax.annotation.Nullable;
import java.io.File;
import java.net.MalformedURLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.wisdom.api.http.Results.ok;
import static org.wisdom.test.parents.Action.action;

public class MustacheTemplateTest {

    ExtendedMustacheFactory factory = new ExtendedMustacheFactory(null);

    private Controller controller = new DefaultController() {
    };

    @Test
    public void testJsonTemplate() throws MalformedURLException {
        File file = new File("src/test/resources/templates/kitten2.mst.json");
        assertThat(file).isFile();

        MustacheTemplate template = new MustacheTemplate(factory, file.toURI().toURL());
        assertThat(template.engine()).isEqualTo("mustache");
        assertThat(template.getURL()).isEqualTo(file.toURI().toURL());
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
    public void testHTMLTemplate() throws MalformedURLException {
        File file = new File("src/test/resources/templates/kitten3.mst.html");
        assertThat(file).isFile();

        MustacheTemplate template = new MustacheTemplate(factory, file.toURI().toURL());
        assertThat(template.engine()).isEqualTo("mustache");
        assertThat(template.getURL()).isEqualTo(file.toURI().toURL());
        assertThat(template.mimetype()).isEqualTo(MimeTypes.HTML);
        assertThat(template.name()).isEqualTo("kitten3");

        Renderable renderable = template.render(new DefaultController() {
        }, ImmutableMap.<String, Object>of("items", Cat.cats()));

        assertThat(renderable.mimetype()).isEqualTo(MimeTypes.HTML);
        assertThat((String) renderable.content())
                .contains("name: romeo - age: 2");

        assertThat(template.getServiceProperties()).isNotNull();
    }

    @Test
    public void testXMLTemplate() throws MalformedURLException {
        File file = new File("src/test/resources/templates/kitten4.mst.xml");
        assertThat(file).isFile();

        MustacheTemplate template = new MustacheTemplate(factory, file.toURI().toURL());
        assertThat(template.engine()).isEqualTo("mustache");
        assertThat(template.getURL()).isEqualTo(file.toURI().toURL());
        assertThat(template.mimetype()).isEqualTo(MimeTypes.XML);
        assertThat(template.name()).isEqualTo("kitten4");

        Renderable renderable = template.render(new DefaultController() {
        }, ImmutableMap.<String, Object>of("items", Cat.cats()));

        assertThat(renderable.mimetype()).isEqualTo(MimeTypes.XML);
        assertThat((String) renderable.content())
                .contains("<cat>name: romeo - age: 2</cat>");
    }

    @Test
    public void testPlainTemplate() throws MalformedURLException {
        File file = new File("src/test/resources/templates/kitten1.mst");
        assertThat(file).isFile();

        MustacheTemplate template = new MustacheTemplate(factory, file.toURI().toURL());
        assertThat(template.engine()).isEqualTo("mustache");
        assertThat(template.getURL()).isEqualTo(file.toURI().toURL());
        assertThat(template.mimetype()).isEqualTo(MimeTypes.TEXT);
        assertThat(template.name()).isEqualTo("kitten1");

        Renderable renderable = template.render(new DefaultController() {
        }, ImmutableMap.<String, Object>of("items", Cat.cats()));

        assertThat(renderable.mimetype()).isEqualTo(MimeTypes.TEXT);
        assertThat((String) renderable.content())
                .contains("name: romeo, age: 2");
    }

    @Test
    public void testTemplateUsingAFunction() throws MalformedURLException {
        File file = new File("src/test/resources/templates/function/function.mst.plain");
        assertThat(file).isFile();

        MustacheTemplate template = new MustacheTemplate(factory, file.toURI().toURL());
        assertThat(template.engine()).isEqualTo("mustache");
        assertThat(template.getURL()).isEqualTo(file.toURI().toURL());
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
    public void testEscaping() throws MalformedURLException {
        File file = new File("src/test/resources/templates/mustache/escape.mst");
        assertThat(file).isFile();

        final MustacheTemplate template = new MustacheTemplate(factory, file.toURI().toURL());

        Renderable renderable = template.render(new DefaultController() {
        }, ImmutableMap.<String, Object>of(
                "name", "Clement",
                "company", "<b>None</b>"
        ));

        assertThat(renderable.mimetype()).isEqualTo(MimeTypes.TEXT);
        assertThat((String) renderable.content())
                .contains("Clement")
                .contains("&lt;b&gt;None&lt;/b&gt;")
                .contains("<b>None</b>");
    }

    @Test
    public void testNotShown() throws MalformedURLException {
        File file = new File("src/test/resources/templates/mustache/shown.mst");
        assertThat(file).isFile();

        final MustacheTemplate template = new MustacheTemplate(factory, file.toURI().toURL());

        Renderable renderable = template.render(new DefaultController() {
        }, ImmutableMap.<String, Object>of(
                "person", false
        ));

        assertThat(renderable.mimetype()).isEqualTo(MimeTypes.TEXT);
        assertThat((String) renderable.content())
                .contains("Shown")
                .doesNotContain("Never");
    }

    @Test
    public void testSectionAndInvertedSection() throws MalformedURLException {
        File file = new File("src/test/resources/templates/mustache/section.mst");
        assertThat(file).isFile();

        final MustacheTemplate template = new MustacheTemplate(factory, file.toURI().toURL());

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
    public void testSessionScope() throws MalformedURLException {
        File file = new File("src/test/resources/templates/var.mst.html");
        assertThat(file).isFile();

        final MustacheTemplate template = new MustacheTemplate(factory, file.toURI().toURL());

        Action.ActionResult result = action(new Invocation() {
            @Override
            public Result invoke() throws Throwable {
                return ok(template.render(controller, ImmutableMap.<String, Object>of("key",
                        "test")));
            }
        }).with(new FakeContext().addToSession("key2", "session")).invoke();

        String content = (String) result.getResult().getRenderable().content();
        assertThat(content)
                .contains("<span>KEY</span> = test")
                .contains("<span>KEY2</span> = session");
    }

    @Test
    public void testFlashScope() throws MalformedURLException {
        File file = new File("src/test/resources/templates/var.mst.html");
        assertThat(file).isFile();

        final MustacheTemplate template = new MustacheTemplate(factory, file.toURI().toURL());

        Action.ActionResult result = action(new Invocation() {
            @Override
            public Result invoke() throws Throwable {
                context().flash().put("key2", "ongoing");
                return ok(template.render(controller));
            }
        }).with(new FakeContext().addToFlash("key", "incoming")).invoke();

        String content = (String) result.getResult().getRenderable().content();
        assertThat(content)
                .contains("<span>KEY</span> = incoming")
                .contains("<span>KEY2</span> = ongoing");
    }

    @Test
    public void testParameter() throws MalformedURLException {
        File file = new File("src/test/resources/templates/var.mst.html");
        assertThat(file).isFile();

        final MustacheTemplate template = new MustacheTemplate(factory, file.toURI().toURL());

        Action.ActionResult result = action(new Invocation() {
            @Override
            public Result invoke() throws Throwable {
                context().session().put("key2", "ongoing");
                return ok(template.render(controller));
            }
        }).parameter("key", "param").invoke();

        String content = (String) result.getResult().getRenderable().content();
        assertThat(content)
                .contains("<span>KEY</span> = param")
                .contains("<span>KEY2</span> = ongoing");
    }

    @Test(expected = MustacheException.class)
    public void testSyntaxError() throws MalformedURLException {
        File file = new File("src/test/resources/templates/erroneous/syntax-error.mst");
        assertThat(file).isFile();

        MustacheTemplate template = new MustacheTemplate(factory, file.toURI().toURL());
        assertThat(template.engine()).isEqualTo("mustache");
        assertThat(template.getURL()).isEqualTo(file.toURI().toURL());
        assertThat(template.mimetype()).isEqualTo(MimeTypes.TEXT);
        assertThat(template.name()).isEqualTo("erroneous/syntax-error");

        Renderable renderable = template.render(new DefaultController() {
        }, ImmutableMap.<String, Object>of("items", Cat.cats()));

        fail("Syntax error expected");
    }

    @Test
    public void testMissingVariable() throws MalformedURLException {
        File file = new File("src/test/resources/templates/erroneous/missing-variable.mst");
        assertThat(file).isFile();

        MustacheTemplate template = new MustacheTemplate(factory, file.toURI().toURL());
        assertThat(template.engine()).isEqualTo("mustache");
        assertThat(template.getURL()).isEqualTo(file.toURI().toURL());
        assertThat(template.mimetype()).isEqualTo(MimeTypes.TEXT);
        assertThat(template.name()).isEqualTo("erroneous/missing-variable");

        Renderable renderable = template.render(new DefaultController() {
        }, ImmutableMap.<String, Object>of());

        assertThat((String)renderable.content()).doesNotContain("{{name}}");
    }


}