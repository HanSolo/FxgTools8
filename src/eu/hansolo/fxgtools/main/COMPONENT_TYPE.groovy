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
 * Date  : 19.09.11
 * Time  : 12:27
 */
enum COMPONENT_TYPE {
    JCOMPONENT("import javax.swing.JComponent;", "JComponent"),
    JPANEL("import javax.swing.JPanel;", "JPanel"),
    TOPCOMPONENT("import org.openide.windows.TopComponent;\nimport org.openide.util.NbBundle;\nimport org.netbeans.api.settings.ConvertAsProperties;\nimport org.openide.awt.ActionID;\nimport org.openide.awt.ActionReference;\n\n@TopComponent.Description(preferredID = \"\$className\", persistenceType = TopComponent.PERSISTENCE_ALWAYS)\n@TopComponent.Registration(mode = \"editor\", openAtStartup = true)\n@ActionID(category = \"Window\", id = \"\$className\")\n@ActionReference(path = \"Menu/Window\")\n@TopComponent.OpenActionRegistration(displayName = \"\$className\", preferredID = \"\$className\")", "TopComponent");

    String IMPORT_STATEMENT
    String CODE

    private COMPONENT_TYPE(final String IMPORT_STATEMENT, final String CODE) {
        this.IMPORT_STATEMENT = IMPORT_STATEMENT;
        this.CODE = CODE;
    }
}
