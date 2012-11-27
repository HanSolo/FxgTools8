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

//fxgFile = '/Volumes/Macintosh HD/Users/hansolo/Desktop/InSync/Java Apps/FXG Converter/fxg files/ForGerrit.fxg'
//fxg = new XmlParser().parse(new File(fxgFile))
selectedLayer = 'Layer_1'
keepAspect = true
width = 640
height = 640

//parser = new FxgParser()
//Map<String, BufferedImage> allLayerImages = parser.parse(fxgFile, width, height, keepAspect)

//Map<String, List<FxgElement>> layerMap = parser.getElements(fxg)
//translator = new FxgTranslator()

//translator.translate("Test.java", layerMap, Language.JAVA, String.valueOf((int)parser.originalWidth), String.valueOf((int)parser.originalHeight), true)

String replaced = "FOREGROUND_1_RR6_0_INDICATOR_FRAME_2_2.setStroke(null);"
//def matcher = (replaced =~ /_?RR[0-9]+_([0-9]+_)?/)
//replaced = matcher.replaceAll("_")
replaced = replaced.replaceAll("_?RR[0-9]+_([0-9]+_)?", '_')
System.out.println replaced

Double d = 5.0

System.out.println(d.is(Double))

/*
JFrame frame = new JFrame();
frame.setTitle("Groovy FXG-Parser")
frame.setDefaultCloseOperation(EXIT_ON_CLOSE)
JPanel panel = new JPanel() {
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g)
        Graphics2D g2 = (Graphics2D) g.create()

        for (BufferedImage image : allLayerImages.values()) {
            g2.drawImage(image, 0, 0, null)
        }

        //g2.drawImage(singleLayerImage, 0, 0, null)
        g2.dispose()
    }
}

frame.add(panel)
frame.setSize(width, height + 22)
frame.setLocationRelativeTo(null)
frame.setVisible(true)
*/
