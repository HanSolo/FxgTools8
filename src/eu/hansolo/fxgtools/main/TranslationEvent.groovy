/*
 * Copyright (c) 2012 Gerrit Grunwald
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package eu.hansolo.fxgtools.main

/**
 * Author: hansolo
 * Date  : 07.09.11
 * Time  : 12:31
 */
class TranslationEvent extends EventObject{
    private static final long serialVersionUID       = 1L;
    private final             TranslationState STATE

  public TranslationEvent(final Object SOURCE, final TranslationState STATE)
  {
    super(SOURCE)
    this.STATE = STATE
  }

  public TranslationState getState()
  {
    return STATE
  }
}
