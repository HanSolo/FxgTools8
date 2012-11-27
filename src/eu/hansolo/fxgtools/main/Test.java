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
