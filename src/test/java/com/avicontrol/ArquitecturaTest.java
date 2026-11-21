package com.avicontrol;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArquitecturaTest {
  private final JavaClasses classes = new ClassFileImporter().importPackages("com.avicontrol");

  @Test
  void dominioNoDependeDeFrameworksNiOtrasCapas() {
    noClasses()
        .that()
        .resideInAPackage("com.avicontrol.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..", "jakarta.persistence..",
            "com.avicontrol.application..", "com.avicontrol.infrastructure..")
        .check(classes);
  }

  @Test
  void aplicacionNoDependeDeInfraestructura() {
    noClasses()
        .that()
        .resideInAPackage("com.avicontrol.application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.avicontrol.infrastructure..")
        .check(classes);
  }
}
