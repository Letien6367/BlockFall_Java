package tetris.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import tetris.service.PlayerRecord;

public final class MenuPanel extends JPanel {
    private static final int MAX_PLAYERS_DISPLAYED = 10;
    private static final Color BACKGROUND_COLOR = new Color(14, 20, 40);
    private static final Color CARD_COLOR = new Color(24, 33, 59);
    private static final Color CARD_BORDER_COLOR = new Color(49, 64, 98);
    private static final Color PRIMARY_TEXT_COLOR = new Color(239, 244, 255);
    private static final Color SECONDARY_TEXT_COLOR = new Color(157, 174, 207);
    private static final Color ACCENT_COLOR = new Color(111, 169, 255);

    private final JTextField playerNameField = new JTextField(18);
    private final JPanel leaderboardEntries = new JPanel();
    private final JLabel persistenceStatus = new JLabel("Đang tải bảng xếp hạng...");
    private ArcadeButton playButton;
    private ArcadeButton historyButton;
    private boolean persistenceReady;
    private boolean busy;

    public MenuPanel(Runnable onPlay, Runnable onExit) {
        this(adaptPlayAction(onPlay), onExit);
    }

    public MenuPanel(Consumer<String> onPlay, Runnable onExit) {
        this(onPlay, onExit, null);
    }

    public MenuPanel(Consumer<String> onPlay, Runnable onExit, Runnable onHistory) {
        super(new BorderLayout(0, 24));
        Objects.requireNonNull(onPlay, "onPlay");
        Objects.requireNonNull(onExit, "onExit");

        setBackground(BACKGROUND_COLOR);
        setBorder(new EmptyBorder(28, 48, 34, 48));

        JPanel header = createHeader();
        CardPanel playerCard = createPlayerCard(onPlay, onExit, onHistory);
        CardPanel leaderboardCard = createLeaderboardCard();

        JPanel content = new JPanel(new GridBagLayout());
        content.setName("menu-content");
        content.setOpaque(false);
        GridBagConstraints playerConstraints = new GridBagConstraints();
        playerConstraints.gridx = 0;
        playerConstraints.gridy = 0;
        playerConstraints.weightx = 1;
        playerConstraints.weighty = 1;
        playerConstraints.anchor = GridBagConstraints.CENTER;
        playerConstraints.insets = new Insets(0, 0, 0, 24);
        content.add(playerCard, playerConstraints);

        GridBagConstraints leaderboardConstraints = new GridBagConstraints();
        leaderboardConstraints.gridx = 1;
        leaderboardConstraints.gridy = 0;
        leaderboardConstraints.anchor = GridBagConstraints.NORTHEAST;
        content.add(leaderboardCard, leaderboardConstraints);

        add(header, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        updatePersistenceControls();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel title = new JLabel("BLOCKFALL", SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(PRIMARY_TEXT_COLOR);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));

        JLabel subtitle = new JLabel("XẾP GẠCH • CHINH PHỤC BẢNG XẾP HẠNG",
                SwingConstants.CENTER);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(SECONDARY_TEXT_COLOR);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        return header;
    }

