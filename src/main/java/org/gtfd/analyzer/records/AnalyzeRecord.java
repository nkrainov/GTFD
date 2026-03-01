package org.gtfd.analyzer.records;

import java.util.ArrayList;
import java.util.List;

public class AnalyzeRecord {
    private List<MethodRecord> records;

    public AnalyzeRecord() {
        records = new ArrayList<MethodRecord>();
    }

    public void addMethodRecords(List<MethodRecord> methodRecords) {
        records.addAll(methodRecords);
    }
}
