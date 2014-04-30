package geneious.plugin;

import com.biomatters.geneious.publicapi.documents.XMLSerializationException;
import com.biomatters.geneious.publicapi.plugin.Options;
import org.jdom.Element;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * Example password option that does not store the value in preferences.  This lets us avoid making the decision between:
 * <ol>
 *     <li>Storing an obfuscated password that can easily be decrypted.</li>
 *     <li>Storing a plain text password so people know things are not secure.</li>
 *     <li>Trying to integrate with an external password store that requires unlocking i.e. Mac OS X keychain.</li>
 * </ol>
 *
 * @author Matthew Cheung
 * @version $Id$
 *          <p/>
 *          Created on 5/12/13 12:53 PM
 */
//public class PasswordOption extends Options.Option<String, JPasswordField> {
public class PasswordOption extends Options.Option<String, JPasswordField> {

    private String label;
    private boolean updatingComponent;

    protected PasswordOption(Element element) throws XMLSerializationException {
        // For deserialization
        super(element);
        setRestorePreferenceApplies(false);
    }

    protected PasswordOption(String optionName, String label) {
        super(optionName, label, "");
        this.label = label;
        setRestorePreferenceApplies(false);
    }

    @Override
    public String getValueFromString(String s) {
        return s;
    }

    @Override
    protected void setValueOnComponent(JPasswordField jPasswordField, String s) {
        if(updatingComponent) {
            return;    // Otherwise we'll end up looping forever between updating the component and the value stored in the option
        }
        updatingComponent = true;
        jPasswordField.setText(s);
        updatingComponent = false;
    }

    protected Box getContainer() {

        Box box = new Box(BoxLayout.X_AXIS);
        box.add(new JLabel(label));
        box.add(createComponent());
        box.setAlignmentX(JDialog.LEFT_ALIGNMENT);

        return box;
    }
    @Override
    protected JPasswordField createComponent() {
        final JPasswordField passwordField = new JPasswordField();
        passwordField.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                if (updatingComponent) {
                    return;    // Otherwise we'll end up looping forever between updating the component and the value stored in the option
                }
                updatingComponent = true;
                setValue(new String(passwordField.getPassword()));
                updatingComponent = false;
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }
        });
        Dimension oldPrefSize = passwordField.getPreferredSize();

        passwordField.setPreferredSize(new Dimension(120, oldPrefSize.height));
        passwordField.setMinimumSize(passwordField.getPreferredSize());  // Because the normal minimum is a sliver of nothing
        return passwordField;
    }
}
