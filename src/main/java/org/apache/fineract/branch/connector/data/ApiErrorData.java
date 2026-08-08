/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.connector.data;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorData {
    private OffsetDateTime timestamp;
    private int status;
    private String code;
    private String message;
    private String traceId;
    private List<ApiErrorDetailData> details;
}
