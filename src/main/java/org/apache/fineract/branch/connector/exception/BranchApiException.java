/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.branch.connector.exception;

import java.util.List;
import lombok.Getter;
import org.apache.fineract.branch.connector.data.ApiErrorDetailData;

@Getter
public class BranchApiException extends RuntimeException {

    private final int httpStatus;
    private final String code;
    private final List<ApiErrorDetailData> details;

    public BranchApiException(int httpStatus, String code, String message) {
        this(httpStatus, code, message, null);
    }

    public BranchApiException(int httpStatus, String code, String message, List<ApiErrorDetailData> details) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
        this.details = details;
    }

    public static BranchApiException notFound(String code, String message) {
        return new BranchApiException(404, code, message);
    }

    public static BranchApiException badRequest(String code, String message) {
        return new BranchApiException(400, code, message);
    }

    public static BranchApiException conflict(String code, String message) {
        return new BranchApiException(409, code, message);
    }

    public static BranchApiException unprocessable(String code, String message) {
        return new BranchApiException(422, code, message);
    }

    public static BranchApiException forbidden(String code, String message) {
        return new BranchApiException(403, code, message);
    }
}
