/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2014 RedHat
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

package org.wildfly.camel.test.jndi;

import javax.naming.Context;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.CamelContextFactory;
import org.wildfly.camel.WildflyCamelContext;
import org.wildfly.camel.test.smoke.subA.HelloBean;

/**
 * Deploys a module which registers a {@link HelloBean} in JNDI, which is later used in a route.
 *
 * @author thomas.diesler@jboss.com
 * @since 03-Jun-2013
 */
@RunWith(Arquillian.class)
public class JNDIIntegrationTest {

    @ArquillianResource
    CamelContextFactory contextFactory;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "jndi-integration-tests");
        archive.addClasses(HelloBean.class);
        return archive;
    }

    @Test
    public void testBeanTransform() throws Exception {
        WildflyCamelContext camelctx = contextFactory.createCamelContext();

        // Bind the bean to JNDI
        Context context = camelctx.getNamingContext();
        context.bind("helloBean", new HelloBean());
        try {
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start").beanRef("helloBean");
                }
            });
            camelctx.start();

            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "Kermit", String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            context.unbind("helloBean");
        }
    }

}