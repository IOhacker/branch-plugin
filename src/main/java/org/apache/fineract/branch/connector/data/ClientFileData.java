/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.connector.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientFileData {
    private Long clientId;
    private String accountNo;
    private String curp;
    private String displayName;
    private String status;
    private Long officeId;
    private String officeName;
    private String mobileNo;
    private String emailAddress;
}
