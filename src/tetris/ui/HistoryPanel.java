package tetris.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import tetris.service.GameRecord;

public final class HistoryPanel extends JPanel {
    private static final Color BACKGROUND_COLOR = new Color(14, 20, 40);
    private static final Color PRIMARY_TEXT_COLOR = new Color(239, 244, 255);
    private static final Color SECONDARY_TEXT_COLOR = new Color(157, 174, 207);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private final JLabel statusLabel = new JLabel("Đang tải lịch sử...", SwingConstants.CENTER);
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[] {"Người chơi", "Điểm", "Cấp", "Dòng", "Thời điểm"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public HistoryPanel(Runnable onBack) {
        super(new BorderLayout(0, 16));
        Objects.requireNonNull(onBack, "onBack");
        setBackground(BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(24, 34, 30, 34));

        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setOpaque(false);
        JLabel title = new JLabel("LỊCH SỬ VÁN CHƠI", SwingConstants.CENTER);
        title.setForeground(PRIMARY_TEXT_COLOR);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        ArcadeButton backButton = new ArcadeButton("Quay lại", onBack);
        backButton.setPreferredSize(new Dimension(130, 42));
        header.add(title, BorderLayout.CENTER);
        header.add(backButton, BorderLayout.EAST);

        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        table.setForeground(PRIMARY_TEXT_COLOR);
        table.setBackground(new Color(24, 33, 59));
        table.setGridColor(new Color(49, 64, 98));
        table.setSelectionBackground(new Color(54, 91, 145));
        table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(4).setPreferredWidth(180);

        statusLabel.setForeground(SECONDARY_TEXT_COLOR);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(statusLabel, BorderLayout.NORTH);
        body.add(new JScrollPane(table), BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    public void setHistory(List<GameRecord> records) {
        Objects.requireNonNull(records, "records");
        tableModel.setRowCount(0);
        for (GameRecord record : records) {
            tableModel.addRow(new Object[] {record.playerName(), record.score(), record.level(),
                    record.lines(), DATE_FORMAT.format(record.completedAt())});
        }
        statusLabel.setText(records.isEmpty()
                ? "Chưa có ván nào được lưu." : "Hiển thị " + records.size() + " ván gần nhất.");
    }

    public void setLoading() {
        tableModel.setRowCount(0);
        statusLabel.setText("Đang tải lịch sử...");
    }

    public void setLoadError(String message) {
        tableModel.setRowCount(0);
        statusLabel.setText("Không tải được lịch sử: " + Objects.requireNonNull(message, "message"));
    }
}
