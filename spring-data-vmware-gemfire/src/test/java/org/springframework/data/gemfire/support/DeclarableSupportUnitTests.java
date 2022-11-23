/*
 * Copyright 2016-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.springframework.data.gemfire.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.beans.factory.BeanFactory;

/**
 * Unit Tests for {@link DeclarableSupport}.
 *
 * @author John Blum
 * @see Test
 * @see Mock
 * @see org.mockito.Mockito
 * @see Spy
 * @see MockitoJUnitRunner
 * @since 2.0.0
 */
@RunWith(MockitoJUnitRunner.class)
public class DeclarableSupportUnitTests {

	@Mock
	private BeanFactory mockBeanFactoryOne;

	@Mock
	private BeanFactory mockBeanFactoryTwo;

	@Spy
	private DeclarableSupport testDeclarableSupport;

	@After
	public void tearDown() {
		testDeclarableSupport.setBeanFactoryKey(null);
		GemfireBeanFactoryLocator.BEAN_FACTORIES.clear();
	}

	@Test
	public void setAndGetBeanFactoryKey() {
		assertThat(testDeclarableSupport.getBeanFactoryKey()).isNull();

		testDeclarableSupport.setBeanFactoryKey("testKey");

		assertThat(testDeclarableSupport.getBeanFactoryKey()).isEqualTo("testKey");

		testDeclarableSupport.setBeanFactoryKey(null);

		assertThat(testDeclarableSupport.getBeanFactoryKey()).isNull();
	}

	@Test
	public void locateBeanFactoryReturnsBeanFactory() {
		GemfireBeanFactoryLocator.BEAN_FACTORIES.put("keyOne", mockBeanFactoryOne);
		GemfireBeanFactoryLocator.BEAN_FACTORIES.put("keyTwo", mockBeanFactoryTwo);

		testDeclarableSupport.setBeanFactoryKey("keyOne");

		assertThat(testDeclarableSupport.getBeanFactoryKey()).isEqualTo("keyOne");
		assertThat(testDeclarableSupport.locateBeanFactory()).isSameAs(mockBeanFactoryOne);
	}

	@Test
	public void locateBeanFactoryWithKeyReturnsBeanFactory() {
		GemfireBeanFactoryLocator.BEAN_FACTORIES.put("keyOne", mockBeanFactoryOne);
		GemfireBeanFactoryLocator.BEAN_FACTORIES.put("keyTwo", mockBeanFactoryTwo);

		assertThat(testDeclarableSupport.getBeanFactoryKey()).isNull();
		assertThat(testDeclarableSupport.locateBeanFactory("keyTwo")).isSameAs(mockBeanFactoryTwo);
	}

	@Test
	public void locateBeanFactoryWithoutKeyReturnsBeanFactory() {
		GemfireBeanFactoryLocator.BEAN_FACTORIES.put("keyOne", mockBeanFactoryOne);
		GemfireBeanFactoryLocator.BEAN_FACTORIES.put("refTwo", mockBeanFactoryOne);

		assertThat(testDeclarableSupport.getBeanFactoryKey()).isNull();
		assertThat(testDeclarableSupport.locateBeanFactory()).isSameAs(mockBeanFactoryOne);
	}

	@Test(expected = IllegalArgumentException.class)
	public void locateBeanFactoryWithUnknownKeyHavingMultipleBeanFactoriesRegisteredThrowsIllegalArgumentException() {

		GemfireBeanFactoryLocator.BEAN_FACTORIES.put("keyOne", mockBeanFactoryOne);
		GemfireBeanFactoryLocator.BEAN_FACTORIES.put("keyTwo", mockBeanFactoryTwo);

		testDeclarableSupport.setBeanFactoryKey("keyOne");

		assertThat(testDeclarableSupport.getBeanFactoryKey()).isEqualTo("keyOne");

		try {
			testDeclarableSupport.locateBeanFactory("UnknownKey");
		}
		catch (IllegalArgumentException expected) {

			assertThat(expected).hasMessage("BeanFactory for key [UnknownKey] was not found");
			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test(expected = IllegalStateException.class)
	public void locateBeanFactoryWithoutKeyHavingMultipleBeanFactoriesRegisteredThrowsIllegalStateException() {

		GemfireBeanFactoryLocator.BEAN_FACTORIES.put("keyOne", mockBeanFactoryOne);
		GemfireBeanFactoryLocator.BEAN_FACTORIES.put("keyTwo", mockBeanFactoryTwo);

		assertThat(testDeclarableSupport.getBeanFactoryKey()).isNull();

		try {
			testDeclarableSupport.locateBeanFactory();
		}
		catch (IllegalStateException expected) {

			assertThat(expected)
				.hasMessage("BeanFactory key must be specified when more than one BeanFactory [keyOne, keyTwo] is registered");

			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test(expected = IllegalStateException.class)
	public void locateBeanFactoryWithKeyWhenNoBeanFactoriesAreRegisteredThrowsIllegalStateException() {

		assertThat(GemfireBeanFactoryLocator.BEAN_FACTORIES).isEmpty();

		try {
			testDeclarableSupport.locateBeanFactory("testKey");
		}
		catch (IllegalStateException expected) {

			assertThat(expected).hasMessage("A BeanFactory was not initialized;"
				+ " Please verify the useBeanFactoryLocator property was properly set");

			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test(expected = IllegalStateException.class)
	public void locateBeanFactoryWithoutKeyWhenNoBeanFactoriesAreRegisteredThrowsIllegalStateException() {

		assertThat(GemfireBeanFactoryLocator.BEAN_FACTORIES).isEmpty();

		try {
			testDeclarableSupport.locateBeanFactory();
		}
		catch (IllegalStateException expected) {

			assertThat(expected).hasMessage("A BeanFactory was not initialized;"
				+ " Please verify the useBeanFactoryLocator property was properly set");

			assertThat(expected).hasNoCause();

			throw expected;
		}
	}

	@Test
	public void getBeanFactoryReturnsBeanFactory() {

		GemfireBeanFactoryLocator.BEAN_FACTORIES.put("keyOne", mockBeanFactoryOne);
		GemfireBeanFactoryLocator.BEAN_FACTORIES.put("keyTwo", mockBeanFactoryTwo);

		testDeclarableSupport.setBeanFactoryKey("keyOne");

		assertThat(testDeclarableSupport.getBeanFactoryKey()).isEqualTo("keyOne");
		assertThat(testDeclarableSupport.getBeanFactory()).isSameAs(mockBeanFactoryOne);
	}

	@Test
	public void closeIsSuccessful() {

		testDeclarableSupport.close();

		verify(testDeclarableSupport, times(1)).close();
	}
}