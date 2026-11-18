package tetris.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public final class MenuPanel extends JPanel {
    private final JLabel bestScoreLabel = new JLabel("Điểm cao nhất: 0", SwingConstants.CENTER);
    private final JLabel bestLevelLabel = new JLabel("Cấp cao nhất: 1", SwingConstants.CENTER);

    public MenuPanel(Runnable onPlay, Runnable onExit) {
        super(new GridBagLayout());
        Objects.requireNonNull(onPlay, "onPlay");
        Objects.requireNonNull(onExit, "onExit");

        setBackground(new Color(14, 20, 40));

        JPanel menuContent = new JPanel();
        menuContent.setLayout(new BoxLayout(menuContent, BoxLayout.Y_AXIS));
        menuContent.setOpaque(false);

        JLabel title = new JLabel("BLOCKFALL", SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(Color.WHITE);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 36));

        bestScoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        bestScoreLabel.setForeground(new Color(157, 174, 207));
        bestScoreLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        bestLevelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        bestLevelLabel.setForeground(new Color(157, 174, 207));
        bestLevelLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));

        ArcadeButton playButton = new ArcadeButton("Chơi", onPlay);
        ArcadeButton exitButton = new ArcadeButton("Thoát", onExit);

        JPanel buttons = new JPanel(new GridLayout(2, 1, 0, 12));
        buttons.setOpaque(false);
        buttons.setPreferredSize(new Dimension(220, 110));
        buttons.add(playButton);
        buttons.add(exitButton);

        menuContent.add(title);
        menuContent.add(Box.createVerticalStrut(32));
        menuContent.add(bestScoreLabel);
        menuContent.add(bestLevelLabel);
        menuContent.add(Box.createVerticalStrut(24));
        menuContent.add(buttons);
        add(menuContent);
    }

    public void setBestScore(long bestScore) {
        bestScoreLabel.setText("Điểm cao nhất: " + bestScore);
    }

    public void setBestLevel(int bestLevel) {
        bestLevelLabel.setText("Cấp cao nhất: " + bestLevel);
    }

    public void setRecords(long bestScore, int bestLevel) {
        setBestScore(bestScore);
        setBestLevel(bestLevel);
    }
}
