/* Copyright 2023 VMware, Inc. */
/* SPDX-License-Identifier: BSD-2-Clause */
package dr.restapi.examples.vsphere.replication.exceptions;

import dr.restapi.examples.apiclient.ApiException;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;

public class ExamplesExecutionException extends RuntimeException {
   private static final String RESPONSE_ERROR = " Response error code is [{0}]. Response body is [{1}].";

   /**
    * Constructor.
    * @param message message
    * @param messageParams message parameters
    */
   public ExamplesExecutionException(String message, Object... messageParams) {
      super(StringUtils.isNotBlank(message) ?
            MessageFormat.format(message, messageParams) :
            message);
   }

   /**
    * Constructor.
    * @param ex exception's cause
    * @param message message
    * @param messageParams message parameters
    */
   public ExamplesExecutionException(ApiException ex, String message, Object... messageParams) {
      super(StringUtils.isNotBlank(message) ?
            MessageFormat.format(message, messageParams) +
            MessageFormat.format(RESPONSE_ERROR, ex.getMessage(), ex.getResponseBody()) :
            message,
            ex);
   }

   /**
    * Constructor.
    * @param ex exception's cause
    */
   public ExamplesExecutionException(ApiException ex) {
      super(ex);
   }
}