    private CardPanel createPlayerCard(Consumer<String> onPlay, Runnable onExit,
            Runnable onHistory) {
        CardPanel card = new CardPanel(new BorderLayout(),
                CARD_COLOR, CARD_BORDER_COLOR);
        card.setName("player-entry-card");
        card.setBorder(new EmptyBorder(24, 28, 24, 28));
        card.setPreferredSize(new Dimension(430, 410));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JLabel sectionTitle = new JLabel("SẴN SÀNG CHƠI?", SwingConstants.CENTER);
        sectionTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        sectionTitle.setForeground(ACCENT_COLOR);
        sectionTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        JLabel nameCaption = new JLabel("Tên người chơi", SwingConstants.LEFT);
        nameCaption.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameCaption.setForeground(PRIMARY_TEXT_COLOR);
        nameCaption.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));

        playerNameField.setName("player-name-field");
        playerNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        playerNameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        playerNameField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        playerNameField.setForeground(PRIMARY_TEXT_COLOR);
        playerNameField.setBackground(new Color(13, 20, 39));
        playerNameField.setCaretColor(ACCENT_COLOR);
        playerNameField.setSelectionColor(new Color(54, 91, 145));
        playerNameField.setHorizontalAlignment(SwingConstants.LEFT);
        playerNameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(68, 96, 143), 1),
                new EmptyBorder(10, 12, 10, 12)));
        playerNameField.setToolTipText("Nhập tên, tối đa "
                + PlayerRecord.MAX_NAME_LENGTH + " ký tự");

        JLabel nameHint = new JLabel("Tên cũ sẽ dùng lại hồ sơ và thành tích.",
                SwingConstants.LEFT);
        nameHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameHint.setForeground(SECONDARY_TEXT_COLOR);
        nameHint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        playButton = new ArcadeButton("Chơi",
                () -> onPlay.accept(playerNameField.getText()));
        playButton.setName("play-button");
        ArcadeButton exitButton = new ArcadeButton("Thoát", onExit);
        playButton.setPreferredSize(new Dimension(150, 48));
        exitButton.setPreferredSize(new Dimension(120, 48));
        if (onHistory != null) {
            historyButton = new ArcadeButton("Lịch sử ván", onHistory);
            historyButton.setName("history-button");
            historyButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            historyButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        }
        JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
        buttons.setOpaque(false);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.add(playButton);
        buttons.add(exitButton);

        content.add(sectionTitle);
        content.add(Box.createVerticalStrut(24));
        content.add(nameCaption);
        content.add(Box.createVerticalStrut(8));
        content.add(playerNameField);
        content.add(Box.createVerticalStrut(6));
        content.add(nameHint);
        if (historyButton != null) {
            content.add(Box.createVerticalStrut(12));
            content.add(historyButton);
        }
        persistenceStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        persistenceStatus.setForeground(SECONDARY_TEXT_COLOR);
        persistenceStatus.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        content.add(Box.createVerticalStrut(8));
        content.add(persistenceStatus);
        content.add(Box.createVerticalGlue());
        content.add(buttons);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private CardPanel createLeaderboardCard() {
        CardPanel card = new CardPanel(new BorderLayout(0, 12),
                CARD_COLOR, CARD_BORDER_COLOR);
        card.setName("leaderboard-card");
        card.setBorder(new EmptyBorder(20, 20, 18, 20));
        card.setPreferredSize(new Dimension(330, 410));

        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);

        JLabel title = new JLabel("BẢNG XẾP HẠNG", SwingConstants.LEFT);
        title.setForeground(PRIMARY_TEXT_COLOR);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));

        JLabel subtitle = new JLabel("TOP 10 NGƯỜI CHƠI", SwingConstants.LEFT);
        subtitle.setForeground(SECONDARY_TEXT_COLOR);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));

        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitle);

        leaderboardEntries.setName("leaderboard-entries");
        leaderboardEntries.setLayout(new BoxLayout(leaderboardEntries, BoxLayout.Y_AXIS));
        leaderboardEntries.setOpaque(false);
        setLeaderboard(List.of());

        card.add(heading, BorderLayout.NORTH);
        card.add(leaderboardEntries, BorderLayout.CENTER);
        return card;
    }

    public void setPlayerName(String playerName) {
        playerNameField.setText(playerName);
    }

    public void setLeaderboard(List<PlayerRecord> players) {
        Objects.requireNonNull(players, "players");
        leaderboardEntries.removeAll();
        int displayedPlayers = Math.min(players.size(), MAX_PLAYERS_DISPLAYED);
        if (displayedPlayers == 0) {
            JLabel emptyState = new JLabel("Chưa có người chơi", SwingConstants.CENTER);
            emptyState.setForeground(SECONDARY_TEXT_COLOR);
            emptyState.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 13));
            emptyState.setAlignmentX(Component.CENTER_ALIGNMENT);
            leaderboardEntries.add(Box.createVerticalStrut(16));
            leaderboardEntries.add(emptyState);
        } else {
            for (int index = 0; index < displayedPlayers; index++) {
                PlayerRecord player = Objects.requireNonNull(players.get(index), "player");
                leaderboardEntries.add(createLeaderboardRow(index + 1, player));
            }
        }
        leaderboardEntries.revalidate();
        leaderboardEntries.repaint();
    }

    public void setPersistenceReady(boolean ready, String status) {
        persistenceReady = ready;
        persistenceStatus.setText(status);
        persistenceStatus.setForeground(ready
                ? new Color(124, 220, 164) : new Color(255, 164, 145));
        updatePersistenceControls();
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
        updatePersistenceControls();
    }

    private void updatePersistenceControls() {
        if (playButton != null) {
            playButton.setEnabled(persistenceReady && !busy);
        }
        if (historyButton != null) {
            historyButton.setEnabled(persistenceReady && !busy);
        }
        playerNameField.setEnabled(persistenceReady && !busy);
    }

    private JPanel createLeaderboardRow(int rank, PlayerRecord player) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(3, 0, 3, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JLabel rankLabel = new JLabel("#" + rank, SwingConstants.CENTER);
        rankLabel.setName("leaderboard-rank-" + rank);
        rankLabel.setPreferredSize(new Dimension(36, 24));
        rankLabel.setOpaque(true);
        rankLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        if (rank == 1) {
            rankLabel.setBackground(new Color(246, 196, 83));
            rankLabel.setForeground(new Color(45, 37, 22));
        } else if (rank == 2) {
            rankLabel.setBackground(new Color(189, 202, 220));
            rankLabel.setForeground(new Color(35, 43, 58));
        } else if (rank == 3) {
            rankLabel.setBackground(new Color(198, 137, 92));
            rankLabel.setForeground(new Color(48, 32, 24));
        } else {
            rankLabel.setBackground(new Color(35, 48, 75));
            rankLabel.setForeground(SECONDARY_TEXT_COLOR);
        }

        JLabel nameLabel = new JLabel(player.name());
        nameLabel.setName("leaderboard-player-name");
        nameLabel.setForeground(PRIMARY_TEXT_COLOR);
        nameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        JLabel scoreLabel = new JLabel(Long.toString(player.highScore()), SwingConstants.RIGHT);
        scoreLabel.setForeground(ACCENT_COLOR);
        scoreLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        row.add(rankLabel, BorderLayout.WEST);
        row.add(nameLabel, BorderLayout.CENTER);
        row.add(scoreLabel, BorderLayout.EAST);
        return row;
    }

    private static Consumer<String> adaptPlayAction(Runnable onPlay) {
        Objects.requireNonNull(onPlay, "onPlay");
        return ignored -> onPlay.run();
    }

    private static final class CardPanel extends JPanel {
        private final Color fillColor;
        private final Color borderColor;

        private CardPanel(BorderLayout layout, Color fillColor, Color borderColor) {
            super(layout);
            this.fillColor = fillColor;
            this.borderColor = borderColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setColor(fillColor);
                graphics2D.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                graphics2D.setColor(borderColor);
                graphics2D.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
            } finally {
                graphics2D.dispose();
            }
        }
    }
}
