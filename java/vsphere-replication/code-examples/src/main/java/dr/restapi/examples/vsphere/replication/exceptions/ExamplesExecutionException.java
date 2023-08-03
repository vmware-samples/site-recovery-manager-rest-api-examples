/* Copyright 2023 VMware, Inc. */
/* SPDX-License-Identifier: BSD-2-Clause */
package dr.restapi.examples.vsphere.replication.exceptions;

import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;

public class ExamplesExecutionException extends RuntimeException {
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
    * @param cause exception's cause
    * @param message message
    * @param messageParams message parameters
    */
   public ExamplesExecutionException(Throwable cause, String message, Object... messageParams) {
      super(StringUtils.isNotBlank(message) ?
            MessageFormat.format(message, messageParams) :
            message,
            cause);
   }

   /**
    * Constructor.
    * @param cause exception's cause
    */
   public ExamplesExecutionException(Throwable cause) {
      super(cause);
   }
}
