-- Local fixture only. Availability columns follow DB wiki section 32 (Table after changes).
-- Users includes the existing profile-schema fixture columns; language columns remain BIGINT.
CREATE TABLE users (
    user_id VARCHAR(255) PRIMARY KEY, first_name VARCHAR(255), middle_name VARCHAR(255),
    last_name VARCHAR(255), full_name VARCHAR(255), primary_email_address VARCHAR(255),
    primary_phone_number VARCHAR(50), time_zone VARCHAR(100), profile_picture_path VARCHAR(500),
    addr_ln1 VARCHAR(255), addr_ln2 VARCHAR(255), addr_ln3 VARCHAR(255), city_name VARCHAR(255),
    zip_code VARCHAR(20), gender VARCHAR(50), last_location VARCHAR(255),
    language_1 BIGINT, language_2 BIGINT, language_3 BIGINT, promotion_wizard_stage INTEGER,
    promotion_wizard_last_updated_at TIMESTAMP, last_updated_at TIMESTAMP,
    state_id VARCHAR(64), country_id INTEGER, user_status_id BIGINT,
    dob DATE, external_auth_provider VARCHAR(255)
);
CREATE TABLE user_additional_details (
    additional_detail_id BIGINT PRIMARY KEY, user_id VARCHAR(255), secondary_email_1 VARCHAR(255),
    secondary_email_2 VARCHAR(255), secondary_phone_1 VARCHAR(255), secondary_phone_2 VARCHAR(255)
);
CREATE TABLE user_availability (
    user_availability_id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL REFERENCES users(user_id),
    day_of_week VARCHAR(10) CHECK (day_of_week IN ('Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday')),
    start_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC')
);
CREATE INDEX idx_user_availability_user_id ON user_availability(user_id);
