package com.minidoodle.scheduler;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture tests enforcing that GraphQL resolvers depend only on service
 * interfaces, never on repositories.
 */
class GraphqlArchitectureTest {

    private final JavaClasses graphqlClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.minidoodle.scheduler.graphql");

    @Test
    void graphqlResolversMustNotImportRepositories() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.minidoodle.scheduler.graphql..")
                .should().dependOnClassesThat().resideInAPackage("com.minidoodle.shared.persistence.repository..");

        rule.check(graphqlClasses);
    }
}