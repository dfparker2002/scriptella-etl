/*
 * Copyright 2006 The Scriptella Project Team.
 *
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
 */

package scriptella.configuration;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import scriptella.spi.DialectIdentifier;
import scriptella.spi.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents dialect based content used inside query/script/onerror elements.
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class DialectBasedContentEl extends XMLConfigurableBase {
    private List<Dialect> dialects;

    public DialectBasedContentEl() {
    }

    public DialectBasedContentEl(final XMLElement element) {
        configure(element);
    }

    public void configure(final XMLElement element) {
        Dialect defaultDialect = null;
        dialects=new ArrayList<Dialect>();
        //iterate through the child nodes of this element
        for (Node node = element.getElement().getFirstChild();node != null;node = node.getNextSibling()) {
            if (isDialectElement(node)) {
                Dialect d = new Dialect();
                d.configure(new XMLElement((Element) node, element));
                dialects.add(d);
            } else {
                //Try to convert the node to resource if possible
                Resource resource = ContentEl.asResource(element, node);
                //If it's a text or include
                if (resource!=null) {
                    //check if we have default dialect instance
                    if (defaultDialect==null) {
                        //if no - create one
                        defaultDialect=new Dialect();
                        defaultDialect.configureDefault(element);
                    }
                    //append a resource to default dialect
                    defaultDialect.contentEl.append(resource);
                }
            }
        }
        if (defaultDialect!=null) {
            dialects.add(defaultDialect); //
        }

    }

    private static boolean isDialectElement(Node node) {
        return node!=null && node instanceof Element && "dialect".equals(node.getNodeName());
    }

    /**
     * This method returns content for specified dialect id or null - if script doesn't support this dialect.
     *
     * @param id dialect identifier. null if any dialect.
     * @return content for specified dialect id or null - if script doesn't support this dialect.
     */
    public ContentEl getContent(final DialectIdentifier id) {
        ContentEl result = null;
        for (Dialect d : dialects) {
            if (d.matches(id)) {
                if (result == null) {
                    result = new ContentEl();
                }
                result.merge(d.getContentEl());
            }
        }
        return result;
    }


    /**
     * For testing purposes
     */
    List<Dialect> getDialects() {
        return dialects;
    }

    static class Dialect extends XMLConfigurableBase {
        private Pattern name;
        private Pattern version;
        private ContentEl contentEl;

        public Pattern getName() {
            return name;
        }

        public void setName(final Pattern name) {
            this.name = name;
        }

        public Pattern getVersion() {
            return version;
        }

        public void setVersion(final Pattern version) {
            this.version = version;
        }

        public ContentEl getContentEl() {
            return contentEl;
        }

        public void configure(final XMLElement element) {
            setPatternProperty(element, "name");
            setPatternProperty(element, "version");
            contentEl = new ContentEl(element);
            setLocation(element, null);
        }

        /**
         * Configures default dialect.
         * @param parent parent element.
         */
        public void configureDefault(final XMLElement parent) {
            setLocation(parent, null);
            contentEl = new ContentEl();
        }


        boolean matches(final DialectIdentifier id) {
            if (id == null) { //if db has no dialect identifier
                //return true only if we have no specified restrictions
                return name == null && version == null;
            }

            if ((name != null) &&
                    !name.matcher(id.getName()).matches()) {
                return false;
            }

            return !((version != null) &&
                    !version.matcher(id.getVersion()).matches());

        }

        public String toString() {
            return "Dialect{" + "name=" + name + ", version=" +
                    version + ", contentEl=" + contentEl + "}";
        }
    }

}
