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

import javafx.geometry.Point2D
import javafx.scene.paint.Color

/**
 * Created by IntelliJ IDEA.
 * User: hansolo
 * Date: 29.08.11
 * Time: 13:40
 */
class FxgShadow extends FxgFilter{
    FxgFilterType type     = FxgFilterType.SHADOW
    boolean       inner
    int           angle
    int           distance
    double        alpha
    double        alphaDouble
    double        blurX
    double        blurY
    Color         color

    Point2D getOffset() {
        return new Point2D(distance * Math.cos(Math.toRadians(-angle)), distance * Math.sin(Math.toRadians(angle)))
    }
}
