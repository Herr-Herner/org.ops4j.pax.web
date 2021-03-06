/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ops4j.pax.web.itest.jetty;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

import java.util.Dictionary;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.support.AnnotatedTestServlet;
import org.ops4j.pax.web.service.spi.WarManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WarIntegrationTest extends ITestBase {

	private static final String TEST_BUNDLE_SYMBOLIC_NAME = "test-bundle";

    private static final Logger LOG = LoggerFactory.getLogger(WarIntegrationTest.class);

	private Bundle installWarBundle;
	
	@Inject 
	private WarManager warManager;
	

	@Configuration
	public static Option[] configure() {
		return combine(configureJetty(),
		            streamBundle(bundle()
		                    .add( AnnotatedTestServlet.class )
		                    .set( Constants.BUNDLE_SYMBOLICNAME, TEST_BUNDLE_SYMBOLIC_NAME )
		                    .set( Constants.EXPORT_PACKAGE, "*" )
		                    .set( Constants.IMPORT_PACKAGE, "*" )
		                    .set( WEB_CONTEXT_PATH, "destroyable")
		                    .build( withBnd() )).noStart()
		        );
	}


	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");
		
		initWebListener();
		
		String bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/war/"
				+ VersionUtil.getProjectVersion() + "/war?"
				+ WEB_CONTEXT_PATH + "=/war";
		installWarBundle = bundleContext.installBundle(bundlePath);
		installWarBundle.start();

		waitForWebListener();
	}

	@After
	public void tearDown() throws BundleException {
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
	}

	/**
	 * You will get a list of bundles installed by default plus your testcase,
	 * wrapped into a bundle called pax-exam-probe
	 */
	@Test
	public void listBundles() {
		for (Bundle b : bundleContext.getBundles()) {
			if (b.getState() != Bundle.ACTIVE && !TEST_BUNDLE_SYMBOLIC_NAME.equalsIgnoreCase(b.getSymbolicName())) {
				fail("Bundle should be active: " + b);
			}

			Dictionary<String,String> headers = b.getHeaders();
			String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
			if (ctxtPath != null) {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName() + " : " + ctxtPath);
			} else {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName());
			}
		}

	}

	@Test
	public void testWC() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/war/wc", "<h1>Hello World</h1>");
			
	}

	@Test
	public void testImage() throws Exception {

		testClient
				.testWebPath("http://127.0.0.1:8181/war/images/logo.png",
				200);

	}

	@Test
	public void testFilterInit() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/war/wc", "Have bundle context in filter: true");
	}
	
	@Test
	public void testStartStopBundle() throws Exception {
		LOG.debug("start/stopping bundle");
		initWebListener();
		
		initServletListener(null);
		
		installWarBundle.stop();
		
		installWarBundle.start();

		waitForWebListener();
		waitForServletListener();
		LOG.debug("Update done, testing bundle");

		testClient.testWebPath("http://127.0.0.1:8181/war/wc", "<h1>Hello World</h1>");
			
	}

	
	@Test
	public void testUpdateBundle() throws Exception {
		LOG.debug("updating bundle");
		initWebListener();
		
		initServletListener(null);
		
		installWarBundle.update();
		
		waitForWebListener();
		waitForServletListener();
		LOG.info("Update done, testing bundle");

		testClient.testWebPath("http://127.0.0.1:8181/war/wc", "<h1>Hello World</h1>");
			
	}
	
	@Test
	public void testWebContainerExample() throws Exception {
			
		testClient.testWebPath("http://127.0.0.1:8181/war/wc/example", "<h1>Hello World</h1>");

		
		testClient.testWebPath("http://127.0.0.1:8181/war/images/logo.png", "", 200, false);
		
	}
	
	@Test
	public void testWebContainerSN() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/war/wc/sn", "<h1>Hello World</h1>");

	}
	
	@Test
	public void testSlash() throws Exception {
			
		testClient.testWebPath("http://127.0.0.1:8181/war/", "<h1>Error Page</h1>", 404, false);

	}
	
	
	@Test
	public void testSubJSP() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/war/wc/subjsp", "<h2>Hello World!</h2>");
	}
	
	@Test
	public void testErrorJSPCall() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/war/wc/error.jsp", "<h1>Error Page</h1>", 404, false);
	}
	
	@Test
	public void testWrongServlet() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/war/wrong/", "<h1>Error Page</h1>", 404, false);
	}

	@Test
	public void testTalkativeServlet() throws Exception {
		testClient.testWebPath("http://127.0.0.1:8181/war/wc/talkative", "<h1>Silent Servlet activated</h1>");
	}

	/**
	 * this is a manual test only, as it's not possible to check if the init/destroy method of the serlvet are called. 
	 * @throws Exception
	 */
	@Test
	public void testWarStop() throws Exception {
	    
	    Bundle bundle = null;
	    
	    for (Bundle b : bundleContext.getBundles()) {
	        if (TEST_BUNDLE_SYMBOLIC_NAME.equalsIgnoreCase(b.getSymbolicName())) {
                bundle = b;
                break;
	        }
	    }
	    
	    assertNotNull(bundle);
	    
	    bundle.start();

	    testClient.testWebPath("http://127.0.0.1:8181/destroyable/test", "TEST OK");
	    
	    warManager.stop(bundle.getBundleId());
	    
	    bundle.stop();
	    
	}

}

