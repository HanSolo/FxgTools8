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

package eu.hansolo.fxgtools.main;

import eu.hansolo.fxgtools.fxg.Language;


/**
 * Created by
 * User: hansolo
 * Date: 25.11.12
 * Time: 07:40
 */
public class Test {
    public static void main(String[] args) {
        FxgParser parser = new FxgParser();
        FxgTranslator translator = new FxgTranslator();
        translator.translate("CheTest", parser.getElements("/Users/hansolo/Dropbox/Java Apps/FXG Converter/fxg files/Che.fxg"), Language.JAVAFX, "400", "400", true);
    }
}
