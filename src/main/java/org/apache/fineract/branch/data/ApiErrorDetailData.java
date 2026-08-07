/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorDetailData {
    private String field;
    private String code;
    private String message;
    private Object rejectedValue;
}
