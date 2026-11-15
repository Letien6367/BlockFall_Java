package tetris.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

public final class ArcadeButton extends JButton {
    private static final Color NORMAL_COLOR = new Color(38, 73, 118);
    private static final Color HOVER_COLOR = new Color(54, 105, 163);
    private static final Color PRESSED_COLOR = new Color(28, 54, 91);
    private static final Color BORDER_COLOR = new Color(93, 136, 190);

    public ArcadeButton(String text, Runnable action) {
        super(text);
        addActionListener(event -> action.run());
        setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        setForeground(new Color(239, 244, 255));
        setBackground(NORMAL_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(8, 16, 8, 16)));
        setFocusPainted(false);
        setOpaque(true);
        setContentAreaFilled(true);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getModel().addChangeListener(event -> refreshBackground());
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                refreshBackground();
            }

            @Override
            public void focusLost(FocusEvent event) {
                refreshBackground();
            }
        });
    }

    private void refreshBackground() {
        if (getModel().isPressed()) {
            setBackground(PRESSED_COLOR);
        } else if (getModel().isRollover() || hasFocus()) {
            setBackground(HOVER_COLOR);
        } else {
            setBackground(NORMAL_COLOR);
        }
    }
}
