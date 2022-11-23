/*
 * Copyright 2017-2022 the original author or authors.
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
 */
package org.springframework.data.gemfire.config.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.gemfire.client.PoolFactoryBean;
import org.springframework.data.gemfire.tests.integration.IntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration Tests for {@link PoolConfigurer}.
 *
 * @author John Blum
 * @see Test
 * @see org.apache.geode.cache.client.Pool
 * @see Bean
 * @see Configuration
 * @see PoolFactoryBean
 * @see AddPoolConfiguration
 * @see AddPoolsConfiguration
 * @see PoolConfigurer
 * @see EnablePool
 * @see EnablePools
 * @see IntegrationTestsSupport
 * @see ContextConfiguration
 * @see SpringRunner
 * @since 2.1.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class PoolConfigurerIntegrationTests extends IntegrationTestsSupport {

	@Autowired
	@Qualifier("configurerOne")
	private TestPoolConfigurer configurerOne;

	@Autowired
	@Qualifier("configurerTwo")
	private TestPoolConfigurer configurerTwo;

	protected void assertPoolConfigurerCalled(TestPoolConfigurer configurer, String... beanNames) {

		assertThat(configurer).isNotNull();
		assertThat(configurer).hasSize(beanNames.length);
		assertThat(configurer).contains(beanNames);
	}

	@Test
	public void poolConfigurerOneCalledSuccessfully() {
		assertPoolConfigurerCalled(this.configurerOne, "poolOne", "poolTwo", "poolThree");
	}

	@Test
	public void poolConfigurerTwoCalledSuccessfully() {
		assertPoolConfigurerCalled(this.configurerTwo, "poolOne", "poolTwo", "poolThree");
	}

	@Configuration
	@EnablePools(pools = {
		@EnablePool(name = "poolOne"),
		@EnablePool(name = "poolTwo"),
		@EnablePool(name = "poolThree"),
	})
	static class TestConfiguration {

		@Bean
		TestPoolConfigurer configurerOne() {
			return new TestPoolConfigurer();
		}

		@Bean
		TestPoolConfigurer configurerTwo() {
			return new TestPoolConfigurer();
		}

		@Bean
		Object nonRelevantBean() {
			return "test";
		}
	}

	static class TestPoolConfigurer implements Iterable<String>, PoolConfigurer {

		private final Set<String> beanNames = new HashSet<>();

		@Override
		public void configure(String beanName, PoolFactoryBean bean) {
			this.beanNames.add(beanName);
		}

		@Override
		public Iterator<String> iterator() {
			return Collections.unmodifiableSet(this.beanNames).iterator();
		}
	}
}