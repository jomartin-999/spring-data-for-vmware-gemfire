/*
 * Copyright (c) VMware, Inc. 2022-2023. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.springframework.data.gemfire.serialization;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.RegionService;
import org.apache.geode.pdx.PdxInstance;
import org.apache.geode.pdx.PdxSerializer;
import org.apache.geode.pdx.ReflectionBasedAutoSerializer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.annotation.Id;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.config.annotation.EnableEntityDefinedRegions;
import org.springframework.data.gemfire.config.annotation.EnablePdx;
import org.springframework.data.gemfire.config.annotation.PeerCacheApplication;
import org.springframework.data.gemfire.mapping.MappingPdxSerializer;
import org.springframework.data.gemfire.mapping.annotation.ClientRegion;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

/**
 * Integration Tests testing and asserting the de/serialization of a complex application domain object model
 * using Apache Geode PDX serialization.
 *
 * @author John Blum
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.pdx.PdxInstance
 * @see org.apache.geode.pdx.PdxSerializer
 * @see org.springframework.data.gemfire.GemfireTemplate
 * @see org.springframework.data.gemfire.config.annotation.EnableEntityDefinedRegions
 * @see org.springframework.data.gemfire.config.annotation.EnablePdx
 * @see org.springframework.data.gemfire.config.annotation.PeerCacheApplication
 * @see org.springframework.data.gemfire.mapping.MappingPdxSerializer
 * @see org.springframework.test.context.ActiveProfiles
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 2.7.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@ActiveProfiles("MappingPdxSerializer")
//@ActiveProfiles("ReflectionBasedAutoSerializer")
@SuppressWarnings("unused")
public class PdxAutoSerializationOfNestedObjects {


    @Autowired
    private Environment environment;

    @Autowired
    private GemfireTemplate gemfireTemplate;

    @Before
    public void assertGeodeCacheAndRegionConfiguration() {

        assertThat(this.environment).isNotNull();
        assertThat(this.gemfireTemplate).isNotNull();

        org.apache.geode.cache.Region<Object, Object> orders = this.gemfireTemplate.getRegion();

        assertThat(orders).isNotNull();
		assertThat(orders.getName()).isEqualTo("adj-coverage-region");
        assertThat(orders.getAttributes()).isNotNull();
        assertThat(orders.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.PARTITION);

        RegionService regionService = orders.getRegionService();

        assertThat(regionService).isInstanceOf(GemFireCache.class);

        Class<? extends PdxSerializer> expectedPdxSerializerType =
                Arrays.asList(this.environment.getActiveProfiles()).contains("MappingPdxSerializer")
                        ? MappingPdxSerializer.class
                        : ReflectionBasedAutoSerializer.class;

        GemFireCache cache = (GemFireCache) regionService;

        assertThat(cache).isNotNull();
        assertThat(cache.getPdxSerializer()).isInstanceOf(expectedPdxSerializerType);
    }

    @Test
    public void serializeNestedObject() {

        LinkedList<CoverageTierCostShareStep> coverageTierCostShareSteps = new LinkedList<>();
        coverageTierCostShareSteps.add(new CoverageTierCostShareStep(12, 2, "", 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, Instant.now(), Instant.now()));
        List<CoverageTier> coverageTiers = new LinkedList<>();
        coverageTiers.add(new CoverageTier(1, "", 1, LocalDate.now(), LocalDate.now(), Instant.now(), Instant.now(), coverageTierCostShareSteps));
        List<Coverage> coverages = new LinkedList<>();
        coverages.add(new Coverage(1, "", "", LocalDate.now(), "", "", LocalDate.now(), Instant.now(), Instant.now(), new DaysSupply(45, LocalDate.now(), LocalDate.now(), 3.0, 4.7, Instant.now(), Instant.now()), coverageTiers));
        List<PharmacyNpiValidation> validations = new LinkedList<>();
        validations.add(new PharmacyNpiValidation(ClaimOrigination.BATCH, LocalDate.now(), LocalDate.now(), "status"));
        PharmacyBenefitPlanWithCoveragesDAO pharmacyBenefitPlanWithCoveragesDAO = new PharmacyBenefitPlanWithCoveragesDAO(10, "", "", 11, "", LocalDate.now(), LocalDate.now(), Instant.now(), Instant.now(), coverages, "", validations, 5);
        gemfireTemplate.put(1L, pharmacyBenefitPlanWithCoveragesDAO);

        Object object = this.gemfireTemplate.get(1L);

        assertThat(object).isInstanceOf(PdxInstance.class);

        PdxInstance pdxInstance = (PdxInstance) object;

        List<Coverage> coverageObject = (List<Coverage>) pdxInstance.getField("coverage");


        Integer coverageTierCostShareStepId = coverages.get(0).getCoverageTier().get(0).coverageTierCostShareStep.get(0).getCoverageTierCostShareStepId();

        assertThat(coverageTierCostShareStepId).isEqualTo(12);
    }

    @PeerCacheApplication(name = "PdxSerializationOfNestedObjectsIntegrationTests", copyOnRead = true)
    @EnableEntityDefinedRegions(basePackageClasses = PdxAutoSerializationOfNestedObjects.class)
    @EnablePdx(serializerBeanName = "pdxSerializer", readSerialized = true)
    static class TestConfiguration {

        @Bean("pdxSerializer")
        @Profile("MappingPdxSerializer")
        MappingPdxSerializer customMappingPdxSerializer() {

            Set<Class<?>> includedTypes = CollectionUtils.asSet(PharmacyBenefitPlanWithCoveragesDAO.class, CoverageTierCostShareStep.class, PharmacyNpiValidation.class, Coverage.class, CoverageTier.class, DaysSupply.class, ClaimOrigination.class);

            MappingPdxSerializer customMappingPdxSerializer = spy(MappingPdxSerializer.newMappingPdxSerializer());

            customMappingPdxSerializer.setIncludeTypeFilters(includedTypes::contains);

            return customMappingPdxSerializer;
        }

        @Bean("pdxSerializer")
        @Profile("ReflectionBasedAutoSerializer")
        PdxSerializer reflectionAutoSerializer() {
            return new ReflectionBasedAutoSerializer(".*");
        }

        @Bean
        @DependsOn("adj-coverage-region")
        GemfireTemplate ordersTemplate(GemFireCache cache) {
            return new GemfireTemplate(cache.getRegion("/adj-coverage-region"));
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    static class CoverageTierCostShareStep {
        Integer coverageTierCostShareStepId;
        Integer stepOrderCount;
        String stepDescription;
        Double stepMinimumAmount;
        Double stepMaximumAmount;
        Double stepMultiplierNumber;
        Double copayAmount;
        Double coinsurancePercent;
        Double coinsuranceMinimumAmount;
        Double coinsuranceMaximumAmount;
        Instant systemBeginDate;
        Instant systemEndDate;
    }

    @ClientRegion(name = "adj-coverage-region")
    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "allArgConstructor")
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @EnablePdx(readSerialized = true)
    static class PharmacyBenefitPlanWithCoveragesDAO {

        @Id
        Integer pharmacyBenefitPlanID;
        String planCode;
        String networkProductName;
        Integer formularyId;
        String costShareLoadLevelDescription;
        LocalDate changeEffectiveDate;
        LocalDate planEndDate;
        Instant systemBeginDate;
        Instant systemEndDate;
        List<Coverage> coverage;
        String recordStatusCode;

        //  Hard coded data
        List<PharmacyNpiValidation> pharmacyNpiValidations;
        Integer processingWindowTimeFrame;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    static class PharmacyNpiValidation {

        private ClaimOrigination claimOrigination;
        private LocalDate fromDate;
        private LocalDate thruDate;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static class Coverage {

        Integer coverageId;
        String planCode;
        String coverageName;
        LocalDate coverageEffectiveDate;
        String distributionChannelName;
        String coverageIndicatorDescription;
        LocalDate coverageEndDate;
        Instant systemBeginDate;
        Instant systemEndDate;
        DaysSupply daysSupply;
        List<CoverageTier> coverageTier;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static class CoverageTier {
        Integer coverageTierId;
        String planCoverageTierDescription;
        Integer planCoverageTierNumber;
        LocalDate coverageTierEffectiveDate;
        LocalDate coverageTierEndDate;
        Instant systemBeginDate;
        Instant systemEndDate;
        List<CoverageTierCostShareStep> coverageTierCostShareStep;
    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static class DaysSupply {
        Integer daysSupplyId;
        LocalDate daysSupplyEffectiveDate;
        LocalDate daysSupplyEndDate;
        Double minimumDaysSupply;
        Double maximumDaysSupply;
        Instant systemBeginDate;
        Instant systemEndDate;
    }

    enum ClaimOrigination {
        ELECTRONIC,
        MANUAL,
        BATCH
    }
}
