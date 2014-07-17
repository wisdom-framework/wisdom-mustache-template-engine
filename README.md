# Mustache Template Engine for Wisdom

[Mustache](http://mustache.github.io/) is a logic-less templates. It can be used from almost any language. This 
extension provides the [Mustache Template Language](http://mustache.github.io/mustache.5.html) for server-side 
templating. It relies on [Mustache.java](https://github.com/spullara/mustache.java), 
and so inherits from all its features. This extension can be used along with Thymeleaf, 
the default template engine of Wisdom.

## Usage

To use Mustache, just add the following Maven dependency to you `pom.xml` file:

```
<dependency>
    <groupId>org.wisdom-framework</groupId>
    <artifactId>wisdom-mustache-template-engine</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Then, you can write Mustache templates. These templates are either in:

* `src/main/resources/assets` for templates embedded within your application
* `src/main/assets` for external templates (not packaged with your application)
 
Mustache templates must have one of the following extension:
 
* `.mst` - generates a `text/plain` output (you can also use `.mst.plain`)
* `.mst.html` - generates a `text/html` output
* `.mst.json` - generates a `application/json` output
* `.mst.xml` - generates a `application/xml` output

For instance, let's create `src/main/resources/assets/list.mst.json`:

```
[
    {{#items}}
        {
        "Name": "{{name}}",
        "Price": "{{price}}",
        "Feature:" [
        {{#features}}
             {{description}}
         {{/features}}
         ]}
     {{/items}}
]
```

Once you have created your template, using them is exactly like for regular templates:

```
@Controller
public class MyController extends DefaultController {

    /**
     * Retrieves the list template.
     **/
    @View("list")
    Template list;

    List<Item> items() {
        return Arrays.asList(
            new Item("Romeo", "$19.99", Arrays.asList(new Feature("New!"), new Feature("Awesome!"))),
            new Item("Tom", "$29.99", Arrays.asList(new Feature("Old."), new Feature("Ugly.")))
        );
    }
        
    @Route(method = HttpMethod.GET, uri = "/list")
    public Result jen() {
        // render the template, and build the JSON response.
        return ok(render(list, "items", items()));
    }

    static class Item {
        Item(String name, String price, List<Feature> features) {
            this.name = name;
            this.price = price;
            this.features = features;
        }
        String name, price;
        List<Feature> features;
    }

    static class Feature {
        Feature(String description) {
            this.description = description;
        }
        String description;
    }
}
```

## Accessing HTTP data from the templates

This extension automatically injects HTTP data in the template context. Are accessible:

* session data
* flash data
* HTTP parameters
  

## Template as a Service

Don't forget that every template matching one of the extensions listed above is exposed as an OSGi service, 
and so inherits from its dynamic. This also implies that `partials` are resolved at runtime.


