package com.saayam.volunteer.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "state")
public class State {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "state_id")
    private Integer stateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    @JsonBackReference
    private Country country;

    @Column(name = "state_name")
    private String stateName;

    @Column(name = "last_update_date")
    private ZonedDateTime lastUpdateDate;


    @Override
    public String toString() {
        return "State{" +
                "stateId=" + stateId +
                ", stateName='" + stateName + '\'' +
                ", lastUpdateDate=" + lastUpdateDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return stateId == state.stateId &&
                Objects.equals(stateName, state.stateName) &&
                Objects.equals(lastUpdateDate, state.lastUpdateDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stateId, stateName, lastUpdateDate);
    }
}
