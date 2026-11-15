package tetris.ui;

import java.awt.Component;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public final class MenuPanelTest {
    private MenuPanelTest() {
    }

    public static void main(String[] args) throws Exception {
        AtomicInteger playCount = new AtomicInteger();
        AtomicInteger exitCount = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                MenuPanel panel = new MenuPanel(playCount::incrementAndGet, exitCount::incrementAndGet);
                panel.setBestScore(1234);

                ArcadeButton playButton = findButton(panel, "Play");
                ArcadeButton exitButton = findButton(panel, "Exit");
                playButton.doClick();
                exitButton.doClick();
                if (playCount.get() != 1 || exitCount.get() != 1) {
                    throw new AssertionError("Menu actions should invoke their callbacks");
                }
                if (!findLabel(panel, "BEST SCORE: 1234")) {
                    throw new AssertionError("Menu should display the current high score");
                }

                java.awt.Color normalColor = playButton.getBackground();
                playButton.getModel().setRollover(true);
                if (playButton.getBackground().equals(normalColor)) {
                    throw new AssertionError("Arcade button should change color on hover");
                }
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        if (failure.get() != null) {
            throw new AssertionError("Menu panel checks failed", failure.get());
        }
        System.out.println("Menu panel checks passed.");
    }

    private static ArcadeButton findButton(java.awt.Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof ArcadeButton button && text.equals(button.getText())) {
                return button;
            }
            if (component instanceof java.awt.Container child) {
                ArcadeButton button = findButton(child, text);
                if (button != null) {
                    return button;
                }
            }
        }
        return null;
    }

    private static boolean findLabel(java.awt.Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label && text.equals(label.getText())) {
                return true;
            }
            if (component instanceof java.awt.Container child && findLabel(child, text)) {
                return true;
            }
        }
        return false;
    }
}
