/*
 * Copyright 2021. gudaoxuri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package idealworld.dew.framework.exception;

import com.ecfront.dew.common.exception.RTException;

/**
 * 错误基础类.
 *
 * @author gudaoxuri
 */
public abstract class DewException extends RTException {

    public abstract Integer getCode();

    public DewException(String message) {
        super(message);
    }

    public DewException(String message, Throwable cause) {
        super(message, cause);
    }

    public DewException(Throwable cause) {
        super(cause);
    }

    public DewException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
