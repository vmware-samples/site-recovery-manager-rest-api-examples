/* Copyright 2023 VMware, Inc. */
/* SPDX-License-Identifier: BSD-2-Clause */
package dr.restapi.examples.vsphere.replication.exceptions;

import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;

/**
 * Runtime exception when VMware ecosystem with servers does not meet some code example expectation.
 */
public class EnvironmentPrerequisiteException extends RuntimeException {
   private static final String CONSIDER_AND_RERUN = " Consider fulfilling the prerequisite and rerun.";

   /**
    * Constructor.
    * @param message message
    * @param messageParams message parameters
    */
   public EnvironmentPrerequisiteException(String message, Object... messageParams) {
      super(StringUtils.isNotBlank(message) ?
            MessageFormat.format(message, messageParams) + CONSIDER_AND_RERUN :
            message);
   }

   /**
    * Constructor.
    * @param cause exception's cause
    * @param message message
    * @param messageParams message parameters
    */
   public EnvironmentPrerequisiteException(Throwable cause, String message, Object... messageParams) {
      super(StringUtils.isNotBlank(message) ?
            MessageFormat.format(message, messageParams) + CONSIDER_AND_RERUN :
            message,
            cause);
   }

   /**
    * Constructor.
    * @param cause exception's cause
    */
   public EnvironmentPrerequisiteException(Throwable cause) {
      super(cause);
   }
}
