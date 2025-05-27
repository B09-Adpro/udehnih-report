-- Main database schema for tests
DROP TABLE IF EXISTS report;

CREATE TABLE report (
    report_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    detail CLOB NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('OPEN', 'CLOSED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED')),
    rejection_message VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_report_student_id ON report(student_id);
CREATE INDEX idx_report_status ON report(status);
