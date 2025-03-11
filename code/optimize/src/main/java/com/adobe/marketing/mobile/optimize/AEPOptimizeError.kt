/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.optimize

import com.adobe.marketing.mobile.AdobeError

/**
 * AEPOptimizeError class is used to create AEPOptimizeError from error details received from Experience Edge.
 * @param type The type of error that occurred.
 * @param status The HTTP status code of the error.
 * @param title The title of the error.
 * @param detail The details of the error.
 * @param report The report of the error.
 * @param adobeError The corresponding AdobeError.
 */

data class AEPOptimizeError(
    val type: String? = "",
    val status: Int? = 0,
    val title: String? = "",
    val detail: String? = "",
    var report: Map<String, Any>?,
    var adobeError: AdobeError?
) {

    fun toEventData(): Map<String, Any?> = mapOf(
        TYPE to type,
        STATUS to status,
        TITLE to title,
        DETAIL to detail,
        REPORT to report,
        ADOBE_ERROR to adobeError?.toEventData()
    )

    companion object {
        const val TYPE = "type"
        const val STATUS = "status"
        const val TITLE = "title"
        const val DETAIL = "detail"
        const val REPORT = "report"
        const val ADOBE_ERROR = "adobeError"
        const val ERROR_NAME = "errorName"
        const val ERROR_CODE = "errorCode"

        private val serverErrors = listOf(
            OptimizeConstants.HTTPResponseCodes.tooManyRequests,
            OptimizeConstants.HTTPResponseCodes.internalServerError,
            OptimizeConstants.HTTPResponseCodes.serviceUnavailable
        )

        private val networkErrors = listOf(
            OptimizeConstants.HTTPResponseCodes.badGateway,
            OptimizeConstants.HTTPResponseCodes.gatewayTimeout
        )

        fun AdobeError.toEventData(): Map<String, Any?> = mapOf(
            ERROR_NAME to errorName,
            ERROR_CODE to errorCode,
        )

        @JvmStatic
        fun toAEPOptimizeError(data: Map<String, Any?>): AEPOptimizeError {
            return AEPOptimizeError(
                type = data[TYPE] as? String ?: "",
                status = data[STATUS] as? Int ?: 0,
                title = data[TITLE] as? String ?: "",
                detail = data[DETAIL] as? String ?: "",
                report = data[REPORT] as? Map<String, Any>,
                adobeError = toAdobeError(data[ADOBE_ERROR] as Map<String, Any?>)
            )
        }

        @JvmStatic
        fun toAdobeError(data: Map<String, Any?>): AdobeError {
            return getAdobeErrorFromStatus(data[STATUS] as Int?)
        }

        @JvmStatic
        fun getTimeoutError(): AEPOptimizeError {
            return AEPOptimizeError(
                null,
                OptimizeConstants.ErrorData.Timeout.STATUS,
                OptimizeConstants.ErrorData.Timeout.TITLE,
                OptimizeConstants.ErrorData.Timeout.DETAIL,
                null,
                AdobeError.CALLBACK_TIMEOUT
            )
        }

        fun getUnexpectedError(): AEPOptimizeError {
            return AEPOptimizeError(
                null,
                null,
                OptimizeConstants.ErrorData.Unexpected.TITLE,
                OptimizeConstants.ErrorData.Unexpected.DETAIL,
                null,
                AdobeError.UNEXPECTED_ERROR
            )
        }

        fun getInvalidRequestError(): AEPOptimizeError {
            return AEPOptimizeError(
                null,
                OptimizeConstants.ErrorData.InvalidRequest.STATUS,
                OptimizeConstants.ErrorData.InvalidRequest.TITLE,
                OptimizeConstants.ErrorData.InvalidRequest.DETAIL,
                null,
                AdobeError.INVALID_REQUEST
            )
        }

        private fun getAdobeErrorFromStatus(status: Int?): AdobeError = when {
            status == OptimizeConstants.HTTPResponseCodes.clientTimeout -> AdobeError.CALLBACK_TIMEOUT
            serverErrors.contains(status) -> AdobeError.SERVER_ERROR
            networkErrors.contains(status) -> AdobeError.NETWORK_ERROR
            else -> AdobeError.UNEXPECTED_ERROR
        }
    }

    init {
        if (adobeError == null) {
            adobeError = getAdobeErrorFromStatus(status)
        }
    }
}
