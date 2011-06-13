/*
 * This file is part of the Illarion Common Library.
 * 
 * The Illarion Common Library is free software: you can redistribute i and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * The Illarion Common Library is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * the Illarion Common Library. If not, see <http://www.gnu.org/licenses/>.
 */
package illarion.common.config.gui.entries.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import illarion.common.config.entries.ConfigEntry;
import illarion.common.config.entries.TextEntry;

/**
 * This is a special implementation for the text area that is initialized with a
 * configuration entry. Its sole purpose is the use along with the configuration
 * system.
 * 
 * @author Martin Karing
 * @since 1.22
 * @version 1.22
 */
public final class TextEntrySwt implements SaveableEntrySwt {
    /**
     * The text entry used to initialize this instance.
     */
    private final TextEntry entry;

    /**
     * The SWT widget used to display this entry.
     */
    private final Text widget;

    /**
     * Create a instance of this text entry and set the configuration entry that
     * is used to setup this class.
     * 
     * @param usedEntry the entry used to setup this class, the entry needs to
     *            pass the check with the static method
     * @param parentWidget the widget this widget is added to
     */
    @SuppressWarnings("nls")
    public TextEntrySwt(final ConfigEntry usedEntry,
        final Composite parentWidget) {
        if (!isUsableEntry(usedEntry)) {
            throw new IllegalArgumentException("ConfigEntry type illegal.");
        }

        widget = new Text(parentWidget, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        entry = (TextEntry) usedEntry;
        widget.setText(entry.getValue());
    }

    /**
     * Text a entry if it is usable with this class or not.
     * 
     * @param entry the entry to test
     * @return <code>true</code> in case this entry is usable with this class
     */
    public static boolean isUsableEntry(final ConfigEntry entry) {
        return (entry instanceof TextEntry);
    }

    /**
     * Save the value in this text entry to the configuration.
     */
    @Override
    public void save() {
        entry.setValue(widget.getText());
    }

    /**
     * Set the layout data for this widget.
     */
    @Override
    public void setLayoutData(final Object data) {
        widget.setLayoutData(data);
    }

}
