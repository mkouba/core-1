/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.addons;

import java.util.Collections;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.addons.facets.AddonAPIFacet;
import org.jboss.forge.addon.addons.facets.AddonImplFacet;
import org.jboss.forge.addon.addons.facets.AddonParentFacet;
import org.jboss.forge.addon.addons.facets.AddonSPIFacet;
import org.jboss.forge.addon.addons.facets.AddonTestFacet;
import org.jboss.forge.addon.addons.facets.DefaultFurnaceContainerAPIFacet;
import org.jboss.forge.addon.addons.facets.DefaultFurnaceContainerFacet;
import org.jboss.forge.addon.addons.facets.FurnaceAPIFacet;
import org.jboss.forge.addon.addons.facets.FurnacePluginFacet;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.javaee.facets.CDIFacet;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.parser.java.facets.JavaCompilerFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.versions.SingleVersion;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class AddonProjectConfiguratorTest
{
   private static final String FURNACE_VERSION = "2.0.0-SNAPSHOT";

   @Deployment
   @Dependencies({
            @AddonDependency(name = "org.jboss.forge.furnace.container:cdi"),
            @AddonDependency(name = "org.jboss.forge.addon:addons")
   })
   public static ForgeArchive getDeployment()
   {
      return ShrinkWrap.create(ForgeArchive.class).
               addBeansXML().
               addAsAddonDependencies(
                        AddonDependencyEntry.create("org.jboss.forge.addon:addons"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:maven"),
                        AddonDependencyEntry.create("org.jboss.forge.addon:javaee"),
                        AddonDependencyEntry.create("org.jboss.forge.furnace.container:cdi")
               );
   }

   @Inject
   private AddonProjectConfigurator configurator;

   @Inject
   private ProjectFactory projectFactory;

   @Test
   public void testCreateAddonProject()
   {
      Project project = projectFactory.createTempProject();
      project.getProjectRoot().deleteOnExit();

      MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
      metadataFacet.setProjectName("testproject");
      metadataFacet.setProjectVersion("1.0.0-SNAPSHOT");
      metadataFacet.setTopLevelPackage("com.acme.testproject");

      SingleVersion forgeVersion = new SingleVersion(FURNACE_VERSION);
      configurator.setupComplexAddonProject(project, forgeVersion, Collections.<AddonId> emptyList());
      Assert.assertTrue(project.hasFacet(AddonParentFacet.class));
      Assert.assertTrue(project.hasFacet(JavaCompilerFacet.class));
      Assert.assertFalse(project.hasFacet(JavaSourceFacet.class));
      DirectoryResource projectRoot = project.getProjectRoot();

      Assert.assertTrue("ADDON module is missing", projectRoot.getChild("addon").exists());
      Assert.assertTrue("API module is missing", projectRoot.getChild("api").exists());
      Assert.assertTrue("IMPL module is missing", projectRoot.getChild("impl").exists());
      Assert.assertTrue("SPI module is missing", projectRoot.getChild("spi").exists());
      Assert.assertTrue("TESTS module is missing", projectRoot.getChild("tests").exists());

      Project addonProject = projectFactory.findProject(projectRoot.getChildDirectory("addon"));
      Project apiProject = projectFactory.findProject(projectRoot.getChildDirectory("api"));
      Project implProject = projectFactory.findProject(projectRoot.getChildDirectory("impl"));
      Project spiProject = projectFactory.findProject(projectRoot.getChildDirectory("spi"));
      Project testsProject = projectFactory.findProject(projectRoot.getChildDirectory("tests"));

      Assert.assertFalse(addonProject.hasFacet(JavaCompilerFacet.class));
      Assert.assertTrue(addonProject.hasFacet(JavaSourceFacet.class));

      Assert.assertFalse(apiProject.hasFacet(JavaCompilerFacet.class));
      Assert.assertTrue(apiProject.hasFacet(JavaSourceFacet.class));
      Assert.assertTrue(apiProject.hasFacet(CDIFacet.class));

      Assert.assertFalse(implProject.hasFacet(JavaCompilerFacet.class));
      Assert.assertTrue(implProject.hasFacet(JavaSourceFacet.class));
      Assert.assertTrue(implProject.hasFacet(CDIFacet.class));

      Assert.assertFalse(spiProject.hasFacet(JavaCompilerFacet.class));
      Assert.assertTrue(spiProject.hasFacet(JavaSourceFacet.class));
      Assert.assertTrue(spiProject.hasFacet(FurnaceAPIFacet.class));
      Assert.assertFalse(spiProject.hasFacet(CDIFacet.class));

      Assert.assertFalse(testsProject.hasFacet(JavaCompilerFacet.class));
      Assert.assertTrue(testsProject.hasFacet(JavaSourceFacet.class));
      Assert.assertFalse(testsProject.hasFacet(CDIFacet.class));

      Dependency addonDependency = DependencyBuilder.create(
               addonProject.getFacet(MetadataFacet.class).getOutputDependency())
               .setClassifier("forge-addon");
      Dependency apiDependency = apiProject.getFacet(MetadataFacet.class).getOutputDependency();
      Dependency implDependency = implProject.getFacet(MetadataFacet.class).getOutputDependency();
      Dependency spiDependency = DependencyBuilder.create(
               spiProject.getFacet(MetadataFacet.class).getOutputDependency())
               .setClassifier("forge-addon");

      /*
       * Verify parent project
       */
      Assert.assertNull(project.getFacet(MavenFacet.class).getPOM().getParent());

      Assert.assertTrue(project.getFacet(DependencyFacet.class).hasDirectManagedDependency(
               DefaultFurnaceContainerAPIFacet.FURNACE_CONTAINER_API_DEPENDENCY));
      Assert.assertTrue(project.getFacet(DependencyFacet.class).hasDirectManagedDependency(
               DependencyBuilder.create(addonProject.getFacet(MetadataFacet.class).getOutputDependency())
                        .setClassifier("forge-addon")));
      Assert.assertTrue(project.getFacet(DependencyFacet.class).hasDirectManagedDependency(apiDependency));
      Assert.assertTrue(project.getFacet(DependencyFacet.class).hasDirectManagedDependency(
               implProject.getFacet(MetadataFacet.class).getOutputDependency()));

      Assert.assertTrue(project.getFacet(DependencyFacet.class).hasDirectManagedDependency(spiDependency));

      /*
       * Verify impl/ sub-module
       */
      Assert.assertEquals("../pom.xml", implProject.getFacet(MavenFacet.class).getPOM().getParent().getRelativePath());
      Assert.assertTrue(implProject.hasFacet(DefaultFurnaceContainerAPIFacet.class));

      Assert.assertTrue(implProject.getFacet(DependencyFacet.class).hasDirectDependency(apiDependency));
      Assert.assertFalse(implProject.getFacet(DependencyFacet.class).hasDirectManagedDependency(apiDependency));
      Assert.assertTrue(implProject.getFacet(DependencyFacet.class).hasEffectiveManagedDependency(apiDependency));
      Assert.assertEquals("provided", implProject.getFacet(DependencyFacet.class).getDirectDependency(apiDependency)
               .getScopeType());

      Assert.assertTrue(implProject.getFacet(DependencyFacet.class).hasDirectDependency(spiDependency));
      Assert.assertFalse(implProject.getFacet(DependencyFacet.class).hasDirectManagedDependency(spiDependency));
      Assert.assertTrue(implProject.getFacet(DependencyFacet.class).hasEffectiveManagedDependency(spiDependency));
      Assert.assertEquals("provided", implProject.getFacet(DependencyFacet.class).getDirectDependency(spiDependency)
               .getScopeType());

      Assert.assertTrue(implProject.getFacet(DependencyFacet.class).getManagedDependencies().isEmpty());
      Assert.assertTrue(implProject.getFacet(DependencyFacet.class).hasDirectDependency(
               DefaultFurnaceContainerAPIFacet.FURNACE_CONTAINER_API_DEPENDENCY));
      Assert.assertFalse(implProject.getFacet(DependencyFacet.class).hasDirectManagedDependency(
               DefaultFurnaceContainerAPIFacet.FURNACE_CONTAINER_API_DEPENDENCY));
      Assert.assertTrue(implProject.getFacet(DependencyFacet.class).hasEffectiveManagedDependency(
               DefaultFurnaceContainerAPIFacet.FURNACE_CONTAINER_API_DEPENDENCY));
      Assert.assertTrue(implProject.hasFacet(AddonImplFacet.class));
      Assert.assertFalse(implProject.hasFacet(FurnacePluginFacet.class));

      /*
       * Verify api/ sub-module
       */
      Assert.assertEquals("../pom.xml", apiProject.getFacet(MavenFacet.class).getPOM().getParent().getRelativePath());
      Assert.assertTrue(apiProject.hasFacet(DefaultFurnaceContainerAPIFacet.class));

      Assert.assertTrue(apiProject.getFacet(DependencyFacet.class).hasDirectDependency(spiDependency));
      Assert.assertFalse(apiProject.getFacet(DependencyFacet.class).hasDirectManagedDependency(spiDependency));
      Assert.assertTrue(apiProject.getFacet(DependencyFacet.class).hasEffectiveManagedDependency(spiDependency));
      Assert.assertEquals("provided", apiProject.getFacet(DependencyFacet.class).getDirectDependency(spiDependency)
               .getScopeType());

      Assert.assertTrue(apiProject.getFacet(DependencyFacet.class).getManagedDependencies().isEmpty());
      Assert.assertTrue(apiProject.getFacet(DependencyFacet.class).hasDirectDependency(
               DefaultFurnaceContainerAPIFacet.FURNACE_CONTAINER_API_DEPENDENCY));
      Assert.assertFalse(apiProject.getFacet(DependencyFacet.class).hasDirectManagedDependency(
               DefaultFurnaceContainerAPIFacet.FURNACE_CONTAINER_API_DEPENDENCY));
      Assert.assertTrue(apiProject.getFacet(DependencyFacet.class).hasEffectiveManagedDependency(
               DefaultFurnaceContainerAPIFacet.FURNACE_CONTAINER_API_DEPENDENCY));
      Assert.assertTrue(apiProject.hasFacet(AddonAPIFacet.class));
      Assert.assertFalse(apiProject.hasFacet(FurnacePluginFacet.class));

      /*
       * Verify spi/ sub-module
       */
      Assert.assertEquals("../pom.xml", spiProject.getFacet(MavenFacet.class).getPOM().getParent().getRelativePath());

      Assert.assertTrue(spiProject.getFacet(DependencyFacet.class).getManagedDependencies().isEmpty());
      Assert.assertTrue(spiProject.getFacet(DependencyFacet.class).hasDirectDependency(
               FurnaceAPIFacet.FURNACE_API_DEPENDENCY));
      Assert.assertFalse(spiProject.getFacet(DependencyFacet.class).hasDirectManagedDependency(
               FurnaceAPIFacet.FURNACE_API_DEPENDENCY));
      Assert.assertTrue(spiProject.getFacet(DependencyFacet.class).hasEffectiveManagedDependency(
               FurnaceAPIFacet.FURNACE_API_DEPENDENCY));
      Assert.assertFalse(spiProject.hasFacet(DefaultFurnaceContainerFacet.class));
      Assert.assertFalse(spiProject.hasFacet(DefaultFurnaceContainerAPIFacet.class));
      Assert.assertFalse(spiProject.hasFacet(CDIFacet.class));
      Assert.assertTrue(spiProject.hasFacet(AddonSPIFacet.class));
      Assert.assertTrue(spiProject.hasFacet(FurnacePluginFacet.class));

      /*
       * Verify addon/ sub-module
       */
      Assert.assertEquals("../pom.xml", addonProject.getFacet(MavenFacet.class).getPOM().getParent().getRelativePath());

      Assert.assertTrue(addonProject.getFacet(DependencyFacet.class).hasDirectDependency(apiDependency));
      Assert.assertFalse(addonProject.getFacet(DependencyFacet.class).hasDirectManagedDependency(apiDependency));
      Assert.assertTrue(addonProject.getFacet(DependencyFacet.class).hasEffectiveManagedDependency(apiDependency));
      Assert.assertNull(addonProject.getFacet(DependencyFacet.class).getDirectDependency(apiDependency)
               .getScopeType());

      Assert.assertTrue(addonProject.getFacet(DependencyFacet.class).hasDirectDependency(implDependency));
      Assert.assertFalse(addonProject.getFacet(DependencyFacet.class).hasDirectManagedDependency(implDependency));
      Assert.assertTrue(addonProject.getFacet(DependencyFacet.class).hasEffectiveManagedDependency(implDependency));
      Assert.assertEquals("runtime", addonProject.getFacet(DependencyFacet.class).getDirectDependency(implDependency)
               .getScopeType());

      Assert.assertTrue(addonProject.getFacet(DependencyFacet.class).hasDirectDependency(spiDependency));
      Assert.assertFalse(addonProject.getFacet(DependencyFacet.class).hasDirectManagedDependency(spiDependency));
      Assert.assertTrue(addonProject.getFacet(DependencyFacet.class).hasEffectiveManagedDependency(spiDependency));
      Assert.assertNull(addonProject.getFacet(DependencyFacet.class).getDirectDependency(spiDependency)
               .getScopeType());
      Assert.assertEquals("compile",
               addonProject.getFacet(DependencyFacet.class).getEffectiveDependency(spiDependency)
                        .getScopeType());

      Assert.assertTrue(addonProject.getFacet(DependencyFacet.class).getManagedDependencies().isEmpty());
      Assert.assertTrue(addonProject.getFacet(DependencyFacet.class).hasDirectDependency(
               DefaultFurnaceContainerFacet.FURNACE_CONTAINER_DEPENDENCY));
      Assert.assertFalse(addonProject.getFacet(DependencyFacet.class).hasDirectManagedDependency(
               DefaultFurnaceContainerFacet.FURNACE_CONTAINER_DEPENDENCY));
      Assert.assertTrue(addonProject.getFacet(DependencyFacet.class).hasEffectiveManagedDependency(
               DefaultFurnaceContainerFacet.FURNACE_CONTAINER_DEPENDENCY));

      Assert.assertTrue(addonProject.hasFacet(FurnacePluginFacet.class));

      
      /*
       * Verify tests/ sub-module
       */

      Assert.assertEquals("../pom.xml", testsProject.getFacet(MavenFacet.class).getPOM().getParent().getRelativePath());

      Assert.assertTrue(testsProject.getFacet(DependencyFacet.class).hasDirectDependency(addonDependency));
      Assert.assertFalse(testsProject.getFacet(DependencyFacet.class).hasDirectManagedDependency(addonDependency));
      Assert.assertTrue(testsProject.getFacet(DependencyFacet.class).hasEffectiveManagedDependency(addonDependency));
      Assert.assertNull(testsProject.getFacet(DependencyFacet.class).getDirectDependency(addonDependency)
               .getScopeType());
      Assert.assertNotNull("ADDON module is not present in the TESTS module",
               testsProject.getFacet(DependencyFacet.class).getEffectiveDependency(addonDependency));
      Assert.assertEquals("compile",
               testsProject.getFacet(DependencyFacet.class).getEffectiveDependency(addonDependency)
                        .getScopeType());

      Assert.assertFalse(testsProject.getFacet(DependencyFacet.class).hasDirectDependency(spiDependency));
      Assert.assertFalse(testsProject.getFacet(DependencyFacet.class).hasDirectManagedDependency(spiDependency));
      Assert.assertTrue(testsProject.getFacet(DependencyFacet.class).hasEffectiveManagedDependency(spiDependency));

      Assert.assertTrue(testsProject.getFacet(DependencyFacet.class).getManagedDependencies().isEmpty());
      Assert.assertTrue(testsProject.getFacet(DependencyFacet.class).hasDirectDependency(
               DefaultFurnaceContainerFacet.FURNACE_CONTAINER_DEPENDENCY));
      Assert.assertFalse(testsProject.getFacet(DependencyFacet.class).hasDirectManagedDependency(
               DefaultFurnaceContainerFacet.FURNACE_CONTAINER_DEPENDENCY));
      Assert.assertTrue(testsProject.getFacet(DependencyFacet.class).hasEffectiveManagedDependency(
               DefaultFurnaceContainerFacet.FURNACE_CONTAINER_DEPENDENCY));
      Assert.assertEquals(FURNACE_VERSION, testsProject.getFacet(DependencyFacet.class).getEffectiveDependency(
               DefaultFurnaceContainerFacet.FURNACE_CONTAINER_DEPENDENCY).getCoordinate().getVersion());

      project.getProjectRoot().delete(true);
      project.getProjectRoot().deleteOnExit();
   }

   @Test
   public void testSimpleAddonProject()
   {
      Project project = projectFactory.createTempProject();
      project.getProjectRoot().deleteOnExit();

      MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
      metadataFacet.setProjectName("testproject");
      metadataFacet.setProjectVersion("1.0.0-SNAPSHOT");
      metadataFacet.setTopLevelPackage("com.acme.testproject");

      SingleVersion forgeVersion = new SingleVersion(FURNACE_VERSION);
      configurator.setupSimpleAddonProject(project, forgeVersion, Collections.<AddonId> emptyList());

      Assert.assertTrue(project.hasFacet(DefaultFurnaceContainerFacet.class));
      Assert.assertTrue(project.hasFacet(FurnacePluginFacet.class));
      Assert.assertTrue(project.hasFacet(AddonTestFacet.class));
      Assert.assertTrue(project.hasFacet(JavaSourceFacet.class));
      Assert.assertTrue(project.hasFacet(JavaCompilerFacet.class));
      Assert.assertTrue(project.hasFacet(CDIFacet.class));

      Assert.assertFalse(project.getFacet(DependencyFacet.class).getManagedDependencies().isEmpty());

      /**
       * Verify test harness dependencies
       */
      Assert.assertTrue(project.getFacet(DependencyFacet.class).hasDirectDependency(
               AddonTestFacet.FURNACE_TEST_ADAPTER_DEPENDENCY));
      Assert.assertTrue(project.getFacet(DependencyFacet.class).hasDirectDependency(
               AddonTestFacet.FURNACE_TEST_HARNESS_DEPENDENCY));
      Assert.assertTrue(project.getFacet(DependencyFacet.class).hasDirectManagedDependency(
               AddonTestFacet.FURNACE_TEST_ADAPTER_DEPENDENCY));
      Assert.assertTrue(project.getFacet(DependencyFacet.class).hasDirectManagedDependency(
               AddonTestFacet.FURNACE_TEST_HARNESS_DEPENDENCY));
      Assert.assertTrue(project.getFacet(DependencyFacet.class).hasEffectiveDependency(
               DependencyBuilder.create("junit:junit").setScopeType("test")));

      /**
       * Verify container dependencies
       */
      Assert.assertTrue(project.getFacet(DependencyFacet.class).hasDirectDependency(
               DefaultFurnaceContainerFacet.FURNACE_CONTAINER_DEPENDENCY));
      Assert.assertNull(project.getFacet(DependencyFacet.class).getDirectDependency(
               DefaultFurnaceContainerFacet.FURNACE_CONTAINER_DEPENDENCY).getCoordinate().getVersion());
      Assert.assertTrue(project.getFacet(DependencyFacet.class).hasDirectManagedDependency(
               DefaultFurnaceContainerFacet.FURNACE_CONTAINER_DEPENDENCY));
      Assert.assertNotNull(project.getFacet(DependencyFacet.class).getDirectManagedDependency(
               DefaultFurnaceContainerFacet.FURNACE_CONTAINER_DEPENDENCY).getCoordinate().getVersion());
      Assert.assertTrue(project.getFacet(DependencyFacet.class).hasEffectiveDependency(
               DefaultFurnaceContainerAPIFacet.FURNACE_CONTAINER_API_DEPENDENCY));
      Assert.assertEquals(FURNACE_VERSION, project.getFacet(DependencyFacet.class).getEffectiveDependency(
               DefaultFurnaceContainerAPIFacet.FURNACE_CONTAINER_API_DEPENDENCY).getCoordinate().getVersion());
      Assert.assertFalse(project.getFacet(DependencyFacet.class).hasDirectManagedDependency(
               DefaultFurnaceContainerAPIFacet.FURNACE_CONTAINER_API_DEPENDENCY));
      Assert.assertFalse(project.getFacet(DependencyFacet.class).hasDirectDependency(
               DefaultFurnaceContainerAPIFacet.FURNACE_CONTAINER_API_DEPENDENCY));

      project.getProjectRoot().delete(true);
      project.getProjectRoot().deleteOnExit();
   }

   @Test
   @Ignore("FORGE-894")
   public void testDependencyResolution()
   {
      Project project = projectFactory.createTempProject();
      project.getProjectRoot().deleteOnExit();

      MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
      metadataFacet.setProjectName("testproject");
      metadataFacet.setProjectVersion("1.0.0-SNAPSHOT");
      metadataFacet.setTopLevelPackage("com.acme.testproject");

      SingleVersion forgeVersion = new SingleVersion("2.0.0.Alpha3");
      configurator.setupComplexAddonProject(project, forgeVersion, Collections.<AddonId> emptyList());

      DirectoryResource projectRoot = project.getProjectRoot();

      Assert.assertTrue("SPI module is missing", projectRoot.getChild("spi").exists());
      Assert.assertTrue("TESTS module is missing", projectRoot.getChild("tests").exists());

      Project spiProject = projectFactory.findProject(projectRoot.getChildDirectory("spi"));
      Project testsProject = projectFactory.findProject(projectRoot.getChildDirectory("tests"));

      Dependency spiDependency = DependencyBuilder.create(
               spiProject.getFacet(MetadataFacet.class).getOutputDependency())
               .setClassifier("forge-addon");
      Assert.assertNotNull("SPI module is not present in the TESTS module", testsProject
               .getFacet(DependencyFacet.class).getEffectiveDependency(spiDependency));
      Assert.assertEquals("compile",
               testsProject.getFacet(DependencyFacet.class).getEffectiveDependency(spiDependency)
                        .getScopeType());
   }
}