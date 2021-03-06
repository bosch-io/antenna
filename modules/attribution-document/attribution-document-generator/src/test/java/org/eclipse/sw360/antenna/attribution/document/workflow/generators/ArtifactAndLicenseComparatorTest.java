/**
 * Copyright (c) Robert Bosch Manufacturing Solutions GmbH 2019.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.attribution.document.workflow.generators;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArtifactAndLicenseComparatorTest {

   ArtifactAndLicenseComparator comp = new ArtifactAndLicenseComparator();

   @Test
   void same() {
      TestArtifactAndLicense a1 = new TestArtifactAndLicense("i am a teapot", "com.bosch:foo:23");

      int compare = comp.compare(a1, a1);
      assertThat(compare).isEqualTo(0);
   }

   @Test
   void compareFilename() {
      TestArtifactAndLicense a1 = new TestArtifactAndLicense("i am a teapot", "com.bosch:foo:23");
      TestArtifactAndLicense a2 = new TestArtifactAndLicense("i am not a teapot", "dom.bosch:foo:23");

      int compare = comp.compare(a1, a2);
      assertThat(compare).isLessThan(0);
      compare = comp.compare(a2, a1);
      assertThat(compare).isGreaterThan(0);
   }

   @Test
   void compareCoordinates() {
      TestArtifactAndLicense a1 = new TestArtifactAndLicense("i am a teapot", "com.bosch:foo:23");
      TestArtifactAndLicense a2 = new TestArtifactAndLicense("i am a teapot", "dom.bosch:foo:23");

      int compare = comp.compare(a1, a2);
      assertThat(compare).isLessThan(0);
      compare = comp.compare(a2, a1);
      assertThat(compare).isGreaterThan(0);
   }
}
