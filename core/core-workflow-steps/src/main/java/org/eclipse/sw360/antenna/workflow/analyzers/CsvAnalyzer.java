/*
 * Copyright (c) Bosch Software Innovations GmbH 2016-2017,2019.
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.antenna.workflow.analyzers;

import org.eclipse.sw360.antenna.api.workflow.ManualAnalyzer;
import org.eclipse.sw360.antenna.api.workflow.WorkflowStepResult;
import org.eclipse.sw360.antenna.csvreader.CSVReader;
import org.eclipse.sw360.antenna.model.artifact.Artifact;

import java.util.Collection;
import java.util.Map;

public class CsvAnalyzer extends ManualAnalyzer {

    private static final String DELIMITER = "delimiter";
    private Character rowDelimiter = ',';

    public CsvAnalyzer() {
        this.workflowStepOrder = 500;
    }

    @Override
    public WorkflowStepResult yield() {
        Collection<Artifact> artifacts = new CSVReader(
                componentInfoFile.toPath(),
                context.getToolConfiguration().getEncoding(),
                rowDelimiter,
                baseDir).createArtifactsList();

        return new WorkflowStepResult(artifacts, true);
    }

    @Override
    public void configure(Map<String, String> configMap) {
        super.configure(configMap);
        if(configMap.containsKey(DELIMITER))  {
            this.rowDelimiter = getConfigValue(DELIMITER, configMap).charAt(0);
        }
    }

    @Override
    public String getName() {
        return "CSV";
    }
}
