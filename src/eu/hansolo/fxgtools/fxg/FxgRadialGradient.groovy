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

package eu.hansolo.fxgtools.fxg

import javafx.scene.paint.CycleMethod
import javafx.scene.paint.RadialGradient
import javafx.scene.paint.Stop

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 28.08.11
 * Time: 08:04
 * To change this template use File | Settings | File Templates.
 */
class FxgRadialGradient extends FxgFill {
    FxgFillType type   = FxgFillType.RADIAL_GRADIENT
    double      centerX
    double      centerY
    double      radius
    List<Stop>  stops

    RadialGradient getRadialGradient() {
        return new RadialGradient(0, 0, centerX, centerY, radius, false, CycleMethod.NO_CYCLE, stops)
    }
}
