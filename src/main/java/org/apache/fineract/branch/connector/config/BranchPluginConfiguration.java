/**
 * Copyright 2026   Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.branch.connector.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Spring configuration entry-point for the   loan-repayment plugin.
 * Drop the built JAR into Fineract's plugin directory; this configuration is
 * picked up automatically via component scan / SPI.
 */
@Configuration
@ComponentScan(basePackages = "org.apache.fineract.branch")
@EntityScan(basePackages = "org.apache.fineract.branch.connector.domain")
@EnableJpaRepositories(basePackages = "org.apache.fineract.branch.connector.domain")
public class BranchPluginConfiguration {
}
