/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.nmr.core;

import static org.junit.Assert.*;

import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Pattern;
import org.apache.servicemix.nmr.core.ExchangeImpl;
import org.junit.Test;


public class ExchangeImplTest {

	@Test
	public void testInOnly() {
		Exchange e = new ExchangeImpl(Pattern.InOnly);
		assertNotNull(e.getIn());
		assertNull(e.getOut());
		assertNull(e.getFault());
	}

	@Test
	public void testRobustInOnly() {
		Exchange e = new ExchangeImpl(Pattern.RobustInOnly);
		assertNotNull(e.getIn());
		assertNull(e.getOut());
		assertNotNull(e.getFault());
	}

	@Test
	public void testInOut() {
		Exchange e = new ExchangeImpl(Pattern.InOut);
		assertNotNull(e.getIn());
		assertNotNull(e.getOut());
		assertNotNull(e.getFault());
	}

	@Test
	public void testInOptionalOut() {
		Exchange e = new ExchangeImpl(Pattern.InOptionalOut);
		assertNotNull(e.getIn());
		assertNotNull(e.getOut());
		assertNotNull(e.getFault());
	}

}
