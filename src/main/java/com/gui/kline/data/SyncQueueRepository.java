package com.gui.kline.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SyncQueueRepository {
    public void enqueue(String entityType, String payload) {
        String sql = "INSERT INTO sync_queue (id, entity_type, payload, status, created_at, last_error) VALUES (?, ?, ?, ?, NOW(), NULL)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, entityType);
            statement.setString(3, payload);
            statement.setString(4, "PENDING");
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to enqueue sync item", ex);
        }
    }

    public List<SyncQueueItem> findPending(int limit) {
        List<SyncQueueItem> items = new ArrayList<>();
        String sql = "SELECT id, entity_type, payload, status FROM sync_queue WHERE status = 'PENDING' ORDER BY created_at LIMIT ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                items.add(new SyncQueueItem(
                        rs.getString("id"),
                        rs.getString("entity_type"),
                        rs.getString("payload"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to read sync queue", ex);
        }
        return items;
    }

    public void markSynced(List<SyncQueueItem> items) {
        updateStatus(items, "SYNCED", null);
    }

    public void markFailed(List<SyncQueueItem> items, String error) {
        updateStatus(items, "FAILED", error);
    }

    private void updateStatus(List<SyncQueueItem> items, String status, String error) {
        if (items.isEmpty()) {
            return;
        }
        String sql = "UPDATE sync_queue SET status = ?, last_error = ? WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (SyncQueueItem item : items) {
                statement.setString(1, status);
                statement.setString(2, error);
                statement.setString(3, item.getId());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to update sync queue", ex);
        }
    }

    public int countPending() {
        String sql = "SELECT COUNT(*) AS total FROM sync_queue WHERE status = 'PENDING'";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            return rs.next() ? rs.getInt("total") : 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to count sync queue", ex);
        }
    }
}
