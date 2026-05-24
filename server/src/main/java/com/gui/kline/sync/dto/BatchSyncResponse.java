package com.gui.kline.sync.dto;

import java.util.ArrayList;
import java.util.List;

public class BatchSyncResponse {
    private int accepted;
    private int duplicate;
    private List<String> errors = new ArrayList<>();

    public BatchSyncResponse() {
    }

    public BatchSyncResponse(int accepted, int duplicate, List<String> errors) {
        this.accepted = accepted;
        this.duplicate = duplicate;
        this.errors = errors;
    }

    public int getAccepted() {
        return accepted;
    }

    public void setAccepted(int accepted) {
        this.accepted = accepted;
    }

    public int getDuplicate() {
        return duplicate;
    }

    public void setDuplicate(int duplicate) {
        this.duplicate = duplicate;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}

